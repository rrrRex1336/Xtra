package awa.qwq.ovo.Naven.events.api.events.callables;

import awa.qwq.ovo.Naven.events.api.events.Cancellable;
import awa.qwq.ovo.Naven.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
   public boolean cancelled;

   protected EventCancellable() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean state) {
      this.cancelled = state;
   }
}
