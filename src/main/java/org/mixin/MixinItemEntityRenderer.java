package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.ItemPhysics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(ItemEntity entity, float entityYaw, float partialTicks,
                         PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ItemPhysics module = (ItemPhysics) Naven.getInstance().getModuleManager().getModule(ItemPhysics.class);

        if (module != null && module.isEnabled()) {
            matrixStack.pushPose();

            if (module.rotateInAir.getCurrentValue() && !entity.onGround()) {
                float rotation = entity.tickCount + partialTicks;
                matrixStack.mulPose(Axis.YP.rotation(rotation * 0.1F * module.rotationSpeed.getCurrentValue()));
            }

            if (module.adjustScale.getCurrentValue()) {
                float scale = module.scaleFactor.getCurrentValue();
                matrixStack.scale(scale, scale, scale);
            }
            matrixStack.popPose();
        }
    }
}