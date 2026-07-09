package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventGlobalPacket;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkUtils {
   public static Set<Packet<?>> passthroughsPackets = new HashSet<>();
   private static final TimeHelper timer = new TimeHelper();
   private static final Notification lagging = new Notification(NotificationLevel.WARNING, "Server lagging!", 2000L);
   private static long totalTime = 0L;
   public static final Logger LOGGER = LogManager.getLogger("PacketUtil");

   public static boolean isServerLag() {
      return timer.delay(500.0);
   }

   public static void sendPacket(Packet<?> packet) {
      if (MinecraftClient.getInstance().getNetworkHandler() != null) {
         MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
      }
   }

   public static void sendUseItemPacket(Hand hand, int sequence) {
      sendPacket(new PlayerInteractItemC2SPacket(hand, sequence));
   }

   public static void sendUseItemOnPacket(Hand hand, BlockHitResult hitResult, int sequence) {
      sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, sequence));
   }

   public static void sendInteractPacket(BlockPos pos, Direction direction, Hand hand) {
      BlockHitResult hitResult = new BlockHitResult(
              new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
              direction, pos, false);
      sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
   }

   public static void sendSwingPacket(Hand hand) {
      sendPacket(new HandSwingC2SPacket(hand));
   }

   public static void sendMovePlayerPacket(double x, double y, double z, boolean onGround) {
      sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
   }

   public static void sendMovePlayerPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround));
   }

   public static void sendPlayerActionPacket(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction) {
      sendPacket(new PlayerActionC2SPacket(action, pos, direction));
   }

   public static void sendPlayerInputPacket(float xxa, float zza, boolean isJumping, boolean isSneaking) {
      sendPacket(new PlayerInputC2SPacket(xxa, zza, isJumping, isSneaking));
   }

   public static void sendSetCreativeModeSlotPacket(int slot, net.minecraft.item.ItemStack item) {
      sendPacket(new CreativeInventoryActionC2SPacket(slot, item));
   }

   public static void sendHeldItemChangePacket(int slot) {
      sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   public static void sendPlayerCommandPacket(ClientCommandC2SPacket.Mode action) {
      sendPacket(new ClientCommandC2SPacket(MinecraftClient.getInstance().player, action));
   }

   public static void sendPacketNoEvent(Packet<?> packet) {
      passthroughsPackets.add(packet);
      MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (isServerLag()) {
            Naven.getInstance().getNotificationManager().addNotification(lagging);
            lagging.setCreateTime(System.currentTimeMillis());
            lagging.setLevel(NotificationLevel.WARNING);
            totalTime = Math.round(timer.getLastDelay());
            lagging.setMessage("Server lagging. Aura disabled! (" + totalTime + "ms)");
         } else {
            lagging.setLevel(NotificationLevel.SUCCESS);
            lagging.setMessage("Server currently online! (" + totalTime + "ms)");
         }
      }
   }
   private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);

   @EventTarget(4)
   public void onGlobalPacket(EventGlobalPacket e) {
      if (isProcessing.get()) {
         return;
      }

      try {
         isProcessing.set(true);

         // 原有的 ping 包计时逻辑
         if (e.getPacket() instanceof CommonPingS2CPacket
                 || e.getPacket() instanceof EntityS2CPacket
                 || e.getPacket() instanceof WorldTimeUpdateS2CPacket
                 || e.getPacket() instanceof TeamS2CPacket) {
            timer.reset();
         }

         // 转发逻辑
         if (!e.isCancelled()) {
            Packet<?> packet = e.getPacket();
            if (passthroughsPackets.remove(packet)) {
               return;
            }
            if (e.getType() == EventType.SEND) {
               GetC03StatusUtil.packetEvent(packet);
            }
            EventPacket event = new EventPacket(e.getType(), packet);
            Naven.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
               e.setCancelled(true);
            }
            e.setPacket(event.getPacket());
         }
      } finally {
         isProcessing.set(false);
      }
   }
}
