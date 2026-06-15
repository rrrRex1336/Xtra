package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;

@ModuleInfo(
        name = "LowFire",
        description = "Show the fire lower.",
        category = Category.VISUAL
)
public class LowFire extends Module {
    public static LowFire instance;

    public LowFire() {
        instance = this;
    }
}
