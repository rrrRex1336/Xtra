package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;

@ModuleInfo(
        name = "ItemPhysics",
        description = "Adds physics to dropped items",
        category = Category.VISUAL
)
public class ItemPhysics extends Module {
    public final BooleanValue rotateInAir = ValueBuilder.create(this, "Rotate In Air")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue adjustScale = ValueBuilder.create(this, "Adjust Scale")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(1.005F)
            .setFloatStep(0.001F)
            .setMinFloatValue(0.9F)
            .setMaxFloatValue(1.1F)
            .build()
            .getFloatValue();

    public final FloatValue scaleFactor = ValueBuilder.create(this, "Scale Factor")
            .setDefaultFloatValue(0.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(2.0F)
            .build()
            .getFloatValue();
}