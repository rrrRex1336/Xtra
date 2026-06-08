package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.network.chat.Component;

public class EventRenderScoreboard implements Event {
   private Component component;

   public EventRenderScoreboard(Component component) {
      this.component = component;
   }

   public Component getComponent() {
      return this.component;
   }

   public void setComponent(Component component) {
      this.component = component;
   }
}
