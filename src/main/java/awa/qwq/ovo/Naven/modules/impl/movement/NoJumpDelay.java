package awa.qwq.ovo.Naven.modules.impl.movement;

import org.mixin.accessors.LivingEntityAccessor;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;

@ModuleInfo(
   name = "NoJumpDelay",
   description = "Removes the delay when jumping.",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (mc.player != null) {
                ((LivingEntityAccessor) mc.player).setNoJumpDelay(0);
            }
        }
    }
}

