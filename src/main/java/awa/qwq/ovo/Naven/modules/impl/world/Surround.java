package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;

@ModuleInfo(name = "Surround", description = "Find suitable conditions and place blocks to surround yourself.", category = Category.WORLD)
public class Surround extends Module {

    public final FloatValue delay = ValueBuilder.create(this, "Delay")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMaxFloatValue(20.0F)
            .setMinFloatValue(1.0F)
            .build()
            .getFloatValue();
}
