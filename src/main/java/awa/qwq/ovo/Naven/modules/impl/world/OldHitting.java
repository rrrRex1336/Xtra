package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

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
    private final MinecraftClient mc = MinecraftClient.getInstance();
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
        if (event.getType() == EventType.SEND && event.getPacket() instanceof HandSwingC2SPacket) {
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

        ClientPlayerEntity localplayer = mc.player;
        ItemStack itemstack = localplayer.getMainHandStack();
        ItemStack itemstack1 = localplayer.getOffHandStack();
        boolean isBlocking = isBlocking();

        if (isBlocking) {
            mainHandHeight = 1.0F;
            if (ItemStack.areEqual(mainHandItem, itemstack)) {
                mainHandItem = itemstack;
            }
            if (ItemStack.areEqual(offHandItem, itemstack1)) {
                offHandItem = itemstack1;
            }
            return;
        }

        if (localplayer.isRiding()) {
            mainHandHeight = MathHelper.clamp(mainHandHeight - 0.4F, 0.0F, 1.0F);
            offHandHeight = MathHelper.clamp(offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackCooldownProgress(1.0F);

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

            mainHandHeight += MathHelper.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);
            offHandHeight += MathHelper.clamp(targetOffHeight - offHandHeight, -0.2F, 0.2F);
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

        boolean itemsEqual = ItemStack.areItemsEqual(from, to);
        boolean tagsEqual = ItemStack.areEqual(from, to);

        return !itemsEqual || !tagsEqual;
    }


    private boolean isBlocking() {
        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("None"))
            return false;

        ClientPlayerEntity player = mc.player;
        if (player == null)
            return false;

        ItemStack mainHandItem = player.getMainHandStack();
        if (!(mainHandItem.getItem() instanceof SwordItem))
            return false;
        boolean isOffhandUsing = false;
        if (player.isUsingItem() && player.getActiveHand() == Hand.OFF_HAND) {
            ItemStack offhandItem = player.getOffHandStack();
            UseAction useAnim = offhandItem.getUseAction();
            if (useAnim != UseAction.BLOCK) {
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

        return mc.options.useKey.isPressed();
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (mc.player == null || mc.world == null)
            return;
        renderHUDItem(event);
    }

    private void renderHUDItem(EventRender event) {
        ItemStack mainHandItem = mc.player.getMainHandStack();
        if (mainHandItem.isEmpty())
            return;

        MatrixStack poseStack = new MatrixStack();
        VertexConsumerProvider bufferSource = mc.getBufferBuilders().getEntityVertexConsumers();
        float partialTicks = mc.getTickDelta();
        int packedLight = 15728880;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float itemX = screenWidth - 100;
        float itemY = screenHeight - 100;

        poseStack.translate(itemX, itemY, 0);

        float swingProgress = mc.player.getHandSwingProgress(partialTicks);
        if (swingProgress > 0) {
            float swingAngle = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * 10.0F;
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swingAngle));
        }

        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        renderItem(mc.player, mainHandItem, ModelTransformationMode.GUI, false, poseStack, bufferSource, packedLight);
    }

    public void renderArmWithItem(
            AbstractClientPlayerEntity player,
            float partialTicks,
            float equipProgress,
            Hand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProg,
            MatrixStack poseStack,
            VertexConsumerProvider multiBufferSource,
            int light) {
        if (!player.isUsingSpyglass()) {
            boolean flag = interactionHand == Hand.MAIN_HAND;
            Arm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            OldHitting oldHitting = this;
            poseStack.push();
            boolean skipOffhandShield = !flag &&
                    player.getOffHandStack().getItem() instanceof ShieldItem;
            if (!skipOffhandShield) {
                if (itemStack.isEmpty()) {
                    if (flag && !player.isInvisible()) {renderPlayerArm(poseStack, multiBufferSource, light, equippedProg, swingProgress, humanoidarm);
                    }
                } else if (itemStack.isOf(Items.FILLED_MAP)) {
                    if (flag && offHandItem.isEmpty()) {
                        renderTwoHandedMap(poseStack, multiBufferSource, light, equipProgress, equippedProg,
                                swingProgress);
                    } else {
                        renderOneHandedMap(poseStack, multiBufferSource, light, equippedProg, humanoidarm,
                                swingProgress, itemStack);
                    }
                } else {
                    boolean flag1 = itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
                    int i = humanoidarm == Arm.RIGHT ? 1 : -1;
                    if (itemStack.isOf(Items.CROSSBOW)) {
                        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0
                                && player.getActiveHand() == interactionHand) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4785682F), -0.094387F, 0.0573153F);
                            poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-11.935F * (float) Math.PI / 180.0F));
                            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 65.3F * (float) Math.PI / 180.0F));
                            poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                            float f6 = (float) itemStack.getMaxUseTime()
                                    - ((float) player.getItemUseTimeLeft() - partialTicks + 1.0F);
                            float f10 = f6 / (float) CrossbowItem.getPullTime(itemStack);
                            f10 = Math.min(f10, 1.0F);
                            if (f10 > 0.1F) {
                                float f14 = MathHelper.sin((f6 - 0.1F) * 1.3F);
                                float f20 = f10 - 0.1F;
                                float f25 = f14 * f20;
                                poseStack.translate((double) (f25 * 0.0F), (double) (f25 * 0.004F),
                                        (double) (f25 * 0.0F));
                            }

                            poseStack.translate((double) (f10 * 0.0F), (double) (f10 * 0.0F), (double) (f10 * 0.04F));
                            poseStack.scale(1.0F, 1.0F, 1.0F + f10 * 0.2F);
                            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                        } else {
                            float f5 = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                            float f9 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                            float f13 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                            poseStack.translate((double) ((float) i * f5), (double) f9, (double) f13);
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            if (flag1 && swingProgress < 0.001F && flag) {
                                poseStack.translate((double) ((float) i * -0.641864F), 0.0, 0.0);
                                poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 10.0F * (float) Math.PI / 180.0F));
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                i == 1 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                                        : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                                i != 1,
                                poseStack,
                                multiBufferSource,
                                light);
                    } else {
                        boolean flag2 = humanoidarm == Arm.RIGHT;
                        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0
                                && player.getActiveHand() == interactionHand) {
                            switch (itemStack.getUseAction()) {
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
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-13.935F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f8 = (float) itemStack.getMaxUseTime()
                                            - ((float) player.getItemUseTimeLeft() - partialTicks + 1.0F);
                                    float f12 = f8 / 20.0F;
                                    f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
                                    f12 = Math.min(f12, 1.0F);
                                    if (f12 > 0.1F) {
                                        float f19 = MathHelper.sin((f8 - 0.1F) * 1.3F);
                                        float f24 = f12 - 0.1F;
                                        float f26 = f19 * f24;
                                        poseStack.translate((double) (f26 * 0.0F), (double) (f26 * 0.004F),
                                                (double) (f26 * 0.0F));
                                    }

                                    poseStack.translate((double) (f12 * 0.0F), (double) (f12 * 0.0F),
                                            (double) (f12 * 0.04F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    break;
                                case SPEAR:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.5F), 0.7F, 0.1F);
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-55.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f7 = (float) itemStack.getMaxUseTime()
                                            - ((float) player.getItemUseTimeLeft() - partialTicks + 1.0F);
                                    float f11 = f7 / 10.0F;
                                    f11 = Math.min(f11, 1.0F);
                                    if (f11 > 0.1F) {
                                        float f18 = MathHelper.sin((f7 - 0.1F) * 1.3F);
                                        float f23 = f11 - 0.1F;
                                        float f4 = f18 * f23;
                                        poseStack.translate((double) (f4 * 0.0F), (double) (f4 * 0.004F),
                                                (double) (f4 * 0.0F));
                                    }

                                    poseStack.translate(0.0, 0.0, (double) (f11 * 0.2F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                            }
                        } else if ((player.isUsingItem()
                                || MinecraftClient.getInstance().options.useKey.isPressed()
                                || oldHitting.KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null)
                                && player.getMainHandStack().getItem() instanceof SwordItem
                                && !oldHitting.BlockMods.getCurrentMode().equals("None")) {
                            String s = oldHitting.BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    float f17 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f22 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.multiply(RotationAxis.POSITIVE_Y
                                            .rotation((float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.multiply(
                                            RotationAxis.POSITIVE_Z.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "push":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                    float f15 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f3 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                            }
                        } else if (player.isUsingRiptide()) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4F), 0.8F, 0.3F);
                            poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 65.0F * (float) Math.PI / 180.0F));
                            poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * -85.0F * (float) Math.PI / 180.0F));
                        } else {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            if (itemStack.getItem() instanceof SwordItem &&
                                    (mc.options.useKey.isPressed() || (oldHitting.KillauraAutoBlock.getCurrentValue()
                                            && getAuraTarget() != null && getAuraTarget() instanceof LivingEntity
                                            && getTargetHudEnabled()))) {
                                String s = oldHitting.BlockMods.getCurrentMode().toLowerCase();
                                switch (s) {
                                    case "1.7":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        float f17 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f22 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(
                                                (float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Z.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Y.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                        poseStack.scale(0.9F, 0.9F, 0.9F);
                                        poseStack.translate(-0.2F, 0.126F, 0.2F);
                                        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Y.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Z.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "Push":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Y.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(
                                                RotationAxis.POSITIVE_Z.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        float f15 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f3 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
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
                                flag2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                                        : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                                !flag2,
                                poseStack,
                                multiBufferSource,
                                light);
                    }
                }
            }

            poseStack.pop();
        }
    }

    public LivingEntity getAuraTarget() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            LivingEntity target = asLivingTarget(KillAura.target);
            if (target != null) {
                return target;
            }
        }

        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura != null && aura.isEnabled()) {
            return asLivingTarget(Aura.target);
        }
        return null;
    }

    private LivingEntity asLivingTarget(Object target) {
        if (target instanceof LivingEntity living && living.isAlive() && living.getHealth() > 0.0F) {
            return living;
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

    private void renderPlayerArm(MatrixStack poseStack, VertexConsumerProvider bufferSource, int light, float equippedProg,
                                 float swingProgress, Arm arm) {
        boolean flag = arm == Arm.RIGHT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = MathHelper.sqrt(swingProgress);
        float f2 = -0.3F * MathHelper.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * MathHelper.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
        poseStack.translate((double) (f * (0.644764F + f2)), (double) (0.644764F + f3), (double) (0.644764F + f4));
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-0.3F * MathHelper.sin(f1 * (float) (Math.PI * 2))));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f * 0.4F * MathHelper.sin(f1 * (float) Math.PI)));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f * -0.4F * MathHelper.sin(swingProgress * (float) Math.PI)));
        float f5 = MathHelper.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = MathHelper.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, flag ? mainHandItem : offHandItem,
                flag ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !flag,
                poseStack, bufferSource, light);
    }

    private void renderTwoHandedMap(MatrixStack poseStack, VertexConsumerProvider bufferSource, int light, float equipProgress,
                                    float equippedProg, float swingProgress) {
        float f = MathHelper.sqrt(swingProgress);
        float f1 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * MathHelper.sin(f * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f1 / 2.0F), (double) f2);
        float f3 = MathHelper.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f4 = MathHelper.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, mainHandItem, ModelTransformationMode.FIRST_PERSON_RIGHT_HAND, false, poseStack,
                bufferSource, light);
        this.renderItem(mc.player, offHandItem, ModelTransformationMode.FIRST_PERSON_LEFT_HAND, true, poseStack,
                bufferSource, light);
    }

    private void renderOneHandedMap(MatrixStack poseStack, VertexConsumerProvider bufferSource, int light, float equippedProg,
                                    Arm arm, float swingProgress, ItemStack item) {
        float f = arm == Arm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate((double) (f * 0.125F), 0.0D, 0.0D);
        float f1 = MathHelper.sqrt(swingProgress);
        float f2 = -0.1F * MathHelper.sin(f1 * (float) Math.PI);
        float f3 = -0.3F * MathHelper.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f2 / 2.0F), (double) f4);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f3 * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f * f1 * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f * f2 * (float) Math.PI / 180.0F));
        float f5 = MathHelper.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = MathHelper.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, item,
                arm == Arm.RIGHT ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                        : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                arm != Arm.RIGHT, poseStack, bufferSource, light);
    }

    private void applyItemArmTransform(MatrixStack poseStack, Arm arm, float equippedProg) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f1 = MathHelper.lerp(equippedProg, oOffHandHeight, offHandHeight);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F + f * -0.6F), -0.72F);
    }

    private void applyItemArmAttackTransform(MatrixStack poseStack, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F), -0.72F);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-102.25F * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
        float swingFactor = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(f1 * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(f1 * -70.0F * swingFactor * (float) Math.PI / 180.0F));
    }

    private void applyEatTransform(MatrixStack poseStack, float partialTicks, Arm arm, ItemStack item) {
        float f = (float) item.getMaxUseTime() - ((float) mc.player.getItemUseTimeLeft() - partialTicks + 1.0F);
        float f1 = f / (float) item.getMaxUseTime();
        if (f1 < 0.8F) {
            float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0D, (double) f2, 0.0D);
        }
        float f3 = 1.0F - (float) Math.pow((double) (1.0F - f1), 27.0D);
        int i = arm == Arm.RIGHT ? 1 : -1;
        poseStack.translate((double) (f3 * 0.6F * (float) i), (double) (f3 * -0.5F), (double) (f3 * 0.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) i * f3 * 90.0F * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_X.rotation(f3 * 10.0F * (float) Math.PI / 180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotation((float) i * f3 * 30.0F * (float) Math.PI / 180.0F));
    }

    private void renderItem(LivingEntity entity, ItemStack stack,
                            ModelTransformationMode transformType, boolean leftHand,
                            MatrixStack poseStack, VertexConsumerProvider buffer, int light) {
        if (stack.isEmpty())
            return;
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderItem(entity, stack, transformType, leftHand, poseStack, buffer, entity.getWorld(), light, 0,
                0);
    }
}
