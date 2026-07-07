package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;

@ModuleInfo(name = "StopMove", description = "Stops movement", category = Category.MOVEMENT)
public class StopMove extends Module {
    public FloatValue skipTicks = ValueBuilder.create(this, "Skip Ticks").setDefaultFloatValue(19.0f).setMinFloatValue(1.0f).setMaxFloatValue(19.0f).setFloatStep(1f).build().getFloatValue();

    @Override
    public void onEnable() {
        SkipTicks.skipTicks(skipTicks.getCurrentValue());
    }

    @Override
    public void onDisable() {
        SkipTicks.dispatch();
    }
}
