package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

@ModuleInfo(name = "KeepSprint", description = "Maintain a sprinting state while attacking.", category = Category.COMBAT)
public class KeepSprint extends Module {

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Vanilla", "Prediction", "Grim")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        if (mode.isCurrentMode("Vanilla")) {
            e.setCancelled(true);
        } else if (mode.isCurrentMode("Prediction")) {
            if (e.getType() == EventAttackSlowdown.Type.Sprinting) {
                e.setCancelled(true);
            }
        } else if (mode.isCurrentMode("Grim")) {
            if (e.getType() == EventAttackSlowdown.Type.Sprinting) {
                e.setCancelled(true);
            } else if (e.getType() == EventAttackSlowdown.Type.Delta_Movement && mc.player.tickCount % 2 == 0) {
                e.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        setSuffix(mode.getCurrentMode());
    }
}
