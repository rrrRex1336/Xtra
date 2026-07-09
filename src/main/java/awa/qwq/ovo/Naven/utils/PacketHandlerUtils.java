package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventReceivePacket;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.thread.ThreadExecutor;
import org.slf4j.Logger;

public class PacketHandlerUtils {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends PacketListener> void processPacket(Logger logger, Packet<T> packet, T listener, ThreadExecutor<?> loop) throws OffThreadException {
        if (loop.isOnThread()) {
            return;
        }
        loop.executeSync(() -> {
            if (!listener.isConnectionOpen()) {
                logger.debug("Ignoring packet due to disconnection: {}", packet);
                return;
            }
            try {
                EventReceivePacket event = new EventReceivePacket((Packet<ClientPlayPacketListener>) (Packet) packet);
                if (loop.isOnThread()) {
                    Naven.getInstance().getEventManager().call(event);
                    if (event.isCancelled()) {
                        return;
                    }
                }
                packet.apply(listener);
            } catch (Exception exception) {
                if (listener.shouldCrashOnException()) {
                    throw exception;
                }
                logger.error("Failed to handle packet {}, suppressing error", packet, exception);
            }
        });
        throw OffThreadException.INSTANCE;
    }
}
