package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;

public class EventHandlePacket extends EventCancellable {
   private Packet<ClientPlayPacketListener> packet;

   public Packet<ClientPlayPacketListener> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<ClientPlayPacketListener> packet) {
      this.packet = packet;
   }

   public EventHandlePacket(Packet<ClientPlayPacketListener> packet) {
      this.packet = packet;
   }
}
