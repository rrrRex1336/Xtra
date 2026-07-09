package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventServerSetPosition;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientPlayNetworkHandler.class})
public class MixinClientPacketListener {
   @Redirect(
      method = {"onPlayerPositionLook"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V",
         ordinal = 1
      )
   )
   public void onSendPacket(ClientConnection instance, Packet<?> pPacket) {
      EventServerSetPosition event = new EventServerSetPosition(pPacket);
      Naven.getInstance().getEventManager().call(event);
      instance.send(event.getPacket());
   }
}
