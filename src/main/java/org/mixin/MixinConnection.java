package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventGlobalPacket;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.mixin.accessors.ConnectionAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public abstract class MixinConnection {

   @Shadow
   private static <T extends PacketListener> void handlePacket(Packet<T> pPacket, PacketListener pListener) {
   }

   @Inject(method = "send", at = @At("HEAD"), cancellable = true)
   private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
      if (packet instanceof PlayerMoveC2SPacket) {
         if (SkipTicks.isSendingStuckPacket.get()) {
            return;
         }
         if (SkipTicks.isActive() && SkipTicks.positionUpdate) {
            if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
               return;
            }
            ci.cancel();
         }
      }
   }

   @Redirect(
           method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V"
           )
   )
   private void onHandlePacket(Packet<?> pPacket, PacketListener pListener) {
      EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
      Naven.getInstance().getEventManager().call(event);

      if (!event.isCancelled()) {
         handlePacket(event.getPacket(), pListener);
      }
   }

   @Redirect(
           method = {"send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V"},  // 注意方法签名
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/ClientConnection;sendImmediately(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V"
           )
   )
   private void onSend(ClientConnection instance, Packet<?> pInPacket, PacketCallbacks pFutureListeners, boolean flush) {
      if (NetworkUtils.passthroughsPackets.contains(pInPacket)) {
         NetworkUtils.passthroughsPackets.remove(pInPacket);
         ((ConnectionAccessor) instance).invokeSendPacket(pInPacket, pFutureListeners, flush);
      } else {
         EventGlobalPacket event = new EventGlobalPacket(EventType.SEND, pInPacket);
         Naven.getInstance().getEventManager().call(event);
         if (!event.isCancelled()) {
            ((ConnectionAccessor) instance).invokeSendPacket(event.getPacket(), pFutureListeners, flush);
         }
      }
   }
}
