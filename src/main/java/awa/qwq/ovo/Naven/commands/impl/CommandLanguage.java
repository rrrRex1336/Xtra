package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;

@CommandInfo(
   name = "language",
   description = "Open language gui.",
   aliases = {"lang"}
)
public class CommandLanguage extends Command {
   @Override
   public void onCommand(String[] args) {
      Naven.getInstance().getEventManager().register(new Object() {
         @EventTarget
         public void onMotion(EventMotion e) {
            if (e.getType() == EventType.PRE) {
               MinecraftClient.getInstance().setScreen(new LanguageOptionsScreen(null, MinecraftClient.getInstance().options, MinecraftClient.getInstance().getLanguageManager()));
               Naven.getInstance().getEventManager().unregister(this);
            }
         }
      });
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
