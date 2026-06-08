package awa.qwq.ovo.Naven.modules.impl.Visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.ClickGUI;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

@ModuleInfo(
   name = "ClickGUI",
   category = Category.VISUAL,
   description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
   ModeValue style = ValueBuilder.create(this, "Style")
           .setDefaultModeIndex(0)
           .setModes("Legacy")
           .build().getModeValue();

   ClickGUI legacyGUI = null;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      if (style.isCurrentMode("Legacy")) {
         if (legacyGUI == null) {
            legacyGUI = new ClickGUI();
         }
         mc.setScreen(legacyGUI);
      }
      this.toggle();
   }
}