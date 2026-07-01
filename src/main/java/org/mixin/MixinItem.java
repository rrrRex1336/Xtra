package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventUseItemRayTrace;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public class MixinItem {
   @Inject(method = "use", at = @At("HEAD"), cancellable = true)
   private void useLegacySwordBlocking(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
      ItemStack stack = player.getItemInHand(hand);
      if (ViaVersionFix.shouldUseLegacySwordBlocking(stack, player, hand)) {
         player.startUsingItem(hand);
         cir.setReturnValue(InteractionResultHolder.consume(stack));
      }
   }

   @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
   private void getLegacySwordUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
      if (ViaVersionFix.shouldUseLegacySwordBlockingStats(stack)) {
         cir.setReturnValue(UseAnim.BLOCK);
      }
   }

   @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
   private void getLegacySwordUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
      if (ViaVersionFix.shouldUseLegacySwordBlockingStats(stack)) {
         cir.setReturnValue(72000);
      }
   }

   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
      )
   )
   private static float hookRayTraceYRot(Player instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYRot(), instance.getXRot());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"
      )
   )
   private static float hookRayTraceXRot(Player instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYRot(), instance.getXRot());
      Naven.getInstance().getEventManager().call(event);
      return event.getPitch();
   }
}
