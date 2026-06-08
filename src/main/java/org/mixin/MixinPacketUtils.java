package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PacketUtils.class)
public class MixinPacketUtils {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static <T extends PacketListener> void ensureRunningOnSameThread(
            Packet<T> packet, T listener, BlockableEventLoop<?> loop) {

        if (packet instanceof ClientboundSetEntityMotionPacket) {
            EventPacket event = new EventPacket(EventType.RECEIVE, packet);
            Naven.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (!loop.isSameThread()) {
            loop.execute(() -> packet.handle(listener));
        } else {
            packet.handle(listener);
        }
    }
}