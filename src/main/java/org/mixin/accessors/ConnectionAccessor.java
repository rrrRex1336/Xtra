package org.mixin.accessors;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ConnectionAccessor {
    @Invoker("sendImmediately")
    void invokeSendPacket(Packet<?> packet, PacketCallbacks packetSendListener, boolean flush);
}
