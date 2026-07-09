package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import lombok.Generated;
import lombok.Getter;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;

public class EventReceivePacket extends EventCancellable {
    @Getter
    private final Packet<ClientPlayPacketListener> packet;

    @Generated
    public EventReceivePacket(Packet<ClientPlayPacketListener> packet) {
        this.packet = packet;
    }
}
