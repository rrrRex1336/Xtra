package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import net.minecraft.text.Text;

public class EventSetTitle extends EventCancellable {
   private EventType type;
   private Text title;

   public EventSetTitle(EventType type, Text title) {
      this.type = type;
      this.title = title;
   }

   public EventType getType() {
      return this.type;
   }

   public Text getTitle() {
      return this.title;
   }

   public void setType(EventType type) {
      this.type = type;
   }

   public void setTitle(Text title) {
      this.title = title;
   }
}
