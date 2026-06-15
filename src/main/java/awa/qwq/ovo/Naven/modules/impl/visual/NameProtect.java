package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
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
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), "§dPlana§7") : string;
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(getName(e.getComponent().getString())));
   }
}
