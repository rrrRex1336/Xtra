package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;

@ModuleInfo(name = "Rotation", description = "In Client display Server rotation.", category = Category.VISUAL)
public class Rotation extends Module {

    public BooleanValue headPitch = ValueBuilder.create(this, "Head Pitch").setDefaultBooleanValue(true).build().getBooleanValue();

    public BooleanValue headYaw = ValueBuilder.create(this, "Head Yaw").setDefaultBooleanValue(true).build().getBooleanValue();

    public BooleanValue syncHeadBodyYaw = ValueBuilder.create(this, "Sync Head Body Yaw").setDefaultBooleanValue(false).build().getBooleanValue();

}
