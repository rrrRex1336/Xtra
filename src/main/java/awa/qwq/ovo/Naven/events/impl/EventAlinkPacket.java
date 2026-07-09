package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import net.minecraft.network.packet.Packet;

public class EventAlinkPacket extends EventCancellable {
    private Packet<?> packet;

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public EventAlinkPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
