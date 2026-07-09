package awa.qwq.ovo.Naven.protocol.heypixel.network;

import awa.qwq.ovo.Naven.utils.NetworkUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;

public final class HeypixelNetworkBridge {
   public static final Identifier MINECRAFT_REGISTER = new Identifier("minecraft", "register");
   public static final Identifier HEYPIXEL_EVENT = new Identifier("heypixel", "s2cevent");
   public static final Identifier HEYPIXEL_SKIN_SYNC = new Identifier("heypixel", "sync_skins");
   public static final Identifier FLOODGATE_FORM = new Identifier("floodgate", "form");
   private static final ThreadLocal<Boolean> INJECTING_REGISTER = ThreadLocal.withInitial(() -> false);

   private HeypixelNetworkBridge() {
   }

   public static boolean isInjectingRegister() {
      return INJECTING_REGISTER.get();
   }

   public static void send(Identifier channel, byte[] data, PacketSender preferredSender) {
      PacketByteBuf buf = PacketByteBufs.create();
      buf.writeBytes(data == null ? new byte[0] : data);
      boolean register = MINECRAFT_REGISTER.equals(channel);

      try {
         INJECTING_REGISTER.set(register);
         if (preferredSender != null) {
            preferredSender.sendPacket(channel, buf);
            return;
         }

         if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            Packet<?> packet = ClientPlayNetworking.createC2SPacket(channel, buf);
            NetworkUtils.sendPacketNoEvent(packet);
            return;
         }

         ClientConfigurationNetworking.getSender().sendPacket(channel, buf);
      } catch (Throwable ignored) {
      } finally {
         INJECTING_REGISTER.set(false);
      }
   }
}
