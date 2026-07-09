package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventUseItemRayTrace;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public class MixinItem {
   @Inject(method = "use", at = @At("HEAD"), cancellable = true)
   private void useLegacySwordBlocking(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      ItemStack stack = player.getStackInHand(hand);
      if (ViaVersionFix.shouldUseLegacySwordBlocking(stack, player, hand)) {
         player.setCurrentHand(hand);
         cir.setReturnValue(TypedActionResult.consume(stack));
      }
   }

   @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
   private void getLegacySwordUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
      if (ViaVersionFix.shouldUseLegacySwordBlockingStats(stack)) {
         cir.setReturnValue(UseAction.BLOCK);
      }
   }

   @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
   private void getLegacySwordUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
      if (ViaVersionFix.shouldUseLegacySwordBlockingStats(stack)) {
         cir.setReturnValue(72000);
      }
   }

   @Redirect(
      method = {"raycast"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
      )
   )
   private static float hookRayTraceYRot(PlayerEntity instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYaw(), instance.getPitch());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"raycast"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getPitch()F"
      )
   )
   private static float hookRayTraceXRot(PlayerEntity instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYaw(), instance.getPitch());
      Naven.getInstance().getEventManager().call(event);
      return event.getPitch();
   }
}
