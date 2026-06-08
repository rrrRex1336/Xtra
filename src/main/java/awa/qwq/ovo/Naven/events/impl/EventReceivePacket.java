package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import lombok.Generated;
import lombok.Getter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

public class EventReceivePacket extends EventCancellable {
    @Getter
    private final Packet<ClientGamePacketListener> packet;

    @Generated
    public EventReceivePacket(Packet<ClientGamePacketListener> packet) {
        this.packet = packet;
    }
}
