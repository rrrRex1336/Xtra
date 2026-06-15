package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventHandlePacket;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "Helper",
        description = "You best helper XDDDDDDDDDDDDDDD :P.",
        category = Category.MISC
)
public class Helper extends Module {
   TimeHelper diamond = new TimeHelper();
   TimeHelper emerald = new TimeHelper();
   private int dia;
   private int eme = 1;
   private boolean startgame = false;
   //收水
   public int collectWaterSlot = -1;
   public int collectWaterTick = 0;
   public boolean collectingWater = false;
   public BlockPos currentFireTarget = null;
   //灭火
   public int extinguishTick = 0;
   //堵岩浆
   public int blockLavaSlot = -1;
   public int blockLavaTick = 0;
   public boolean blockingLava = false;
   //堵水
   public int blockWaterSlot = -1;
   public int blockWaterTick = 0;
   public boolean blockingWater = false;
   //堵B(?

   //堵箱子
   public int wreckChestSlot = -1;
   public int wreckChestTick = 0;
   public boolean wreckingChest = false;

   //转头
   private int originalSlot = -1;
   public boolean rotation;
   public Vector2f helperRotation = null;
   public boolean needRotate = false;
   private int waitTick = 0;
   public BlockPos above;

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

   @Override
   public void onDisable() {
      super.onDisable();
      if (originalSlot != -1) {
         mc.player.getInventory().selected = originalSlot;
         originalSlot = -1;
      }
      needRotate = false;
      helperRotation = null;
      collectingWater = false;
      blockingLava = false;
      blockingWater = false;
      RotationManager.active = false;
   }

   private void interactWithBlock(BlockPos pos, InteractionHand hand) {
      if (pos == null || mc.level == null) return;
      if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) {
         lookAtBlock(pos);
         needRotate = true;
         return;
      }
      mc.gameMode.useItem(mc.player, hand);
      mc.player.swing(hand);
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (!collectingWater && !blockingLava && !blockingWater && !wreckingChest) {
         if (originalSlot != -1) {
            mc.player.getInventory().selected = originalSlot;
            originalSlot = -1;
         }
      }
      if (e.getType() != EventType.PRE || mc.player == null || mc.level == null) return;
      if (this.diamond.delay(30000.0) && this.startgame) {
         this.dia++;
         ChatUtils.addChatMessage("第" + this.dia + "波钻石刷新");
         this.diamond.reset();
      }
      if (this.emerald.delay(60000.0) && this.startgame) {
         this.eme++;
         ChatUtils.addChatMessage("第" + this.eme + "波绿宝石刷新");
         this.emerald.reset();
      }
      needRotate = false;
      helperRotation = null;

      if (collectWater.getCurrentValue()) {
         processCollectWater();
         if (needRotate) return;
      }

      if (extinguisher.getCurrentValue() && !needRotate) {
         processExtinguisher();
         if (needRotate) return;
      }

      if (blockLava.getCurrentValue() && !needRotate) {
         processBlockLava();
         if (needRotate) return;
      }

      if (blockWater.getCurrentValue() && !needRotate && !collectingWater) {
         processBlockWater();
         if (needRotate) return;
      }

