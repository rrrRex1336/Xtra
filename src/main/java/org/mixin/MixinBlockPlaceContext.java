package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
public class MixinBlockPlaceContext {
   @Inject(method = "getNearestLookingDirection", at = @At("HEAD"), cancellable = true)
   private void getLegacyNearestLookingDirection(CallbackInfoReturnable<Direction> cir) {
      if (!ViaVersionFix.isPlacementFixEnabled() || !ViaVersionFix.isTargetOlderThanOrEqualTo("v1_12_2")) {
         return;
      }

      BlockPlaceContext context = (BlockPlaceContext)(Object)this;
      Player player = context.getPlayer();
      if (player == null) {
         return;
      }

      BlockPos pos = context.getClickedPos();
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

      cir.setReturnValue(player.getDirection());
   }

   @Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
   private void canPlaceLegacyDecoration(CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValue() || !ViaVersionFix.isPlacementFixEnabled() || !ViaVersionFix.isTargetOlderThanOrEqualTo("v1_12_2")) {
         return;
      }

      BlockPlaceContext context = (BlockPlaceContext)(Object)this;
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      if (!state.blocksMotion() && Block.byItem(context.getItemInHand().getItem()) == Blocks.AIR) {
         cir.setReturnValue(true);
      }
   }
}
