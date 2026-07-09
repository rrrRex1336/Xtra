package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
   name = "NameProtect",
   description = "Protect your name",
   category = Category.VISUAL
)
public class NameProtect extends Module {
   public static NameProtect instance;

   public NameProtect() {
      instance = this;
   }

   public static String getName(String string) {
      if (string == null || instance == null || !instance.isEnabled() || mc.player == null) {
         return string;
      }

      String playerName = mc.player.getName().getString();
      return string.contains(playerName) ? StringUtils.replace(string, playerName, "\u00a7dPlana\u00a77") : string;
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay event) {
      if (event.getComponent() == null) {
         return;
      }

      event.setComponent(Text.literal(getName(event.getComponent().getString())));
   }
}
