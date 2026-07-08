package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "Helper",
        description = "You best helper XDDDDDDDDDDDDDDD :P.",
        category = Category.MISC
)
public class Helper extends Module {
   private static final int RANGE = 4;

   public boolean rotation;
   public Vector2f helperRotation = null;
   public boolean needRotate = false;
   public BlockPos above;

   public int collectWaterSlot = -1;
   public int collectWaterTick = 0;
   public boolean collectingWater = false;
   public BlockPos currentFireTarget = null;
   public int extinguishTick = 0;
   public int blockLavaSlot = -1;
   public int blockLavaTick = 0;
   public boolean blockingLava = false;
   public int blockWaterSlot = -1;
   public int blockWaterTick = 0;
   public boolean blockingWater = false;
   public int wreckChestSlot = -1;
   public int wreckChestTick = 0;
   public boolean wreckingChest = false;

   public BooleanValue collectWater = ValueBuilder.create(this, "Auto Collect Water")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   BooleanValue extinguisher = ValueBuilder.create(this, "Auto Extinguisher")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   BooleanValue blockLava = ValueBuilder.create(this, "Auto Block Lava")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   BooleanValue blockWater = ValueBuilder.create(this, "Auto Block Water")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   BooleanValue chestWreck = ValueBuilder.create(this, "Chest Wreck")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private Task currentTask;
   private int originalSlot = -1;
   private int rotateTicks;
   private int cooldownTicks;

   @Override
   public void onDisable() {
      this.restoreSlot();
      this.clearTask();
      this.resetLegacyState();
      super.onDisable();
   }

   @EventTarget
   public void onRunTicks(EventRunTicks event) {
      if (event.getType() != EventType.PRE || mc.player == null || mc.level == null || mc.gameMode == null) {
         return;
      }

      this.needRotate = false;
      this.helperRotation = null;
      if (this.cooldownTicks > 0) {
         --this.cooldownTicks;
         this.restoreSlotIfIdle();
         return;
      }

      if (this.currentTask == null) {
         this.currentTask = this.findTask();
         this.rotateTicks = 0;
      }

      if (this.currentTask == null) {
         this.restoreSlotIfIdle();
         return;
      }

      this.applyTaskRotation(this.currentTask);
      if (++this.rotateTicks < 2) {
         return;
      }

      this.executeTask(this.currentTask);
      this.clearTask();
      this.cooldownTicks = 2;
      this.restoreSlot();
   }

   private Task findTask() {
      if (this.collectWater.getCurrentValue()) {
         Task task = this.createCollectWaterTask();
         if (task != null) {
            return task;
         }
      }
      if (this.extinguisher.getCurrentValue()) {
         Task task = this.createExtinguishTask();
         if (task != null) {
            return task;
         }
      }
      if (this.blockLava.getCurrentValue()) {
         Task task = this.createBlockFluidTask(Fluids.LAVA);
         if (task != null) {
            return task;
         }
      }
      if (this.blockWater.getCurrentValue()) {
         Task task = this.createBlockFluidTask(Fluids.WATER);
         if (task != null) {
            return task;
         }
      }
      if (this.chestWreck.getCurrentValue()) {
         return this.createChestWreckTask();
      }
      return null;
   }

   private Task createCollectWaterTask() {
      BlockPos water = this.findNearestBlock(state -> state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource());
      int bucketSlot = this.findSlot(stack -> stack.is(Items.BUCKET));
      if (water == null || bucketSlot == -1) {
         return null;
      }

      BlockHitResult hit = new BlockHitResult(center(water), Direction.UP, water, false);
      return new Task(TaskType.COLLECT_WATER, hit, bucketSlot);
   }

   private Task createExtinguishTask() {
      BlockPos fire = this.findNearestBlock(state -> state.getBlock() instanceof FireBlock
              || state.is(Blocks.FIRE)
              || state.is(Blocks.SOUL_FIRE));
      if (fire == null) {
         this.currentFireTarget = null;
         return null;
      }

      this.currentFireTarget = fire;
      BlockHitResult hit = new BlockHitResult(center(fire), Direction.UP, fire, false);
      return new Task(TaskType.EXTINGUISH, hit, -1);
   }

   private Task createBlockFluidTask(net.minecraft.world.level.material.Fluid fluid) {
      BlockPos fluidPos = this.findNearestBlock(state -> state.getFluidState().is(fluid) && state.getFluidState().isSource());
      int blockSlot = this.findSlot(stack -> stack.getItem() instanceof BlockItem && Scaffold.isValidStack(stack));
      if (fluidPos == null || blockSlot == -1) {
         return null;
      }

      BlockHitResult hit = this.findPlacementHit(fluidPos);
      if (hit == null) {
         return null;
      }

      return new Task(fluid == Fluids.LAVA ? TaskType.BLOCK_LAVA : TaskType.BLOCK_WATER, hit, blockSlot);
   }

   private Task createChestWreckTask() {
      BlockPos chest = this.findNearestBlock(state -> state.getBlock() instanceof ChestBlock || state.is(Blocks.CHEST) || state.is(Blocks.ENDER_CHEST));
      if (chest == null) {
         return null;
      }

      Direction face = this.getFacingFace(chest);
      BlockHitResult hit = new BlockHitResult(center(chest), face, chest, false);
      return new Task(TaskType.WRECK_CHEST, hit, -1);
   }

