package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.AirBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.FungusBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShortPlantBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Surround", description = "Find suitable conditions and place blocks to surround yourself.", category = Category.WORLD)
public class Surround extends Module {
   private static final int MAX_ACTIVE_TICKS = 80;
   private static final Direction[] PLACE_FACES = new Direction[]{
      Direction.UP,
      Direction.NORTH,
      Direction.SOUTH,
      Direction.WEST,
      Direction.EAST,
      Direction.DOWN
   };

   public final FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .setMaxFloatValue(20.0F)
      .setMinFloatValue(0.0F)
      .build()
      .getFloatValue();

   public final Vector2f rots = new Vector2f();

   private final ArrayDeque<PlaceTask> queue = new ArrayDeque<>();
   private BlockPos center;
   private Direction front;
   private Direction[] orderedDirections = new Direction[0];
   private Direction supportDirection;
   private Phase phase = Phase.IDLE;
   private Placement currentPlacement;
   private int oldSlot = -1;
   private int activeTicks;
   private long lastPlaceTick = -1L;
   private int noProgressTicks;
   private boolean jumpRushSawAir;

   @Override
   public void onEnable() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      this.oldSlot = mc.player.getInventory().selectedSlot;
      this.rots.set(mc.player.getYaw(), mc.player.getPitch());
      this.center = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      this.front = Direction.fromRotation(mc.player.getYaw());
      this.orderedDirections = new Direction[]{
         this.front,
         this.front.rotateYCounterclockwise(),
         this.front.getOpposite(),
         this.front.rotateYClockwise()
      };
      this.supportDirection = null;
      this.currentPlacement = null;
      this.activeTicks = 0;
      this.lastPlaceTick = -1L;
      this.noProgressTicks = 0;
      this.jumpRushSawAir = false;
      this.queue.clear();

      if (!ensureBlockSelected()) {
         this.setEnabled(false);
         return;
      }

