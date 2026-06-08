package awa.qwq.ovo.Naven.modules.impl.Combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

@ModuleInfo(name = "KeepSprint", description = "Maintain a sprinting state while attacking.", category = Category.COMBAT)
public class KeepSprint extends Module {

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Vanilla", "Prediction", "Grim")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private int sprintTickCounter = 0;
    private boolean shouldAttack = false;

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        if (mode.isCurrentMode("Vanilla") || mode.isCurrentMode("Grim")) {
            e.setCancelled(true);
        } else if (mode.isCurrentMode("Prediction")) {
            if (e.getType() == EventAttackSlowdown.Type.Sprinting) {
                e.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onSprint(EventSprint event) {
        if (!isEnabled()) return;

        if (mode.isCurrentMode("Grim")) {
            sprintTickCounter++;
            if (mc.player != null) {
                if (sprintTickCounter % 2 == 0) {
                    mc.player.setSprinting(false);
                    shouldAttack = true;
                } else {
                    mc.player.setSprinting(true);
                    shouldAttack = false;
                }
            }
        } else if (mode.isCurrentMode("Vanilla") || mode.isCurrentMode("Prediction")) {
            if (mc.player != null && !mc.player.isSprinting() && MoveUtils.isMoving()) {
                mc.player.setSprinting(true);
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (mode.isCurrentMode("Grim")) {
            if (!shouldAttack) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        setSuffix(mode.getCurrentMode());
    }
}