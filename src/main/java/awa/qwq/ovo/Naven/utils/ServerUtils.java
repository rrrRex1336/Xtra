package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventGlobalPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;

public class ServerUtils {
   private static int grimTransactionCount = 0;
   public static final Map<String, AtomicInteger> HEALTHS = new HashMap<>();

   @EventTarget(0)
   public void onAllPackets(EventGlobalPacket e) {
      if (e.getType() == EventType.RECEIVE) {
         if (e.getPacket() instanceof CommonPingS2CPacket) {
            grimTransactionCount++;
         }

         if (e.getPacket() instanceof ScoreboardScoreUpdateS2CPacket packet
                 && MinecraftClient.getInstance().world != null
                 && ("belowHealth".equals(packet.objectiveName()) || "health".equals(packet.objectiveName()))
                 && !packet.scoreHolderName().equals(MinecraftClient.getInstance().player.getGameProfile().getName())) {

            if (!HEALTHS.containsKey(packet.scoreHolderName())) {
               AtomicInteger atomic = new AtomicInteger();
               HEALTHS.put(packet.scoreHolderName(), atomic);
            }

            HEALTHS.get(packet.scoreHolderName()).set(packet.score());
         }

         if (e.getPacket() instanceof HealthUpdateS2CPacket packet && packet.getHealth() > 20.0F) {
            e.setCancelled(true);
         }
      }
   }

   @EventTarget
   public void onUpdate(EventRender2D event) {
      for (AbstractClientPlayerEntity player : MinecraftClient.getInstance().world.getPlayers()) {
         if (player != MinecraftClient.getInstance().player && HEALTHS.containsKey(player.getName().getString())) {
            player.setHealth((float)Math.max(1, HEALTHS.get(player.getName().getString()).get()));
         }
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      grimTransactionCount = 0;
   }

   public static int getGrimTransactionCount() {
      return grimTransactionCount;
   }
}
