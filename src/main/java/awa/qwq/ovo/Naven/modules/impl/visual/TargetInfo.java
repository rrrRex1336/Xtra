package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.Easing;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.animation.AnimationUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.awt.Color;
import java.util.Locale;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Vector4f;

@ModuleInfo(name = "TargetInfo", description = "Display your target info.", category = Category.VISUAL)
public class TargetInfo extends Module {
    private static final long TARGET_TIMEOUT_MS = 4000L;
    private static final float ROUNDED_EDGE = 8.0F;
    private static final float ROUNDED_PADDING = 7.0F;
    private static final float ROUNDED_INDENT = 4.0F;
    private static final float ROUNDED_FACE_SIZE = 30.0F;
    private static final float ROUNDED_RADIUS = 10.0F;

    private static LivingEntity target;
    private static long targetUpdateTime;
    private static boolean combatModuleTarget;

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Naven", "Rounded")
            .build()
            .getModeValue();
    private final FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
    private final FloatValue yOffset = DragManager.createHiddenPositionValue(this, "Drag Y", 0.0F);
    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);
    private Vector4f blurMatrix;
    private float blurRadius = 3.0F;
    private LivingEntity navenRenderTarget;
    private float navenAlpha;
    private float navenHealthPercent = -1.0F;
    private LivingEntity roundedRenderTarget;
    private float roundedScale;
    private float roundedScaleStart;
    private float roundedScaleTarget;
    private long roundedScaleStartTime;
    private float roundedHealthWidth = -1.0F;

    public static void trackTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == mc.player) {
            clearTarget();
            return;
        }

        if (isValidTarget(living)) {
            target = living;
            targetUpdateTime = System.currentTimeMillis();
            combatModuleTarget = isCurrentCombatTarget(living);
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (event.isPost()) {
            trackTarget(event.getTarget());
        }
    }

    @EventTarget
    public void onRender(EventRender2D event) {
        this.blurMatrix = null;
        this.blurRadius = 3.0F;
        setSuffix(this.mode.getCurrentMode());

        LivingEntity living = getDisplayTarget();
        if (living == null && DragManager.isHudEditorActive() && mc.player != null) {
            living = mc.player;
        }

        if (this.mode.isCurrentMode("Rounded")) {
            renderRounded(event, living);
            return;
        }

        if (!this.mode.isCurrentMode("Naven")) {
            return;
        }

        boolean opening = living != null;
        if (opening && this.navenRenderTarget != living) {
            this.navenRenderTarget = living;
            this.navenHealthPercent = -1.0F;
        }

        LivingEntity renderTarget = opening ? living : this.navenRenderTarget;
        this.navenAlpha = AnimationUtils.smooth(this.navenAlpha, opening ? 1.0F : 0.0F, 0.25F);
        if (renderTarget == null || this.navenAlpha <= 0.01F) {
            if (!opening) {
                this.navenRenderTarget = null;
            }
            return;
        }

        living = renderTarget;
        event.getStack().push();
        float baseX = mc.getWindow().getScaledWidth() / 2.0F + 10.0F;
        float baseY = mc.getWindow().getScaledHeight() / 2.0F + 10.0F;
        String targetName = displayName(living) + (living.isBaby() ? " (Baby)" : "");
        float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);

        this.dragManager.update(baseX, baseY, width, 30.0F);
        float x = this.dragManager.getX(baseX);
        float y = this.dragManager.getY(baseY);
        this.blurMatrix = new Vector4f(x, y, width, 30.0F);
        this.blurRadius = 3.0F;
        float targetHealthPercent = getHealthPercent(living);
        if (this.navenHealthPercent < 0.0F) {
            this.navenHealthPercent = targetHealthPercent;
        } else {
            this.navenHealthPercent = AnimationUtils.smooth(this.navenHealthPercent, targetHealthPercent, 0.35F);
        }

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(event.getStack(), x, y, width, 30.0F, 5.0F, scaleAlpha(WaterMark.headerColor, this.navenAlpha));
        StencilUtils.erase(true);
        RenderUtils.fillBound(event.getStack(), x, y, width, 30.0F, scaleAlpha(WaterMark.bodyColor, this.navenAlpha));
        RenderUtils.fillBound(event.getStack(), x, y, width * this.navenHealthPercent, 3.0F, scaleAlpha(WaterMark.headerColor, this.navenAlpha));
        StencilUtils.dispose();

        Fonts.harmony.setAlpha(this.navenAlpha);
        Fonts.harmony.render(event.getStack(), targetName, x + 5.0F, y + 6.0F, Color.WHITE, true, 0.35F);
        Fonts.harmony.render(event.getStack(), getHealthText(living), x + 5.0F, y + 17.0F, Color.WHITE, true, 0.35F);
        Fonts.harmony.setAlpha(1.0F);
        event.getStack().pop();
    }

    @EventTarget
    public void onShader(EventShader event) {
        if (this.blurMatrix != null) {
            if (this.mode.isCurrentMode("Rounded")) {
                if (event.getType() == EventType.BLUR) {
                    RenderUtils.drawRoundedRect(event.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), this.blurRadius, 1073741824);
                } else if (event.getType() == EventType.SHADOW) {
                    RenderUtils.drawRoundedRect(event.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), this.blurRadius, Integer.MIN_VALUE);
                }
            } else {
                RenderUtils.drawRoundedRect(event.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), this.blurRadius, 1073741824);
            }
        }
    }

    @Override
    public void onDisable() {
        this.blurMatrix = null;
        this.navenRenderTarget = null;
        this.navenAlpha = 0.0F;
        this.navenHealthPercent = -1.0F;
        this.roundedRenderTarget = null;
        this.roundedScale = 0.0F;
        this.roundedScaleStart = 0.0F;
        this.roundedScaleTarget = 0.0F;
        this.roundedHealthWidth = -1.0F;
        clearTarget();
        super.onDisable();
    }

    private void renderRounded(EventRender2D event, LivingEntity displayTarget) {
        boolean opening = displayTarget != null;
        if (opening && this.roundedRenderTarget != displayTarget) {
            this.roundedRenderTarget = displayTarget;
            this.roundedHealthWidth = -1.0F;
        }

        LivingEntity living = opening ? displayTarget : this.roundedRenderTarget;
        float scale = updateRoundedScale(opening);
        if (living == null || scale <= 0.01F) {
            if (!opening) {
                this.roundedRenderTarget = null;
            }
            return;
        }

        String name = displayName(living);
        String status = getRoundedStatus(living);
        String statusText = status + ": ";
        String healthText = getRoundedHealthText(living);
        CustomTextRenderer font = getRoundedFont();
        float titleScale = font == Fonts.productSansMedium ? 0.60F : font == Fonts.axiforma_regular ? 0.56F : 0.42F;
        float healthScale = font == Fonts.productSansMedium ? 0.62F : font == Fonts.axiforma_regular ? 0.58F : 0.45F;
        float statusWidth = font.getWidth(statusText, titleScale);
        float nameWidth = font.getWidth(name, titleScale);
        float healthTextWidth = font.getWidth(healthText, healthScale);
        float healthBarWidth = Math.max(statusWidth + nameWidth + 35.0F - healthTextWidth, 65.0F);
        float width = ROUNDED_EDGE + ROUNDED_FACE_SIZE + ROUNDED_EDGE + healthBarWidth + ROUNDED_INDENT + healthTextWidth + ROUNDED_EDGE;
        float height = ROUNDED_FACE_SIZE + ROUNDED_EDGE * 2.0F;
        float baseX = mc.getWindow().getScaledWidth() / 2.0F + 10.0F;
        float baseY = mc.getWindow().getScaledHeight() / 2.0F + 10.0F;

        this.dragManager.update(baseX, baseY, width, height);
        float x = this.dragManager.getX(baseX);
        float y = this.dragManager.getY(baseY);
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        this.blurMatrix = new Vector4f(
                x + width / 2.0F - scaledWidth / 2.0F,
                y + height / 2.0F - scaledHeight / 2.0F,
                scaledWidth,
                scaledHeight
        );
        this.blurRadius = ROUNDED_RADIUS * scale;

        float healthPercent = getHealthPercent(living);
        float targetHealthWidth = healthPercent * healthBarWidth;
        if (this.roundedHealthWidth < 0.0F) {
            this.roundedHealthWidth = targetHealthWidth;
        } else {
            this.roundedHealthWidth = AnimationUtils.smooth(this.roundedHealthWidth, targetHealthWidth, 0.35F);
        }

        float hurtTime = Math.max(0.0F, living.hurtTime - event.getPartialTicks()) * 0.5F;
        float faceOffset = hurtTime / 2.0F;
        float faceSize = Math.max(20.0F, ROUNDED_FACE_SIZE - hurtTime);
        float textX = x + ROUNDED_EDGE + ROUNDED_FACE_SIZE + ROUNDED_PADDING;
        float titleY = y + ROUNDED_EDGE + ROUNDED_INDENT + 2.0F;
        float barY = y + ROUNDED_EDGE + ROUNDED_FACE_SIZE - ROUNDED_INDENT - 7.0F;
        int accentTop = getRoundedAccent(y);
        int accentBottom = getRoundedAccent(y + height);

        MatrixStack stack = event.getStack();
        stack.push();
        stack.translate(x + width / 2.0F, y + height / 2.0F, 0.0F);
        stack.scale(scale, scale, 1.0F);
        stack.translate(-(x + width / 2.0F), -(y + height / 2.0F), 0.0F);

        RenderUtils.drawRoundedRect(stack, x, y, width - 1.0F, height, ROUNDED_RADIUS, new Color(0, 0, 0, 105).getRGB());
        renderRoundedHead(event, living, x + ROUNDED_EDGE + faceOffset, y + ROUNDED_EDGE + faceOffset, faceSize, hurtTime);

        font.render(stack, statusText, textX, titleY, Color.WHITE, true, titleScale);
        font.render(stack, name, textX + statusWidth + 3.0F, titleY + 0.5F, new Color(accentTop, true), true, titleScale);

        RenderUtils.drawRoundedRect(stack, textX, barY, healthBarWidth, 6.0F, 3.0F, new Color(25, 25, 25, 150).getRGB());
        drawRoundedGradientRect(stack, textX, barY, Math.min(healthBarWidth, this.roundedHealthWidth), 6.0F, 3.0F, accentBottom, accentTop);
        font.render(stack, healthText, textX + healthBarWidth + ROUNDED_INDENT, barY - 1.0F, new Color(accentTop, true), true, healthScale);

        stack.pop();
    }

    private float updateRoundedScale(boolean opening) {
        float target = opening ? 1.0F : 0.0F;
        if (this.roundedScaleTarget != target) {
            this.roundedScaleStart = this.roundedScale;
            this.roundedScaleTarget = target;
            this.roundedScaleStartTime = System.currentTimeMillis();
        }

        long duration = opening ? 850L : 400L;
        float progress = Math.min(1.0F, (System.currentTimeMillis() - this.roundedScaleStartTime) / (float) duration);
        float eased = opening ? Easing.EASE_OUT_ELASTIC.apply(progress) : Easing.EASE_IN_BACK.apply(progress);
        this.roundedScale = this.roundedScaleStart + (this.roundedScaleTarget - this.roundedScaleStart) * eased;
        if (progress >= 1.0F) {
            this.roundedScale = this.roundedScaleTarget;
        }
        return Math.max(0.0F, this.roundedScale);
    }

    private void renderRoundedHead(EventRender2D event, LivingEntity living, float x, float y, float size, float hurtTime) {
        MatrixStack stack = event.getStack();
        RenderUtils.drawRoundedRect(stack, x - 1.0F, y + 2.0F, size + 2.0F, size + 2.0F, 9.0F, new Color(0, 0, 0, 80).getRGB());
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(stack, x, y, size, size, 8.0F, Color.WHITE.getRGB());
        StencilUtils.erase(true);

        if (living instanceof AbstractClientPlayerEntity player) {
            boolean upsideDown = LivingEntityRenderer.shouldFlipUpsideDown(player);
            boolean hasHat = player.isPartVisible(PlayerModelPart.HAT);
            PlayerSkinDrawer.draw(event.getGuiGraphics(), player.getSkinTextures().texture(), (int) x, (int) y, (int) size, hasHat, upsideDown);
        } else {
            RenderUtils.drawRoundedRect(stack, x, y, size, size, 8.0F, new Color(35, 35, 35, 255).getRGB());
            String first = living.getName().getString().isEmpty() ? "?" : living.getName().getString().substring(0, 1);
            CustomTextRenderer font = getRoundedFont();
            float scale = font == Fonts.productSansMedium ? 0.78F : font == Fonts.axiforma_regular ? 0.65F : 0.5F;
            font.render(stack, first, x + size / 2.0F - font.getWidth(first, scale) / 2.0F, y + size / 2.0F - 5.0F, Color.WHITE, true, scale);
        }

        if (hurtTime > 0.0F) {
            RenderUtils.drawRoundedRect(stack, x, y, size, size, 8.0F, alphaColor(0xFFFF3030, Math.min(0.65F, hurtTime / 9.0F)));
        }

        StencilUtils.dispose();
    }

    private static LivingEntity getDisplayTarget() {
        if (target == null) {
            return null;
        }

        if (!isValidTarget(target)
                || combatModuleTarget && !isCurrentCombatTarget(target)
                || !combatModuleTarget && !isCurrentLookTarget(target)
                || !combatModuleTarget && System.currentTimeMillis() - targetUpdateTime > TARGET_TIMEOUT_MS) {
            clearTarget();
            return null;
        }

        return target;
    }

    private static boolean isValidTarget(LivingEntity living) {
        if (living == null || mc.player == null || mc.world == null) {
            return false;
        }

        if (living == mc.player || living.getWorld() != mc.world || living.isRemoved() || !living.isAlive() || living.isDead() || living.getHealth() <= 0.0F) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity == living) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCurrentCombatTarget(Entity entity) {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        return killAura != null && killAura.isEnabled() && (KillAura.target == entity || KillAura.targets.contains(entity))
                || aura != null && aura.isEnabled() && (Aura.target == entity || Aura.targets.contains(entity));
    }

    private static boolean isCurrentLookTarget(Entity entity) {
        return mc.crosshairTarget instanceof EntityHitResult hitResult && hitResult.getEntity() == entity;
    }

    private static String getRoundedStatus(LivingEntity living) {
        float selfHealth = mc.player == null ? 0.0F : mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float targetHealth = living.getHealth() + living.getAbsorptionAmount();
        return selfHealth >= targetHealth ? "Winning" : "Losing";
    }

    private static String getRoundedHealthText(LivingEntity living) {
        return String.format(Locale.US, "%.1f", Math.min(living.getHealth(), living.getMaxHealth()));
    }

    private static int getRoundedAccent(float y) {
        return opaque(Naven.getInstance().getThemeManager().getColor(y));
    }

    private static CustomTextRenderer getRoundedFont() {
        if (Fonts.productSansMedium != null) return Fonts.productSansMedium;
        return Fonts.axiforma_regular != null ? Fonts.axiforma_regular : Fonts.harmony;
    }

    private static void drawRoundedGradientRect(MatrixStack stack, float x, float y, float width, float height, float radius, int leftColor, int rightColor) {
        if (width <= 0.0F || height <= 0.0F) return;
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(stack, x, y, width, height, radius, Color.WHITE.getRGB());
        StencilUtils.erase(true);

        int steps = Math.max(1, Math.min(24, (int) Math.ceil(width / 4.0F)));
        for (int i = 0; i < steps; i++) {
            float start = i / (float) steps;
            float end = (i + 1) / (float) steps;
            float stripX = x + width * start;
            float stripWidth = width * (end - start) + 0.75F;
            RenderUtils.fillBound(stack, stripX, y, stripWidth, height, mixColor(leftColor, rightColor, (start + end) * 0.5F));
        }

        StencilUtils.dispose();
    }

    private static int mixColor(int first, int second, float progress) {
        float t = Math.max(0.0F, Math.min(1.0F, progress));
        int a1 = first >>> 24;
        int r1 = first >> 16 & 0xFF;
        int g1 = first >> 8 & 0xFF;
        int b1 = first & 0xFF;
        int a2 = second >>> 24;
        int r2 = second >> 16 & 0xFF;
        int g2 = second >> 8 & 0xFF;
        int b2 = second & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int alphaColor(int color, float alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (color & 0x00FFFFFF) | (clampedAlpha << 24);
    }

    private static int scaleAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }
        int clampedAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * Math.max(0.0F, Math.min(1.0F, alpha)))));
        return (color & 0x00FFFFFF) | (clampedAlpha << 24);
    }

    private static int opaque(int color) {
        return (color & 0x00FFFFFF) | 0xFF000000;
    }

    private static void clearTarget() {
        target = null;
        targetUpdateTime = 0L;
        combatModuleTarget = false;
    }

    private static float getHealthPercent(LivingEntity living) {
        float maxHealth = Math.max(1.0F, living.getMaxHealth());
        return Math.max(0.0F, Math.min(1.0F, living.getHealth() / maxHealth));
    }

    private static String getHealthText(LivingEntity living) {
        return "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "");
    }

    private static String displayName(LivingEntity living) {
        return living.getName().getString();
    }
}
