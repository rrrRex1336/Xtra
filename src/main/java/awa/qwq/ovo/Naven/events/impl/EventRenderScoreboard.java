package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.text.Text;

public class EventRenderScoreboard implements Event {
   private Text component;

   public EventRenderScoreboard(Text component) {
      this.component = component;
   }

   public Text getComponent() {
      return this.component;
   }

   public void setComponent(Text component) {
      this.component = component;
   }
}
