package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetworkThreadUtils.class)
public class MixinPacketUtils {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static <T extends PacketListener> void forceMainThread(
            Packet<T> packet, T listener, ThreadExecutor<?> loop) {

        if (packet instanceof EntityVelocityUpdateS2CPacket) {
            EventPacket event = new EventPacket(EventType.RECEIVE, packet);
            Naven.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (!loop.isOnThread()) {
            loop.execute(() -> packet.apply(listener));
        } else {
            packet.apply(listener);
        }
    }
}
