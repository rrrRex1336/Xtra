package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventHandlePacket;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.thread.ThreadExecutor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NetworkThreadUtils.class})
public class MixinPacketThreadUtils {
   @Shadow
   @Final
   private static Logger LOGGER;

   @Inject(
      method = {"forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   @SuppressWarnings({"rawtypes", "unchecked"})
   private static <T extends PacketListener> void onEnsureRunningOnSameThread(Packet<T> packet, T listener, ThreadExecutor<?> executor, CallbackInfo ci) throws OffThreadException {
      if (!executor.isOnThread()) {
         ci.cancel();
         executor.executeSync(() -> {
            if (listener.isConnectionOpen()) {
               try {
                  EventHandlePacket event = new EventHandlePacket((Packet) packet);
                  if (executor.isOnThread()) {
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

                  LOGGER.error("Failed to handle packet {}, suppressing error", packet, exception);
               }
            } else {
               LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
            }
         });
         throw OffThreadException.INSTANCE;
      }
   }
}
