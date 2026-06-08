package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventGlobalPacket;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import org.mixin.accessors.ConnectionAccessor;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public abstract class MixinConnection {

   @Shadow
   private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
   }

   @Inject(method = "send", at = @At("HEAD"), cancellable = true)
   private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
      if (packet instanceof ServerboundMovePlayerPacket) {
         if (SkipTicks.isSendingStuckPacket.get()) {
            return;
         }
         if (SkipTicks.isActive() && SkipTicks.positionUpdate) {
            if (packet instanceof ServerboundMovePlayerPacket.Rot) {
               return;
            }
            ci.cancel();
         }
      }
   }

   @Redirect(
           method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
           )
   )
   private void onGenericsFtw(Packet<?> pPacket, PacketListener pListener) {
      EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
      Naven.getInstance().getEventManager().call(event);

      if (!event.isCancelled()) {
         genericsFtw(event.getPacket(), pListener);
      }
   }

   @Redirect(
           method = {"send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V"},  // 注意方法签名
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V"
           )
   )
   private void onSend(Connection instance, Packet<?> pInPacket, PacketSendListener pFutureListeners, boolean flush) {
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