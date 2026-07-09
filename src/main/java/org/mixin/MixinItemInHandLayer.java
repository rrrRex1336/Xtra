package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventUpdateHeldItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({HeldItemFeatureRenderer.class})
public class MixinItemInHandLayer {
   @Redirect(
      method = {"render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"
      )
   )
   private ItemStack hookMainHand(LivingEntity instance) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(Hand.MAIN_HAND, instance.getMainHandStack());
      if (instance == MinecraftClient.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"
      )
   )
   private ItemStack hookOffHand(LivingEntity instance) {
      EventUpdateHeldItem event = new EventUpdateHeldItem(Hand.OFF_HAND, instance.getOffHandStack());
      if (instance == MinecraftClient.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return event.getItem();
   }
}
