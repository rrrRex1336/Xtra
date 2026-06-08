package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.types.EventType;

public record EventRunTicks(EventType type) implements Event {

   public EventType getType() {
      return this.type;
   }

   public EventRunTicks(EventType type) {
      this.type = type;
   }

   public static record Post() implements Event {
      public EventType getType() {
         return EventType.POST;
      }
   }
}