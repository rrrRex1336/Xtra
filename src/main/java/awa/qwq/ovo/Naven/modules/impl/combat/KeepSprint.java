package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.util.hit.HitResult;

@ModuleInfo(name = "KeepSprint", description = "Maintain a sprinting state while attacking.", category = Category.COMBAT)
public class KeepSprint extends Module {
    private static final double GRIM2_MOTION_XZ = 0.8D;
    private static final double PREDICTION2_CLOSE_MOTION_XZ = 0.8D;

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Vanilla", "Prediction", "Grim", "Grim2", "Prediction2")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public final BooleanValue fullSprint = ValueBuilder.create(this, "Full Sprint")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Grim"))
            .build()
            .getBooleanValue();

    public final BooleanValue prediction = ValueBuilder.create(this, "Prediction")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Prediction2"))
            .build()
            .getBooleanValue();

    public final FloatValue slowdown = ValueBuilder.create(this, "Slowdown")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("Prediction2"))
            .build()
            .getFloatValue();

    public final BooleanValue groundOnly = ValueBuilder.create(this, "Ground Only")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Prediction2"))
            .build()
            .getBooleanValue();

    public final BooleanValue reachOnly = ValueBuilder.create(this, "Reach Only")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Prediction2"))
            .build()
            .getBooleanValue();

    private int wTapTicks;
    private boolean restoreForward;
    private boolean restoreSprint;
    private boolean attackForward;
    private boolean attackSprinting;
    private boolean canSprint;

    @Override
    public void onDisable() {
        resetWTap();
        this.canSprint = false;
        super.onDisable();
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (!mode.isCurrentMode("Prediction2")) {
            this.canSprint = false;
            return;
        }

        if (event.getType() == EventType.PRE) {
            this.canSprint = false;
        } else if (event.getType() == EventType.POST) {
            this.canSprint = true;
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (!mode.isCurrentMode("Grim2") || mc.player == null || mc.world == null || event.getTarget() == null) {
            return;
        }
        if (!event.isPost()) {
            this.attackForward = mc.options.forwardKey.isPressed();
            this.attackSprinting = mc.player.isSprinting() || mc.options.sprintKey.isPressed();
            return;
        }

        this.wTapTicks = 2;
        this.restoreForward = this.attackForward || mc.options.forwardKey.isPressed();
        this.restoreSprint = this.attackSprinting || mc.options.sprintKey.isPressed();
    }

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        if (mode.isCurrentMode("Vanilla")) {
            e.setCancelled(true);
        } else if (mode.isCurrentMode("Prediction")) {
            if (e.getType() == EventAttackSlowdown.Type.Sprinting) {
                e.setCancelled(true);
            }
        } else if (mode.isCurrentMode("Grim")) {
            if (fullSprint.getCurrentValue() && e.getType() == EventAttackSlowdown.Type.Sprinting) {
                e.setCancelled(true);
            } else if (e.getType() == EventAttackSlowdown.Type.Delta_Movement) {
                e.setMotionXZ(1.0D);
            }
        } else if (mode.isCurrentMode("Grim2")) {
            if (e.getType() == EventAttackSlowdown.Type.Delta_Movement) {
                e.setMotionXZ(GRIM2_MOTION_XZ);
            }
        } else if (mode.isCurrentMode("Prediction2")) {
            handlePrediction2Slowdown(e);
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (!mode.isCurrentMode("Grim2")) {
            resetWTap();
            return;
        }
        if (mc.player == null || this.wTapTicks <= 0) {
            return;
        }

        if (this.wTapTicks == 2) {
            event.setForward(0.0F);
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
        } else {
            if (this.restoreForward && mc.options.forwardKey.isPressed()) {
                event.setForward(1.0F);
            }
            if (this.restoreSprint && canRestoreSprint()) {
                mc.player.setSprinting(true);
            }
        }

        --this.wTapTicks;
        if (this.wTapTicks <= 0) {
            resetWTap();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        setSuffix(mode.getCurrentMode());
        if (!mode.isCurrentMode("Grim2") || mc.player == null) {
            resetWTap();
        }
        if (!mode.isCurrentMode("Prediction2")) {
            this.canSprint = false;
        }
    }

    private boolean canRestoreSprint() {
        return mc.player != null
                && mc.options.forwardKey.isPressed()
                && !mc.player.isUsingItem()
                && !mc.player.isInSneakingPose();
    }

    private void resetWTap() {
        this.wTapTicks = 0;
        this.restoreForward = false;
        this.restoreSprint = false;
        this.attackForward = false;
        this.attackSprinting = false;
    }

    private boolean shouldKeepSprint() {
        if (mc.player == null) {
            return false;
        }
        if (this.prediction.getCurrentValue() && !this.canSprint) {
            return false;
        }
        if (this.groundOnly.getCurrentValue() && !mc.player.isOnGround()) {
            return false;
        }
        return true;
    }

    private void handlePrediction2Slowdown(EventAttackSlowdown event) {
        if (!shouldKeepSprint()) {
            return;
        }

        boolean reachHit = isReachHit();
        if (reachHit) {
            if (event.getType() == EventAttackSlowdown.Type.Sprinting) {
                event.setCancelled(true);
            } else if (event.getType() == EventAttackSlowdown.Type.Delta_Movement) {
                event.setMotionXZ(getMotionXZ());
            }
            return;
        }

        if (this.reachOnly.getCurrentValue()) {
            return;
        }
        if (event.getType() == EventAttackSlowdown.Type.Delta_Movement) {
            event.setMotionXZ(Math.min(getMotionXZ(), PREDICTION2_CLOSE_MOTION_XZ));
        }
    }

    private boolean isReachHit() {
        if (mc.player == null || mc.crosshairTarget == null || mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (mc.getCameraEntity() == null) {
            return mc.crosshairTarget.getPos().distanceTo(mc.player.getCameraPosVec(1.0F)) > 3.0D;
        }
        return mc.crosshairTarget.getPos().distanceTo(mc.getCameraEntity().getCameraPosVec(1.0F)) > 3.0D;
    }

    private double getMotionXZ() {
        double slowdown = Math.max(0.0D, Math.min(100.0D, this.slowdown.getCurrentValue())) / 100.0D;
        return 1.0D - 0.4D * slowdown;
    }
}
