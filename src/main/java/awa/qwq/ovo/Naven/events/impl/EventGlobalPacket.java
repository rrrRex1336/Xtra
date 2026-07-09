package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import net.minecraft.network.packet.Packet;

public class EventGlobalPacket extends EventCancellable {
   private final EventType type;
   private Packet<?> packet;

   public EventType getType() {
      return this.type;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventGlobalPacket(EventType type, Packet<?> packet) {
      this.type = type;
      this.packet = packet;
   }
}
