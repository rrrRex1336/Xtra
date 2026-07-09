package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventHandlePacket;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.thread.ThreadExecutor;
import org.slf4j.Logger;

public class MixinProtectionUtils {
   public static <T extends PacketListener> void onEnsureRunningOnSameThread(Logger LOGGER, Packet<T> packet, T listener, ThreadExecutor<?> executor) throws OffThreadException {
      if (!executor.isOnThread()) {
         executor.executeSync(() -> {
            if (listener.isConnectionOpen()) {
               try {
                  EventHandlePacket event = new EventHandlePacket((Packet)packet);
                  if (executor.isOnThread()) {
                     Naven.getInstance().getEventManager().call(event);
                     if (event.isCancelled()) {
                        return;
                     }
                  }

                  packet.apply(listener);
               } catch (Exception var5) {
                  if (listener.shouldCrashOnException()) {
                     throw var5;
                  }

                  LOGGER.error("Failed to handle packet {}, suppressing error", packet, var5);
               }
            } else {
               LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
            }
         });
         throw OffThreadException.INSTANCE;
      }
   }

   public static byte[] readByteArray(PacketByteBuf buf, int maxSize) {
      int i = buf.readVarInt() - 1;
      if (i > maxSize) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         byte[] abyte = new byte[i];
         buf.readBytes(abyte);
         return abyte;
      }
   }
}