   private void executeTask(Task task) {
      switch (task.type) {
         case COLLECT_WATER -> {
            this.selectSlot(task.slot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.collectWaterSlot = task.slot;
            this.collectWaterTick = 1;
            this.collectingWater = true;
         }
         case BLOCK_LAVA, BLOCK_WATER -> {
            this.selectSlot(task.slot);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, task.hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND);
            if (task.type == TaskType.BLOCK_LAVA) {
               this.blockLavaSlot = task.slot;
               this.blockLavaTick = 1;
               this.blockingLava = true;
            } else {
               this.blockWaterSlot = task.slot;
               this.blockWaterTick = 1;
               this.blockingWater = true;
            }
         }
         case EXTINGUISH -> {
            mc.gameMode.startDestroyBlock(task.hitResult.getBlockPos(), task.hitResult.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.extinguishTick = 10;
         }
         case WRECK_CHEST -> {
            mc.gameMode.startDestroyBlock(task.hitResult.getBlockPos(), task.hitResult.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.wreckChestTick = 1;
            this.wreckingChest = true;
         }
      }
   }

   private void applyTaskRotation(Task task) {
      this.helperRotation = RotationUtils.getRotations(task.hitResult.getLocation());
      this.needRotate = this.helperRotation != null;
      if (this.needRotate) {
         RotationManager.setRotations(new Vector2f(this.helperRotation.x, this.helperRotation.y));
         RotationManager.active = true;
      }
   }

   private BlockHitResult findPlacementHit(BlockPos targetPos) {
      Vec3 eye = mc.player.getEyePosition();
      BlockHitResult best = null;
      double bestDistance = Double.MAX_VALUE;
      for (Direction direction : Direction.values()) {
         BlockPos support = targetPos.relative(direction);
         BlockState supportState = mc.level.getBlockState(support);
         if (!this.isValidSupport(supportState)) {
            continue;
         }

         Direction face = direction.getOpposite();
         Vec3 hitVec = new Vec3(
                 support.getX() + 0.5D + face.getStepX() * 0.5D,
                 support.getY() + 0.5D + face.getStepY() * 0.5D,
                 support.getZ() + 0.5D + face.getStepZ() * 0.5D
         );
         double distance = eye.distanceToSqr(hitVec);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = new BlockHitResult(hitVec, face, support, false);
         }
      }
      return best;
   }

   private boolean isValidSupport(BlockState state) {
      return !state.isAir()
              && !(state.getBlock() instanceof LiquidBlock)
              && !state.getShape(mc.level, BlockPos.ZERO).isEmpty()
              && state.isSolid();
   }

   private BlockPos findNearestBlock(java.util.function.Predicate<BlockState> predicate) {
      BlockPos playerPos = mc.player.blockPosition();
      BlockPos nearest = null;
      double bestDistance = Double.MAX_VALUE;
      for (int x = -RANGE; x <= RANGE; x++) {
         for (int y = -RANGE; y <= RANGE; y++) {
            for (int z = -RANGE; z <= RANGE; z++) {
               BlockPos pos = playerPos.offset(x, y, z);
               BlockState state = mc.level.getBlockState(pos);
               if (!predicate.test(state)) {
                  continue;
               }
               double distance = mc.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
               if (distance < bestDistance) {
                  bestDistance = distance;
                  nearest = pos;
               }
            }
         }
      }
      return nearest;
   }

   private int findSlot(java.util.function.Predicate<ItemStack> predicate) {
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (predicate.test(stack)) {
            return slot;
         }
      }
      return -1;
   }

   private void selectSlot(int slot) {
      if (slot < 0 || slot > 8) {
         return;
      }
      if (this.originalSlot == -1) {
         this.originalSlot = mc.player.getInventory().selected;
      }
      mc.player.getInventory().selected = slot;
   }

   private void restoreSlotIfIdle() {
      if (this.currentTask == null) {
         this.restoreSlot();
      }
   }

   private void restoreSlot() {
      if (mc.player != null && this.originalSlot >= 0 && this.originalSlot < 9) {
         mc.player.getInventory().selected = this.originalSlot;
      }
      this.originalSlot = -1;
   }

   private void clearTask() {
      this.currentTask = null;
      this.rotateTicks = 0;
      this.needRotate = false;
      this.helperRotation = null;
   }

   private void resetLegacyState() {
      this.collectWaterSlot = -1;
      this.collectWaterTick = 0;
      this.collectingWater = false;
      this.currentFireTarget = null;
      this.extinguishTick = 0;
      this.blockLavaSlot = -1;
      this.blockLavaTick = 0;
      this.blockingLava = false;
      this.blockWaterSlot = -1;
      this.blockWaterTick = 0;
      this.blockingWater = false;
      this.wreckChestSlot = -1;
      this.wreckChestTick = 0;
      this.wreckingChest = false;
   }

   private Direction getFacingFace(BlockPos pos) {
      Vec3 eye = mc.player.getEyePosition();
      Vec3 center = center(pos);
      return Direction.getNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
   }

   private static Vec3 center(BlockPos pos) {
      return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
   }

   private enum TaskType {
      COLLECT_WATER,
      EXTINGUISH,
      BLOCK_LAVA,
      BLOCK_WATER,
      WRECK_CHEST
   }

   private static class Task {
      private final TaskType type;
      private final BlockHitResult hitResult;
      private final int slot;

      private Task(TaskType type, BlockHitResult hitResult, int slot) {
         this.type = type;
         this.hitResult = hitResult;
         this.slot = slot;
      }
   }
}
