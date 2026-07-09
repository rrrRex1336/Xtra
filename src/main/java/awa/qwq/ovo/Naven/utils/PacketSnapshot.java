package awa.qwq.ovo.Naven.utils;

import net.minecraft.network.packet.Packet;

public class PacketSnapshot {
    public Packet<?> packet;
    public long tick;

    public PacketSnapshot(Packet<?> packet, long tick) {
        this.packet = packet;
        this.tick = tick;
    }
    
}
