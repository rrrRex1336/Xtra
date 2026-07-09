package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventRotationAnimation;
import awa.qwq.ovo.Naven.modules.impl.visual.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {

   private Rotation getRotationModule() {
      return (Rotation) Naven.getInstance().getModuleManager().getModule(Rotation.class);
   }

   @Inject(
           method = {"render"},
           at = {@At("HEAD")}
   )
   private void onRenderPre(T entity, float entityYaw, float partialTicks, MatrixStack poseStack,
                            VertexConsumerProvider buffer, int packedLight, CallbackInfo ci) {
      Rotation rotation = getRotationModule();
      if (rotation != null && rotation.isEnabled()) {
         EventRotationAnimation.currentEntity = entity;

         if (entity instanceof PlayerEntity player && player == MinecraftClient.getInstance().player
                 && RotationManager.active && RotationManager.rotations != null
                 && rotation.syncHeadBodyYaw.getCurrentValue()) {
            player.bodyYaw = RotationManager.rotations.x;
            player.prevBodyYaw = RotationManager.rotations.x;
         }
      }
   }

   @Inject(
           method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
           at = {@At("HEAD")}
   )
   private void renderHead(
           T pEntity, float pEntityYaw, float pPartialTicks, MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pPackedLight, CallbackInfo ci
   ) {
      Rotation rotation = getRotationModule();
      if (rotation != null && rotation.isEnabled()) {
         EventRotationAnimation.currentEntity = pEntity;
      }
   }

   @Redirect(
           method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F",
                   ordinal = 1
           )
   )
   private float rotAnimationYaw(float pDelta, float pStart, float pEnd) {
      Rotation rotation = getRotationModule();
      if (rotation != null && rotation.isEnabled() && rotation.headYaw.getCurrentValue()) {
         EventRotationAnimation event = new EventRotationAnimation(pEnd, pStart, 0.0F, 0.0F);
         if (EventRotationAnimation.currentEntity == MinecraftClient.getInstance().player) {
            Naven.getInstance().getEventManager().call(event);
            return MathHelper.lerpAngleDegrees(pDelta, event.getLastYaw(), event.getYaw());
         }
      }
      return MathHelper.lerpAngleDegrees(pDelta, pStart, pEnd);
   }

   @Redirect(
           method = {"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F",
                   ordinal = 0
           )
   )
   private float rotAnimationPitch(float pDelta, float pStart, float pEnd) {
      Rotation rotation = getRotationModule();
      if (rotation != null && rotation.isEnabled() && rotation.headPitch.getCurrentValue()) {
         EventRotationAnimation event = new EventRotationAnimation(0.0F, 0.0F, pEnd, pStart);
         if (EventRotationAnimation.currentEntity == MinecraftClient.getInstance().player) {
            Naven.getInstance().getEventManager().call(event);
            return MathHelper.lerp(pDelta, event.getLastPitch(), event.getPitch());
         }
      }
      return MathHelper.lerp(pDelta, pStart, pEnd);
   }
}
