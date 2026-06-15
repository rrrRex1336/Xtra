package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventStrafe2;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;

@ModuleInfo(
        name = "LegitSpeed",
        description = "Make you move speed faster",
        category = Category.MOVEMENT
)
public class LegitSpeed extends Module {

    @EventTarget
    public void onStrafe(EventStrafe2 event) {
        if (mc.player == null) return;
        event.setFriction(event.getFriction() * 1.002F);
    }

    @EventTarget
    public void onRunTicks(EventRunTicks e) {
        if (e.getType() != EventType.PRE) return;
        if (mc.player == null) return;
        Naven.TICK_TIMER = 1F / 1.004F;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        Naven.TICK_TIMER = 1F;
        super.onDisable();
    }
}