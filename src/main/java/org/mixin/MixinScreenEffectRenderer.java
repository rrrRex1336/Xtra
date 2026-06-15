package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.visual.LowFire;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(
            method = {"renderFire"},
            at = {@At(value = "HEAD")}
    )
    private static void onRenderFire(Minecraft mc, PoseStack poseStack, CallbackInfo ci) {
        if (LowFire.instance.isEnabled()) {
            poseStack.translate(0.0f, -0.3f, 0.0f);
        }
    }
}