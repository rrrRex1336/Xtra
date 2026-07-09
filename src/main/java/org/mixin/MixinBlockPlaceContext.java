package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPlacementContext.class)
public class MixinBlockPlaceContext {
   @Inject(method = "getPlayerLookDirection", at = @At("HEAD"), cancellable = true)
   private void getLegacyNearestLookingDirection(CallbackInfoReturnable<Direction> cir) {
      if (!ViaVersionFix.isPlacementFixEnabled() || !ViaVersionFix.isTargetOlderThanOrEqualTo("v1_12_2")) {
         return;
      }

      ItemPlacementContext context = (ItemPlacementContext)(Object)this;
      PlayerEntity player = context.getPlayer();
      if (player == null) {
         return;
      }

      BlockPos pos = context.getBlockPos();
      double centerOffset = ViaVersionFix.isTargetNewerThan("v1_10") ? 0.5D : 0.0D;
      if (Math.abs(player.getX() - ((double)pos.getX() + centerOffset)) < 2.0D
         && Math.abs(player.getZ() - ((double)pos.getZ() + centerOffset)) < 2.0D) {
         double eyeY = player.getEyeY();
         if (eyeY - (double)pos.getY() > 2.0D) {
            cir.setReturnValue(Direction.UP);
            return;
         }

         if ((double)pos.getY() - eyeY > 0.0D) {
            cir.setReturnValue(Direction.DOWN);
            return;
         }
      }

      cir.setReturnValue(player.getHorizontalFacing());
   }

   @Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
   private void canPlaceLegacyDecoration(CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValue() || !ViaVersionFix.isPlacementFixEnabled() || !ViaVersionFix.isTargetOlderThanOrEqualTo("v1_12_2")) {
         return;
      }

      ItemPlacementContext context = (ItemPlacementContext)(Object)this;
      BlockState state = context.getWorld().getBlockState(context.getBlockPos());
      if (!state.blocksMovement() && Block.getBlockFromItem(context.getStack().getItem()) == Blocks.AIR) {
         cir.setReturnValue(true);
      }
   }
}
