package awa.qwq.ovo.Naven.modules.impl.Visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.AkarinClickGUI;
import awa.qwq.ovo.Naven.ui.ClickGUI;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

@ModuleInfo(
   name = "ClickGUI",
   category = Category.VISUAL,
   description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
   ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Naven", "Akarin")
           .build().getModeValue();

   ClickGUI navenGUI = null;
   AkarinClickGUI akarinGUI = null;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      if (mode.isCurrentMode("Akarin")) {
         if (akarinGUI == null) {
            akarinGUI = new AkarinClickGUI();
         }
         mc.setScreen(akarinGUI);
      } else {
         if (navenGUI == null) {
            navenGUI = new ClickGUI();
         }
         mc.setScreen(navenGUI);
      }
      this.toggle();
   }
}
