package awa.qwq.ovo.Naven.protocol.heypixel;

import awa.qwq.ovo.Naven.modules.impl.misc.Protocol;
import awa.qwq.ovo.Naven.protocol.heypixel.network.HeypixelNetworkBridge;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class HeypixelProtocol {
   private static final Set<Identifier> PLAY_REGISTERED = new HashSet<>();
   private static final Set<Identifier> CONFIG_REGISTERED = new HashSet<>();
   private static final Identifier[] CHANNELS = {
           HeypixelNetworkBridge.HEYPIXEL_EVENT,
           HeypixelNetworkBridge.FLOODGATE_FORM,
           HeypixelNetworkBridge.MINECRAFT_REGISTER
   };

   private HeypixelProtocol() {
   }

   public static synchronized void enable() {
      for (Identifier channel : CHANNELS) {
         if (!PLAY_REGISTERED.contains(channel)
                 && ClientPlayNetworking.registerGlobalReceiver(channel,
                 (client, handler, buf, responseSender) -> handlePayload(channel, buf, responseSender))) {
            PLAY_REGISTERED.add(channel);
         }
         if (!CONFIG_REGISTERED.contains(channel)
                 && ClientConfigurationNetworking.registerGlobalReceiver(channel,
                 (client, handler, buf, responseSender) -> handlePayload(channel, buf, responseSender))) {
            CONFIG_REGISTERED.add(channel);
         }
      }
      HeypixelSession.current();
   }

   public static synchronized void disable() {
      for (Identifier channel : PLAY_REGISTERED) {
         ClientPlayNetworking.unregisterGlobalReceiver(channel);
      }
      PLAY_REGISTERED.clear();

      for (Identifier channel : CONFIG_REGISTERED) {
         ClientConfigurationNetworking.unregisterGlobalReceiver(channel);
      }
      CONFIG_REGISTERED.clear();
      HeypixelSession.disposeCurrent();
   }

   public static void handlePayload(Identifier explicitChannel, PacketByteBuf buf, PacketSender responseSender) {
      if (!Protocol.isProtocolEnabled()) {
         return;
      }

      Identifier channel = explicitChannel;
      byte[] data = new byte[buf.readableBytes()];
      buf.readBytes(data);
      HeypixelSession session = HeypixelSession.current();
      if (HeypixelNetworkBridge.MINECRAFT_REGISTER.equals(channel)) {
         session.handleChannelRegister(data, responseSender);
      } else if (HeypixelNetworkBridge.HEYPIXEL_EVENT.equals(channel)) {
         session.handleHeypixelMessage(data, responseSender);
      } else if (HeypixelNetworkBridge.FLOODGATE_FORM.equals(channel)) {
         session.handleFloodgateForm(data, responseSender);
      }
   }
}
