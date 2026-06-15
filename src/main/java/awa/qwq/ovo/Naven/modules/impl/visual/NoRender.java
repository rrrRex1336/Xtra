package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;

@ModuleInfo(
   name = "NoRender",
   description = "Disables rendering",
   category = Category.VISUAL
)
public class NoRender extends Module {
   public BooleanValue disableEffects = ValueBuilder.create(this, "Disable Effects").setDefaultBooleanValue(true).build().getBooleanValue();
}
