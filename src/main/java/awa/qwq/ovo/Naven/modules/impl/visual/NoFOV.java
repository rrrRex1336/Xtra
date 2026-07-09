package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventUpdateFoV;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.PlayerUtils;

@ModuleInfo(
        name = "NoFOV",
        description = "Prevents sprint FOV changes.",
        category = Category.VISUAL
)
public class NoFOV extends Module {
    @EventTarget(0)
    public void onUpdateFoV(EventUpdateFoV event) {
        if (mc.player == null) {
            return;
        }

        event.setFov(1.0F + PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
    }
}
