package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.World.OldHitting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float equipProgress, InteractionHand hand, float swingProgress, ItemStack itemStack, float equippedProg, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        OldHitting oldHitting = (OldHitting) Naven.getInstance().getModuleManager().getModule(OldHitting.class);
        if (oldHitting == null || !oldHitting.isEnabled()) {
            return;
        }
        if (oldHitting.BlockMods.getCurrentMode().equals("None")) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND || !(itemStack.getItem() instanceof SwordItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean isOffhandUsing = false;
        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = mc.player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }
        boolean isKillauraBlocking = oldHitting.KillauraAutoBlock.getCurrentValue()
                && oldHitting.getAuraTarget() != null;

        if (isOffhandUsing && !isKillauraBlocking) {
            return;
        }
        if (!mc.options.keyUse.isDown() && !isKillauraBlocking) {
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