      if (needRotate && helperRotation != null) {
         RotationManager.setRotations(helperRotation);
         RotationManager.active = true;
      }
   }

   private void processCollectWater() {
      if (collectingWater) {
         collectWaterTick--;
         if (collectWaterTick > 0) return;

         if (collectWaterSlot != -1 && mc.player.getInventory().selected == collectWaterSlot) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (mainHand.getItem() == Items.WATER_BUCKET) {
               if (originalSlot != -1) {
                  mc.player.getInventory().selected = originalSlot;
                  originalSlot = -1;
               }
               collectingWater = false;
               collectWaterSlot = -1;
               return;
            }
         }
         collectingWater = false;
         collectWaterSlot = -1;
      }

      BlockPos waterPos = findNearestWater();
      if (waterPos == null) return;

      int bucketSlot = findEmptyBucket();
      if (bucketSlot == -1) return;

      if (originalSlot == -1) {
         originalSlot = mc.player.getInventory().selected;
      }
      if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(waterPos)) {

         lookAtBlock(waterPos);
         needRotate = true;
         return;
      }

      collectWaterSlot = bucketSlot;
      mc.player.getInventory().selected = bucketSlot;
      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      mc.player.swing(InteractionHand.MAIN_HAND);

      collectingWater = true;
      collectWaterTick = 1;
   }

   private void processBlockLava() {
      if (blockingLava) {
         blockLavaTick--;
         if (blockLavaTick > 0) return;

         BlockPos lavaPos = findNearestLava();
         if (lavaPos == null) {
            if (originalSlot != -1) {
               mc.player.getInventory().selected = originalSlot;
               originalSlot = -1;
            }
            blockingLava = false;
            blockLavaSlot = -1;
            return;
         }
         blockingLava = false;
         blockLavaSlot = -1;
      }

      BlockPos lavaPos = findNearestLava();
      if (lavaPos == null) return;

      PlaceOnResult placeOn = findPlaceOnBlock(lavaPos);
      if (placeOn == null) return;

      int blockSlot = findBlockInHotbar();
      if (blockSlot == -1) return;
      if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(placeOn.facePos)) {
         lookAtBlock(placeOn.facePos);
         needRotate = true;
         return;
      }

      if (originalSlot == -1) {
         originalSlot = mc.player.getInventory().selected;
      }

      blockLavaSlot = blockSlot;
      mc.player.getInventory().selected = blockSlot;

      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      mc.player.swing(InteractionHand.MAIN_HAND);

      blockingLava = true;
      blockLavaTick = 1;
   }

   private void processBlockWater() {
      if (blockingWater) {
         blockWaterTick--;
         if (blockWaterTick > 0) return;

         BlockPos waterPos = findNearestWaterForBlock();
         if (waterPos == null) {
            if (originalSlot != -1) {
               mc.player.getInventory().selected = originalSlot;
               originalSlot = -1;
            }
            blockingWater = false;
            blockWaterSlot = -1;
            return;
         }
         blockingWater = false;
         blockWaterSlot = -1;
      }

      BlockPos waterPos = findNearestWaterForBlock();
      if (waterPos == null) return;

      PlaceOnResult placeOn = findPlaceOnBlock(waterPos);
      if (placeOn == null) return;

      int blockSlot = findBlockInHotbar();
      if (blockSlot == -1) return;

      if (originalSlot == -1) {
         originalSlot = mc.player.getInventory().selected;
      }

      lookAtBlock(placeOn.facePos);
      blockWaterSlot = blockSlot;
      mc.player.getInventory().selected = blockSlot;
      interactWithBlock(placeOn.facePos, InteractionHand.MAIN_HAND);

      blockingWater = true;
      blockWaterTick = 1;
   }

   private void processWreckChest() {
      if (wreckingChest) {
         wreckChestTick--;
         if (wreckChestTick > 0) return;

         BlockPos chestPos = findNearestChest();
         if (chestPos == null) {
            mc.player.getInventory().selected = getOriginalSlot();
            wreckingChest = false;
            wreckChestSlot = -1;
            return;
         }
         wreckingChest = false;
         wreckChestSlot = -1;
      }

      BlockPos chestPos = findNearestChest();
      if (chestPos == null) return;

      PlaceOnResult placeOn = findPlaceOnBlock(chestPos);
      if (placeOn == null) return;

      int blockSlot = findBlockInHotbar();
      if (blockSlot == -1) return;

      lookAtBlock(placeOn.facePos);

      wreckChestSlot = blockSlot;
      mc.player.getInventory().selected = blockSlot;

      interactWithBlock(placeOn.facePos, InteractionHand.MAIN_HAND);

      wreckingChest = true;
      wreckChestTick = 1;
   }

   private void processExtinguisher() {
      if (extinguishTick > 0) {
         extinguishTick--;
         return;
      }

      BlockPos firePos = findNearestFire();
      if (firePos == null) {
         currentFireTarget = null;
         return;
      }

      currentFireTarget = firePos;
      if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(firePos)) {
         lookAtBlock(firePos);
         needRotate = true;
         return;
      }
      mc.gameMode.attack(mc.player, null);
      mc.player.swing(InteractionHand.MAIN_HAND);

      extinguishTick = 10;
   }

   private BlockPos findNearestWater() {
      return findNearestBlockInRange((int)3.8F, state -> state.getBlock() == Blocks.WATER && state.getValue(LiquidBlock.LEVEL) == 0);
   }

   private BlockPos findNearestWaterForBlock() {
      return findNearestBlockInRange((int)3.8F, state -> state.getBlock() == Blocks.WATER && state.getValue(LiquidBlock.LEVEL) == 0);
   }

   private BlockPos findNearestLava() {
      return findNearestBlockInRange( (int)3.8F, state -> state.getBlock() == Blocks.LAVA && state.getValue(LiquidBlock.LEVEL) == 0);
   }

   public BlockPos findNearestFire() {
      return findNearestBlockInRange( (int)3.8F, state -> state.getBlock() instanceof FireBlock || state.getBlock() == Blocks.FIRE || state.getBlock() == Blocks.SOUL_FIRE);
   }

   public BlockPos findNearestChest() {
      return findNearestBlockInRange((int)3.8F, state -> state.getBlock() instanceof ChestBlock || state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.ENDER_CHEST);
   }

   private PlaceOnResult findPlaceOnBlock(BlockPos fluidPos) {
      if (mc.level == null || mc.player == null) return null;
      Vec3 playerPos = mc.player.getEyePosition(1.0F);
      Vec3 fluidCenter = new Vec3(fluidPos.getX() + 0.5, fluidPos.getY() + 0.5, fluidPos.getZ() + 0.5);
      Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
      PlaceOnResult bestResult = null;
      double bestDistance = Double.MAX_VALUE;

      for (Direction dir : directions) {
         BlockPos neighborPos = fluidPos.relative(dir);
         BlockState state = mc.level.getBlockState(neighborPos);
         if (state.isAir() || state.getBlock() instanceof LiquidBlock || !state.isSolid()) {
            continue;
         }
         Direction clickDir = dir;
         Vec3 blockCenter = new Vec3(neighborPos.getX() + 0.5, neighborPos.getY() + 0.5, neighborPos.getZ() + 0.5);
         Vec3 faceCenter = blockCenter.add(clickDir.getStepX() * 0.5, clickDir.getStepY() * 0.5, clickDir.getStepZ() * 0.5);
         Vec3 playerToFace = faceCenter.subtract(playerPos).normalize();
         Vec3 clickDirVec = new Vec3(clickDir.getStepX(), clickDir.getStepY(), clickDir.getStepZ());

         double dot = playerToFace.dot(clickDirVec);
         if (dot > 0) {
            double dist = mc.player.distanceToSqr(neighborPos.getX() + 0.5, neighborPos.getY() + 0.5, neighborPos.getZ() + 0.5);
            if (dist < bestDistance) {
               bestDistance = dist;
               bestResult = new PlaceOnResult(neighborPos, dir.getOpposite());
            }
         }
      }
      if (bestResult == null) {
         BlockPos belowPos = fluidPos.below();
         BlockState belowState = mc.level.getBlockState(belowPos);
         if (!belowState.isAir() && !(belowState.getBlock() instanceof LiquidBlock) && belowState.isSolid()) {
            if (playerPos.y > belowPos.getY() + 1) {
               bestResult = new PlaceOnResult(belowPos, Direction.UP);
            }
         }
      }
      return bestResult;
   }

   public BlockPos findNearestBlockInRange(int range, java.util.function.Predicate<BlockState> predicate) {
      if (mc.player == null || mc.level == null) return null;

      BlockPos playerPos = mc.player.blockPosition();
      BlockPos nearest = null;
      double nearestDist = Double.MAX_VALUE;

      for (int x = -range; x <= range; x++) {
         for (int y = -range; y <= range; y++) {
            for (int z = -range; z <= range; z++) {
               BlockPos pos = playerPos.offset(x, y, z);
               BlockState state = mc.level.getBlockState(pos);
               if (predicate.test(state)) {
                  double dist = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                  if (dist < nearestDist) {
                     nearestDist = dist;
                     nearest = pos;
                  }
               }
            }
         }
      }
      return nearest;
   }

   public int findEmptyBucket() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() == Items.BUCKET) {
            return i;
         }
      }
      return -1;
   }

   public int findBlockInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem) {
            return i;
         }
      }
      return -1;
   }

   public int getOriginalSlot() {
      return 0;
   }

   private void lookAtBlock(BlockPos pos) {
      if (pos == null) return;

      Vec3 eyePos = mc.player.getEyePosition(1.0F);
      Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

      double dx = target.x - eyePos.x;
      double dy = target.y - eyePos.y;
      double dz = target.z - eyePos.z;
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);

      float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
      float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));

      helperRotation = new Vector2f(yaw, pitch);
      needRotate = true;
   }

   private static class PlaceOnResult {
      final BlockPos facePos;
      final Direction direction;

      PlaceOnResult(BlockPos facePos, Direction direction) {
         this.facePos = facePos;
         this.direction = direction;
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.dia = 0;
      this.eme = 0;
   }

   @EventTarget(0)
   public void onPacket(EventHandlePacket e) {
      try {
         if (mc.player != null && !e.isCancelled() && e.getPacket() instanceof ClientboundSystemChatPacket) {
            ClientboundSystemChatPacket s02 = (ClientboundSystemChatPacket)e.getPacket();
            String words = s02.content().getString();
            if (words.contains("游戏准备开始")) {
               this.diamond.reset();
               this.emerald.reset();
               this.startgame = true;
            }
            if (words.contains("游戏结束")) {
               this.startgame = false;
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }
}