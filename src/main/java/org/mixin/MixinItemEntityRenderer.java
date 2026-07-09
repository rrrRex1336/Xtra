package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(ItemEntity entity, float entityYaw, float partialTicks,
                         MatrixStack matrixStack, VertexConsumerProvider buffer, int packedLight, CallbackInfo ci) {
        ItemPhysics module = (ItemPhysics) Naven.getInstance().getModuleManager().getModule(ItemPhysics.class);

        if (module != null && module.isEnabled()) {
            matrixStack.push();

            if (module.rotateInAir.getCurrentValue() && !entity.isOnGround()) {
                float rotation = entity.age + partialTicks;
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(rotation * 0.1F * module.rotationSpeed.getCurrentValue()));
            }

            if (module.adjustScale.getCurrentValue()) {
                float scale = module.scaleFactor.getCurrentValue();
                matrixStack.scale(scale, scale, scale);
            }
            matrixStack.pop();
        }
    }
}