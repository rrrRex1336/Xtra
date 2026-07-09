package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.ClickGUI;

@ModuleInfo(
   name = "ClickGUI",
   category = Category.VISUAL,
   description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
   ClickGUI navenGUI = null;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      if (navenGUI == null) {
         navenGUI = new ClickGUI();
      }
      mc.setScreen(navenGUI);
      this.toggle();
   }
}
