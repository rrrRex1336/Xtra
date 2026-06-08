package awa.qwq.ovo.Naven.modules.impl.World;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Combat.KillAura;
import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;

@ModuleInfo(name = "OldHitting", description = "Customizes item animations and block animations", category = Category.WORLD)
public class OldHitting extends Module {
    public final ModeValue BlockMods = ValueBuilder.create(this, "Block Mods")
            .setModes("None", "1.7", "Push")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    public final BooleanValue KillauraAutoBlock = ValueBuilder.create(this, "Killaura Auto Block")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue BlockingX = ValueBuilder.create(this, "Blocking-X")
            .setDefaultFloatValue(0.56F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    public final FloatValue BlockingY = ValueBuilder.create(this, "Blocking-Y")
            .setDefaultFloatValue(-0.52F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    private boolean flip;
    public static boolean isBlocking = false;
    private final Minecraft mc = Minecraft.getInstance();
    private float mainHandHeight = 0.0F;
    private float offHandHeight = 0.0F;
    private float oMainHandHeight = 0.0F;
    private float oOffHandHeight = 0.0F;
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

//    @SubscribeEvent
//    public void onRenderHand(RenderHandEvent event) {
//        if (!this.isEnabled() || !OverrideVanilla.getCurrentValue() || BlockMods.getCurrentMode().equals("None"))
//            return;
//
//        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof SwordItem))
//            return;
//        boolean isOffhandUsing = false;
//        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
//            ItemStack offhandItem = mc.player.getOffhandItem();
//            UseAnim useAnim = offhandItem.getUseAnimation();
//            if (useAnim != UseAnim.BLOCK) {
//                isOffhandUsing = true;
//            }
//        }
//        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null;
//        if (isOffhandUsing && !isKillauraBlocking)
//            return;
//
//        if (!mc.options.keyUse.isDown() && !isKillauraBlocking)
//            return;
//
//        event.setCanceled(true);
//
//        renderArmWithItem(
//                mc.player,
//                event.getPartialTick(),
//                event.getEquipProgress(),
//                event.getHand(),
//                event.getSwingProgress(),
//                event.getItemStack(),
//                event.getEquipProgress(),
//                event.getPoseStack(),
//                event.getMultiBufferSource(),
//                event.getPackedLight());
//    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSwingPacket) {
            flip = !flip;
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE || mc.player == null)
            return;

        updateHandStates();
    }

    private void updateHandStates() {
        oMainHandHeight = mainHandHeight;
        oOffHandHeight = offHandHeight;

        LocalPlayer localplayer = mc.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();
        boolean isBlocking = isBlocking();

        if (isBlocking) {
            mainHandHeight = 1.0F;
            if (ItemStack.matches(mainHandItem, itemstack)) {
                mainHandItem = itemstack;
            }
            if (ItemStack.matches(offHandItem, itemstack1)) {
                offHandItem = itemstack1;
            }
            return;
        }

        if (localplayer.isHandsBusy()) {
            mainHandHeight = Mth.clamp(mainHandHeight - 0.4F, 0.0F, 1.0F);
            offHandHeight = Mth.clamp(offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackStrengthScale(1.0F);

            // ========== 替换 ForgeHooksClient ==========
            boolean flag = shouldCauseReequipAnimation(mainHandItem, itemstack);
            boolean flag1 = shouldCauseReequipAnimation(offHandItem, itemstack1);

            if (!flag && mainHandItem != itemstack) {
                mainHandItem = itemstack;
            }

            if (!flag1 && offHandItem != itemstack1) {
                offHandItem = itemstack1;
            }
            float targetMainHeight = !flag ? f * f * f : 0.0F;
            float targetOffHeight = !flag1 ? 1.0F : 0.0F;

            mainHandHeight += Mth.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);
            offHandHeight += Mth.clamp(targetOffHeight - offHandHeight, -0.2F, 0.2F);
        }

        if (mainHandHeight < 0.1F) {
            mainHandItem = itemstack;
        }

        if (offHandHeight < 0.1F) {
            offHandItem = itemstack1;
        }
    }

