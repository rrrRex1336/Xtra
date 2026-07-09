package awa.qwq.ovo.Naven.utils;

import org.mixin.accessors.ClientLevelAccessor;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

public class PacketUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final ArrayList<Packet<ServerPlayPacketListener>> queuedPackets = new ArrayList<>();

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.getNetworkHandler() != null && mc.world != null) {
         PendingUpdateManager pendingUpdateManager = ((ClientLevelAccessor)mc.world).getBlockStatePredictionHandler().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var5) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (pendingUpdateManager != null) {
            pendingUpdateManager.close();
         }
      }
   }

   public static void sendQueued(Packet<ServerPlayPacketListener> packet) {
      if (mc.player == null) {
         return;
      }
      queuedPackets.add(packet);
      mc.player.networkHandler.sendPacket(packet);
   }

   public static void send(Packet<ServerPlayPacketListener> packet) {
      if (mc.player == null) {
         return;
      }
      mc.player.networkHandler.sendPacket(packet);
   }

   public static void sendPacketNoEvent(Packet<ServerPlayPacketListener> packet) {
      if (mc.player == null) return;
      queuedPackets.add(packet);
      NetworkUtils.sendPacketNoEvent(packet);  // 用 NetworkUtils 的
   }
}
