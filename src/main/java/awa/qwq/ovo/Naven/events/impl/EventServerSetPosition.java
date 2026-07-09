package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.network.packet.Packet;

public class EventServerSetPosition implements Event {
   private Packet<?> packet;

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventServerSetPosition(Packet<?> packet) {
      this.packet = packet;
   }
}