    private boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to) {
        if (from == to) return false;
        if (from.isEmpty() && to.isEmpty()) return false;

        boolean itemsEqual = ItemStack.isSameItem(from, to);
        boolean tagsEqual = ItemStack.matches(from, to);

        return !itemsEqual || !tagsEqual;
    }


    private boolean isBlocking() {
        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("None"))
            return false;

        LocalPlayer player = mc.player;
        if (player == null)
            return false;

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof SwordItem))
            return false;
        boolean isOffhandUsing = false;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }
        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null;
        if (isKillauraBlocking) {
            return true;
        }
        if (isOffhandUsing) {
            return false;
        }

        return mc.options.keyUse.isDown();
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (mc.player == null || mc.level == null)
            return;
        renderHUDItem(event);
    }

    private void renderHUDItem(EventRender event) {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.isEmpty())
            return;

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTicks = mc.getFrameTime();
        int packedLight = 15728880;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float itemX = screenWidth - 100;
        float itemY = screenHeight - 100;

        poseStack.translate(itemX, itemY, 0);

        float swingProgress = mc.player.getAttackAnim(partialTicks);
        if (swingProgress > 0) {
            float swingAngle = Mth.sin(swingProgress * swingProgress * (float) Math.PI) * 10.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(swingAngle));
        }

        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        renderItem(mc.player, mainHandItem, ItemDisplayContext.GUI, false, poseStack, bufferSource, packedLight);
    }

    public void renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float equipProgress,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProg,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light) {
        if (!player.isScoping()) {
            boolean flag = interactionHand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            OldHitting oldHitting = this;
            poseStack.pushPose();
            boolean skipOffhandShield = !flag &&
                    player.getOffhandItem().getItem() instanceof ShieldItem;
            if (!skipOffhandShield) {
                if (itemStack.isEmpty()) {
                    if (flag && !player.isInvisible()) {renderPlayerArm(poseStack, multiBufferSource, light, equippedProg, swingProgress, humanoidarm);
                    }
                } else if (itemStack.is(Items.FILLED_MAP)) {
                    if (flag && offHandItem.isEmpty()) {
                        renderTwoHandedMap(poseStack, multiBufferSource, light, equipProgress, equippedProg,
                                swingProgress);
                    } else {
                        renderOneHandedMap(poseStack, multiBufferSource, light, equippedProg, humanoidarm,
                                swingProgress, itemStack);
                    }
                } else {
                    boolean flag1 = itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
                    int i = humanoidarm == HumanoidArm.RIGHT ? 1 : -1;
                    if (itemStack.is(Items.CROSSBOW)) {
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4785682F), -0.094387F, 0.0573153F);
                            poseStack.mulPose(Axis.XP.rotation(-11.935F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.3F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                            float f6 = (float) itemStack.getUseDuration()
                                    - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                            float f10 = f6 / (float) CrossbowItem.getChargeDuration(itemStack);
                            f10 = Math.min(f10, 1.0F);
                            if (f10 > 0.1F) {
                                float f14 = Mth.sin((f6 - 0.1F) * 1.3F);
                                float f20 = f10 - 0.1F;
                                float f25 = f14 * f20;
                                poseStack.translate((double) (f25 * 0.0F), (double) (f25 * 0.004F),
                                        (double) (f25 * 0.0F));
                            }

                            poseStack.translate((double) (f10 * 0.0F), (double) (f10 * 0.0F), (double) (f10 * 0.04F));
                            poseStack.scale(1.0F, 1.0F, 1.0F + f10 * 0.2F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                        } else {
                            float f5 = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                            float f9 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
                            float f13 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                            poseStack.translate((double) ((float) i * f5), (double) f9, (double) f13);
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            if (flag1 && swingProgress < 0.001F && flag) {
                                poseStack.translate((double) ((float) i * -0.641864F), 0.0, 0.0);
                                poseStack.mulPose(Axis.YP.rotation((float) i * 10.0F * (float) Math.PI / 180.0F));
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                i == 1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                i != 1,
                                poseStack,
                                multiBufferSource,
                                light);
                    } else {
                        boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            switch (itemStack.getUseAnimation()) {
                                case NONE:
                                case BLOCK:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case EAT:
                                case DRINK:
                                    applyEatTransform(poseStack, partialTicks, humanoidarm, itemStack);
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case BOW:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.2785682F), 0.183444F, 0.1573153F);
                                    poseStack.mulPose(Axis.XP.rotation(-13.935F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f8 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f12 = f8 / 20.0F;
                                    f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
                                    f12 = Math.min(f12, 1.0F);
                                    if (f12 > 0.1F) {
                                        float f19 = Mth.sin((f8 - 0.1F) * 1.3F);
                                        float f24 = f12 - 0.1F;
                                        float f26 = f19 * f24;
                                        poseStack.translate((double) (f26 * 0.0F), (double) (f26 * 0.004F),
                                                (double) (f26 * 0.0F));
                                    }

                                    poseStack.translate((double) (f12 * 0.0F), (double) (f12 * 0.0F),
                                            (double) (f12 * 0.04F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    break;
                                case SPEAR:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.5F), 0.7F, 0.1F);
                                    poseStack.mulPose(Axis.XP.rotation(-55.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f7 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f11 = f7 / 10.0F;
                                    f11 = Math.min(f11, 1.0F);
                                    if (f11 > 0.1F) {
                                        float f18 = Mth.sin((f7 - 0.1F) * 1.3F);
                                        float f23 = f11 - 0.1F;
                                        float f4 = f18 * f23;
                                        poseStack.translate((double) (f4 * 0.0F), (double) (f4 * 0.004F),
                                                (double) (f4 * 0.0F));
                                    }

                                    poseStack.translate(0.0, 0.0, (double) (f11 * 0.2F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                            }
                        } else if ((player.isUsingItem()
                                || Minecraft.getInstance().options.keyUse.isDown()
                                || oldHitting.KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null)
                                && player.getMainHandItem().getItem() instanceof SwordItem
                                && !oldHitting.BlockMods.getCurrentMode().equals("None")) {
                            String s = oldHitting.BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.YP
                                            .rotation((float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(
                                            Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "push":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                    float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                            }
                        } else if (player.isAutoSpinAttack()) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4F), 0.8F, 0.3F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.0F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -85.0F * (float) Math.PI / 180.0F));
                        } else {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            if (itemStack.getItem() instanceof SwordItem &&
                                    (mc.options.keyUse.isDown() || (oldHitting.KillauraAutoBlock.getCurrentValue()
                                            && getAuraTarget() != null && getAuraTarget() instanceof LivingEntity
                                            && getTargetHudEnabled()))) {
                                String s = oldHitting.BlockMods.getCurrentMode().toLowerCase();
                                switch (s) {
                                    case "1.7":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.YP.rotation(
                                                (float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                        poseStack.scale(0.9F, 0.9F, 0.9F);
                                        poseStack.translate(-0.2F, 0.126F, 0.2F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "Push":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        break;
                                    default:
                                        applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                                }
                            } else {
                                applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                !flag2,
                                poseStack,
                                multiBufferSource,
                                light);
                    }
                }
            }

            poseStack.popPose();
        }
    }

    public LivingEntity getAuraTarget() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            try {
                java.lang.reflect.Field targetField = KillAura.class.getDeclaredField("target");
                targetField.setAccessible(true);
                return (LivingEntity) targetField.get(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean getTargetHudEnabled() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            try {
                java.lang.reflect.Field targetHudField = KillAura.class.getDeclaredField("targetHud");
                targetHudField.setAccessible(true);
                Object targetHudValue = targetHudField.get(killAura);
                if (targetHudValue != null) {
                    java.lang.reflect.Method getCurrentValueMethod = targetHudValue.getClass().getMethod("getCurrentValue");
                    return (Boolean) getCurrentValueMethod.invoke(targetHudValue);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                 float swingProgress, HumanoidArm arm) {
        boolean flag = arm == HumanoidArm.RIGHT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate((double) (f * (0.644764F + f2)), (double) (0.644764F + f3), (double) (0.644764F + f4));
        poseStack.mulPose(Axis.XP.rotation(-0.3F * Mth.sin(f1 * (float) (Math.PI * 2))));
        poseStack.mulPose(Axis.YP.rotation(f * 0.4F * Mth.sin(f1 * (float) Math.PI)));
        poseStack.mulPose(Axis.ZP.rotation(f * -0.4F * Mth.sin(swingProgress * (float) Math.PI)));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, flag ? mainHandItem : offHandItem,
                flag ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !flag,
                poseStack, bufferSource, light);
    }

    private void renderTwoHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equipProgress,
                                    float equippedProg, float swingProgress) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f1 / 2.0F), (double) f2);
        float f3 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f4 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, mainHandItem, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, poseStack,
                bufferSource, light);
        this.renderItem(mc.player, offHandItem, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, poseStack,
                bufferSource, light);
    }

    private void renderOneHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                    HumanoidArm arm, float swingProgress, ItemStack item) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate((double) (f * 0.125F), 0.0D, 0.0D);
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.1F * Mth.sin(f1 * (float) Math.PI);
        float f3 = -0.3F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f2 / 2.0F), (double) f4);
        poseStack.mulPose(Axis.XP.rotation(f3 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f * f1 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f * f2 * (float) Math.PI / 180.0F));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, item,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                arm != HumanoidArm.RIGHT, poseStack, bufferSource, light);
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equippedProg) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f1 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F + f * -0.6F), -0.72F);
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F), -0.72F);
        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
        float swingFactor = Mth.clamp(swingProgress, 0.0F, 1.0F);
        poseStack.mulPose(Axis.XP.rotation(f * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f1 * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f1 * -70.0F * swingFactor * (float) Math.PI / 180.0F));
    }

    private void applyEatTransform(PoseStack poseStack, float partialTicks, HumanoidArm arm, ItemStack item) {
        float f = (float) item.getUseDuration() - ((float) mc.player.getUseItemRemainingTicks() - partialTicks + 1.0F);
        float f1 = f / (float) item.getUseDuration();
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0D, (double) f2, 0.0D);
        }
        float f3 = 1.0F - (float) Math.pow((double) (1.0F - f1), 27.0D);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate((double) (f3 * 0.6F * (float) i), (double) (f3 * -0.5F), (double) (f3 * 0.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * f3 * 90.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.XP.rotation(f3 * 10.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * f3 * 30.0F * (float) Math.PI / 180.0F));
    }

    private void renderItem(LivingEntity entity, ItemStack stack,
                            ItemDisplayContext transformType, boolean leftHand,
                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (stack.isEmpty())
            return;
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderStatic(entity, stack, transformType, leftHand, poseStack, buffer, entity.level(), light, 0,
                0);
    }
}