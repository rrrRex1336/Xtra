package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.Packet;

@Getter
public class EventPacket extends EventCancellable {
   private final EventType type;
   @Setter
   private Packet<?> packet;

    public EventPacket(EventType type, Packet<?> packet) {
      this.type = type;
      this.packet = packet;
   }
}