      if (isJumpRequested()) {
         startJumpRush();
      } else {
         startBaseBuild();
      }
   }

   @Override
   public void onDisable() {
      this.queue.clear();
      this.currentPlacement = null;
      this.supportDirection = null;
      this.jumpRushSawAir = false;
      this.phase = Phase.IDLE;
      if (mc.player != null && this.oldSlot >= 0 && this.oldSlot < 9) {
         mc.player.getInventory().selectedSlot = this.oldSlot;
      }
   }

   @EventTarget(1)
   public void onRunTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE || mc.player == null || mc.world == null || mc.interactionManager == null || this.center == null) {
         return;
      }

      if (mc.currentScreen != null || ++this.activeTicks > MAX_ACTIVE_TICKS || !ensureBlockSelected()) {
         this.setEnabled(false);
         return;
      }

      if (this.phase == Phase.BUILD_BASE && isJumpRequested()) {
         startJumpRush();
      }

      advancePhases();
      this.currentPlacement = findNextPlacement();
      if (this.currentPlacement != null) {
         this.rots.set(this.currentPlacement.rotation.x, this.currentPlacement.rotation.y);
         RotationManager.setRotations(new Vector2f(this.rots.x, this.rots.y));
         RotationManager.active = true;
         this.noProgressTicks = 0;
      } else if (this.phase != Phase.WAIT_LANDING && this.queue.isEmpty()) {
         this.setEnabled(false);
      } else if (this.phase != Phase.WAIT_LANDING && ++this.noProgressTicks > 10) {
         this.setEnabled(false);
      }
   }

   @EventTarget
   public void onClick(EventClick event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || this.currentPlacement == null) {
         return;
      }

      event.setCancelled(true);
      int placeDelay = Math.max(0, Math.round(this.delay.getCurrentValue()));
      if (placeDelay > 0 && this.lastPlaceTick >= 0L && mc.world.getTime() - this.lastPlaceTick < placeDelay) {
         return;
      }

      if (isCompleted(this.currentPlacement.task.pos)) {
         this.queue.remove(this.currentPlacement.task);
         this.currentPlacement = null;
         return;
      }

      Hand hand = getPlaceHand();
      if (hand == null) {
         this.setEnabled(false);
         return;
      }

      RotationManager.setRotations(new Vector2f(this.rots.x, this.rots.y));
      RotationManager.active = true;
      ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, this.currentPlacement.hitResult);
      if (result == ActionResult.SUCCESS) {
         NetworkUtils.sendPacket(new HandSwingC2SPacket(hand));
         this.lastPlaceTick = mc.world.getTime();
         this.queue.remove(this.currentPlacement.task);
         this.currentPlacement = null;
         this.noProgressTicks = 0;
      } else {
         this.queue.remove(this.currentPlacement.task);
         this.currentPlacement = null;
      }
   }

   private void advancePhases() {
      removeCompletedTasks();
      if (this.phase == Phase.JUMP_PILLAR && this.queue.isEmpty()) {
         this.phase = Phase.WAIT_LANDING;
      }

      if (this.phase == Phase.WAIT_LANDING && !mc.player.isOnGround()) {
         this.jumpRushSawAir = true;
      }

      if (this.phase == Phase.WAIT_LANDING && this.jumpRushSawAir && mc.player.isOnGround()) {
         this.phase = Phase.CAP;
         this.queue.clear();
         addTask(this.center.up(2), TaskType.CAP);
      }

      removeCompletedTasks();
      if (this.phase == Phase.CAP && this.queue.isEmpty()) {
         startRemainingFill();
      }
   }

   private void startBaseBuild() {
      this.phase = Phase.BUILD_BASE;
      this.queue.clear();
      for (Direction direction : this.orderedDirections) {
         addSideColumn(direction, 2);
      }
   }

   private void startJumpRush() {
      this.supportDirection = chooseSupportDirection();
      if (this.supportDirection == null) {
         startBaseBuild();
         return;
      }

      this.phase = Phase.JUMP_PILLAR;
      this.jumpRushSawAir = !mc.player.isOnGround();
      this.queue.clear();
      addSideColumn(this.supportDirection, 3);
   }

   private void startRemainingFill() {
      this.phase = Phase.FILL_REMAINING;
      this.queue.clear();
      for (Direction direction : this.orderedDirections) {
         addSideColumn(direction, 2);
      }
      removeCompletedTasks();
   }

   private void addSideColumn(Direction direction, int height) {
      BlockPos base = this.center.offset(direction);
      for (int y = 0; y < height; ++y) {
         addTask(base.up(y), TaskType.SIDE);
      }
   }

   private void addTask(BlockPos pos, TaskType type) {
      if (!isCompleted(pos)) {
         this.queue.offer(new PlaceTask(pos, type));
      }
   }

   private void removeCompletedTasks() {
      this.queue.removeIf(task -> isCompleted(task.pos));
   }

   private Placement findNextPlacement() {
      if (this.queue.isEmpty()) {
         return null;
      }

      List<PlaceTask> tasks = new ArrayList<>(this.queue);
      for (PlaceTask task : tasks) {
         Placement placement = createPlacement(task);
         if (placement != null) {
            return placement;
         }

         if (task.type == TaskType.SIDE && !canOccupy(task.pos)) {
            this.queue.remove(task);
         }
      }

      return null;
   }

   private Placement createPlacement(PlaceTask task) {
      if (isCompleted(task.pos) || !canOccupy(task.pos)) {
         return null;
      }

      for (Direction face : PLACE_FACES) {
         BlockPos support = task.pos.offset(face.getOpposite());
         if (!isValidSupport(support)) {
            continue;
         }

         Vec3d hitVec = getHitVec(support, face);
         if (hitVec.distanceTo(mc.player.getEyePos()) > 4.5D) {
            continue;
         }

         Vector2f rotation = RotationUtils.getRotations(hitVec);
         if (rotation == null) {
            continue;
         }

         return new Placement(task, new BlockHitResult(hitVec, face, support, false), rotation);
      }

      return null;
   }

   private Direction chooseSupportDirection() {
      Direction best = null;
      int bestMissing = Integer.MAX_VALUE;
      int bestExisting = -1;

      for (Direction direction : this.orderedDirections) {
         int missing = 0;
         int existing = 0;
         boolean feasible = true;
         Set<BlockPos> planned = new HashSet<>();
         BlockPos base = this.center.offset(direction);

         for (int y = 0; y < 3; ++y) {
            BlockPos pos = base.up(y);
            if (isCompleted(pos)) {
               ++existing;
               planned.add(pos);
               continue;
            }

            if (!canOccupy(pos) || !hasSupport(pos, planned)) {
               feasible = false;
               break;
            }

            ++missing;
            planned.add(pos);
         }

         if (feasible && (missing < bestMissing || missing == bestMissing && existing > bestExisting)) {
            best = direction;
            bestMissing = missing;
            bestExisting = existing;
         }
      }

      return best;
   }

   private boolean hasSupport(BlockPos pos, Set<BlockPos> planned) {
      for (Direction face : PLACE_FACES) {
         BlockPos support = pos.offset(face.getOpposite());
         if (planned.contains(support) || isValidSupport(support)) {
            return true;
         }
      }

      return false;
   }

   private boolean isCompleted(BlockPos pos) {
      return !isReplaceable(pos);
   }

   private boolean canOccupy(BlockPos pos) {
      Box box = new Box(
         pos.getX(),
         pos.getY(),
         pos.getZ(),
         pos.getX() + 1.0D,
         pos.getY() + 1.0D,
         pos.getZ() + 1.0D
      );
      return isReplaceable(pos) && !mc.player.getBoundingBox().intersects(box) && mc.world.isSpaceEmpty(box);
   }

   private boolean isReplaceable(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.isAir() || state.isReplaceable();
   }

   private boolean isValidSupport(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      Block block = state.getBlock();
      return !isReplaceable(pos)
         && !(block instanceof FluidBlock)
         && !(block instanceof AirBlock)
         && !(block instanceof ChestBlock)
         && !(block instanceof FurnaceBlock)
         && !(block instanceof EnderChestBlock)
         && !(block instanceof ShortPlantBlock)
         && !(block instanceof SnowBlock)
         && !(block instanceof EnchantingTableBlock)
         && !(block instanceof AnvilBlock)
         && !(block instanceof CraftingTableBlock)
         && !state.getOutlineShape(mc.world, pos).isEmpty();
   }

   private Vec3d getHitVec(BlockPos support, Direction face) {
      return new Vec3d(
         support.getX() + 0.5D + face.getOffsetX() * 0.5D,
         support.getY() + 0.5D + face.getOffsetY() * 0.5D,
         support.getZ() + 0.5D + face.getOffsetZ() * 0.5D
      );
   }

   private boolean ensureBlockSelected() {
      if (getPlaceHand() != null) {
         return true;
      }

      int slot = findBlockSlot();
      if (slot == -1) {
         return false;
      }

      mc.player.getInventory().selectedSlot = slot;
      return true;
   }

   private Hand getPlaceHand() {
      if (isValidStack(mc.player.getMainHandStack())) {
         return Hand.MAIN_HAND;
      }

      if (isValidStack(mc.player.getOffHandStack())) {
         return Hand.OFF_HAND;
      }

      return null;
   }

   private int findBlockSlot() {
      for (int i = 0; i < 9; ++i) {
         if (isValidStack(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem blockItem) || stack.getCount() <= 0) {
         return false;
      }

      if (stack.getItem() instanceof AliasedBlockItem) {
         return false;
      }

      String name = stack.toHoverableText().getString();
      if (name.contains("Click") || name.contains("点击") || name.contains("鐐瑰嚮")) {
         return false;
      }

      Block block = blockItem.getBlock();
      return !(block instanceof FlowerBlock)
         && !(block instanceof PlantBlock)
         && !(block instanceof FungusBlock)
         && !(block instanceof CropBlock)
         && !(block instanceof SlabBlock)
         && !Scaffold.blacklistedBlocks.contains(block);
   }

   private boolean isJumpRequested() {
      return !mc.player.isOnGround() || mc.options.jumpKey.isPressed() || mc.player.getVelocity().y > 0.05D;
   }

   private enum Phase {
      IDLE,
      BUILD_BASE,
      JUMP_PILLAR,
      WAIT_LANDING,
      CAP,
      FILL_REMAINING
   }

   private enum TaskType {
      SIDE,
      CAP
   }

   private static final class PlaceTask {
      private final BlockPos pos;
      private final TaskType type;

      private PlaceTask(BlockPos pos, TaskType type) {
         this.pos = pos;
         this.type = type;
      }
   }

   private static final class Placement {
      private final PlaceTask task;
      private final BlockHitResult hitResult;
      private final Vector2f rotation;

      private Placement(PlaceTask task, BlockHitResult hitResult, Vector2f rotation) {
         this.task = task;
         this.hitResult = hitResult;
         this.rotation = rotation;
      }
   }
}
