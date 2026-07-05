package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.chat.IrcClient;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.awt.Color;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;

@ModuleInfo(name = "TargetHUD", description = "Display your target info.", category = Category.VISUAL)
public class TargetHUD extends Module {
    private static final long TARGET_TIMEOUT_MS = 4000L;

    private static LivingEntity target;
    private static long targetUpdateTime;
    private static boolean combatModuleTarget;

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Naven")
            .build()
            .getModeValue();
    private final FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
    private final FloatValue yOffset = DragManager.createHiddenPositionValue(this, "Drag Y", 0.0F);
    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);
    private Vector4f blurMatrix;

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
        setSuffix(this.mode.getCurrentMode());

        LivingEntity living = getDisplayTarget();
        if (living == null && DragManager.isHudEditorActive() && mc.player != null) {
            living = mc.player;
        }

        if (living == null || !this.mode.isCurrentMode("Naven")) {
            return;
        }

        event.getStack().pushPose();
        float baseX = mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
        float baseY = mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
        String targetName = displayName(living) + (living.isBaby() ? " (Baby)" : "");
        float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);

        this.dragManager.update(baseX, baseY, width, 30.0F);
        float x = this.dragManager.getX(baseX);
        float y = this.dragManager.getY(baseY);
        this.blurMatrix = new Vector4f(x, y, width, 30.0F);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(event.getStack(), x, y, width, 30.0F, 5.0F, WaterMark.headerColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(event.getStack(), x, y, width, 30.0F, WaterMark.bodyColor);
        RenderUtils.fillBound(event.getStack(), x, y, width * getHealthPercent(living), 3.0F, WaterMark.headerColor);
        StencilUtils.dispose();

        Fonts.harmony.render(event.getStack(), targetName, x + 5.0F, y + 6.0F, Color.WHITE, true, 0.35F);
        Fonts.harmony.render(event.getStack(), getHealthText(living), x + 5.0F, y + 17.0F, Color.WHITE, true, 0.35F);
        event.getStack().popPose();
    }

    @EventTarget
    public void onShader(EventShader event) {
        if (this.blurMatrix != null) {
            RenderUtils.drawRoundedRect(event.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }

    @Override
    public void onDisable() {
        this.blurMatrix = null;
        clearTarget();
        super.onDisable();
    }

    private static LivingEntity getDisplayTarget() {
        if (target == null) {
            return null;
        }

        if (!isValidTarget(target)
                || combatModuleTarget && !isCurrentCombatTarget(target)
                || !combatModuleTarget && System.currentTimeMillis() - targetUpdateTime > TARGET_TIMEOUT_MS) {
            clearTarget();
            return null;
        }

        return target;
    }

    private static boolean isValidTarget(LivingEntity living) {
        if (living == null || mc.player == null || mc.level == null) {
            return false;
        }

        if (living == mc.player || living.level() != mc.level || living.isRemoved() || !living.isAlive() || living.isDeadOrDying() || living.getHealth() <= 0.0F) {
            return false;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == living) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCurrentCombatTarget(Entity entity) {
        return KillAura.target == entity
                || KillAura.targets.contains(entity)
                || Aura.target == entity
                || Aura.targets.contains(entity);
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
        String name = living.getName().getString();
        if (IrcClient.isIrcUser(name)) {
            return awa.qwq.ovo.Naven.modules.impl.misc.IRC.ircStatusPrefix(name) + name;
        }
        return name;
    }
}
