package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.visual.LowFire;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameOverlayRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(
            method = {"renderFireOverlay"},
            at = {@At(value = "HEAD")}
    )
    private static void onRenderFire(MinecraftClient mc, MatrixStack poseStack, CallbackInfo ci) {
        if (LowFire.instance.isEnabled()) {
            poseStack.translate(0.0f, -0.3f, 0.0f);
        }
    }
}
