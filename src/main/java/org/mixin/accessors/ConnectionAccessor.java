package org.mixin.accessors;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Connection.class)
public interface ConnectionAccessor {
    @Invoker("sendPacket")
    void invokeSendPacket(Packet<?> packet, PacketSendListener packetSendListener, boolean flush);
}