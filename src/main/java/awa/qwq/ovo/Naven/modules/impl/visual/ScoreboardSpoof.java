package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderScoreboard;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.StringValue;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

@ModuleInfo(
   name = "ServerNameSpoof",
   description = "Spoof the server name",
   category = Category.VISUAL
)
public class ScoreboardSpoof extends Module {

   public StringValue serverName = ValueBuilder.create(this, "Spoof name")
           .setDefaultStringValue("Naven")
           .build()
           .getStringValue();

   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         MutableText textComponent = Text.literal("§d§l" + serverName.getStringValue());
         textComponent.setStyle(e.getComponent().getStyle());
         e.setComponent(textComponent);
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         if (e.getType() == EventType.HEADER) {
            e.setComponent(Text.literal("§d§lNaven.tech"));
         } else if (e.getType() == EventType.FOOTER) {
            e.setComponent(Text.literal(""));
         }
      }
   }
}
