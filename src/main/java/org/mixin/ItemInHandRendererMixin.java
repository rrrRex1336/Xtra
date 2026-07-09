package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.modules.impl.world.OldHitting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayerEntity player, float partialTick, float equipProgress, Hand hand, float swingProgress, ItemStack itemStack, float equippedProg, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight, CallbackInfo ci) {
        if (hand == Hand.OFF_HAND && ViaVersionFix.shouldHideServerLegacyBlockingShield(itemStack)) {
            ci.cancel();
            return;
        }

        OldHitting oldHitting = (OldHitting) Naven.getInstance().getModuleManager().getModule(OldHitting.class);
        if (oldHitting == null || !oldHitting.isEnabled()) {
            return;
        }
        if (oldHitting.BlockMods.getCurrentMode().equals("None")) {
            return;
        }
        if (hand != Hand.MAIN_HAND || !(itemStack.getItem() instanceof SwordItem)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean isOffhandUsing = false;
        if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND) {
            ItemStack offhandItem = mc.player.getOffHandStack();
            UseAction useAnim = offhandItem.getUseAction();
            if (useAnim != UseAction.BLOCK) {
                isOffhandUsing = true;
            }
        }
        boolean isKillauraBlocking = oldHitting.KillauraAutoBlock.getCurrentValue()
                && oldHitting.getAuraTarget() != null;

        if (isOffhandUsing && !isKillauraBlocking) {
            return;
        }
        if (!mc.options.useKey.isPressed() && !isKillauraBlocking) {
            return;
        }
        ci.cancel();
        oldHitting.renderArmWithItem(
                player,
                partialTick,
                equipProgress,
                hand,
                swingProgress,
                itemStack,
                equipProgress,
                poseStack,
                bufferSource,
                packedLight);
    }
}
