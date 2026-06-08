package awa.qwq.ovo.Naven.utils;

import org.mixin.accessors.ClientLevelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

import java.util.ArrayList;

public class PacketUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   public static final ArrayList<Packet<ServerGamePacketListener>> queuedPackets = new ArrayList<>();

   public static void sendSequencedPacket(PredictiveAction packetCreator) {
      if (mc.getConnection() != null && mc.level != null) {
         BlockStatePredictionHandler pendingUpdateManager = ((ClientLevelAccessor)mc.level).getBlockStatePredictionHandler().startPredicting();

         try {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
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

   public static void sendQueued(Packet<ServerGamePacketListener> packet) {
      if (mc.player == null) {
         return;
      }
      queuedPackets.add(packet);
      mc.player.connection.send(packet);
   }

   public static void send(Packet<ServerGamePacketListener> packet) {
      if (mc.player == null) {
         return;
      }
      mc.player.connection.send(packet);
   }

   public static void sendPacketNoEvent(Packet<ServerGamePacketListener> packet) {
      if (mc.player == null) return;
      queuedPackets.add(packet);
      NetworkUtils.sendPacketNoEvent(packet);  // 用 NetworkUtils 的
   }
}
