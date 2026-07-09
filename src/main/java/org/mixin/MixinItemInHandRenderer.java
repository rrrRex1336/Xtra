package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventUpdateHeldItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({HeldItemRenderer.class})
public class MixinItemInHandRenderer {
   @Redirect(
      method = {"updateHeldItems"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"
      )
   )
   public ItemStack hookMainHand(ClientPlayerEntity player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(Hand.MAIN_HAND, player.getMainHandStack());
      if (player == MinecraftClient.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Redirect(
      method = {"updateHeldItems"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"
      )
   )
   public ItemStack hookOffHand(ClientPlayerEntity player) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(Hand.OFF_HAND, player.getOffHandStack());
      if (player == MinecraftClient.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }
}
