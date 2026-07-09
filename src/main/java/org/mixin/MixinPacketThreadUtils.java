package org.mixin;

import awa.qwq.ovo.Naven.utils.MixinProtectionUtils;
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
   private static <T extends PacketListener> void onEnsureRunningOnSameThread(Packet<T> packet, T listener, ThreadExecutor<?> executor, CallbackInfo ci) throws OffThreadException {
      ci.cancel();
      MixinProtectionUtils.onEnsureRunningOnSameThread(LOGGER, packet, listener, executor);
   }
}
