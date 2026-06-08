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

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
      if (Minecraft.getInstance().getConnection() != null) {
         Minecraft.getInstance().getConnection().send(packet);
      }
   }

   public static void sendUseItemPacket(InteractionHand hand, int sequence) {
      sendPacket(new ServerboundUseItemPacket(hand, sequence));
   }

   public static void sendUseItemOnPacket(InteractionHand hand, BlockHitResult hitResult, int sequence) {
      sendPacket(new ServerboundUseItemOnPacket(hand, hitResult, sequence));
   }

   public static void sendInteractPacket(BlockPos pos, Direction direction, InteractionHand hand) {
      BlockHitResult hitResult = new BlockHitResult(
              new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
              direction, pos, false);
      sendPacket(new ServerboundUseItemOnPacket(hand, hitResult, 0));
   }

   public static void sendSwingPacket(InteractionHand hand) {
      sendPacket(new ServerboundSwingPacket(hand));
   }

   public static void sendMovePlayerPacket(double x, double y, double z, boolean onGround) {
      sendPacket(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround));
   }

   public static void sendMovePlayerPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      sendPacket(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround));
   }

   public static void sendPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction) {
      sendPacket(new ServerboundPlayerActionPacket(action, pos, direction));
   }

   public static void sendPlayerInputPacket(float xxa, float zza, boolean isJumping, boolean isSneaking) {
      sendPacket(new ServerboundPlayerInputPacket(xxa, zza, isJumping, isSneaking));
   }

   public static void sendSetCreativeModeSlotPacket(int slot, net.minecraft.world.item.ItemStack item) {
      sendPacket(new ServerboundSetCreativeModeSlotPacket(slot, item));
   }

   public static void sendHeldItemChangePacket(int slot) {
      sendPacket(new ServerboundSetCarriedItemPacket(slot));
   }

   public static void sendPlayerCommandPacket(ServerboundPlayerCommandPacket.Action action) {
      sendPacket(new ServerboundPlayerCommandPacket(Minecraft.getInstance().player, action));
   }

   public static void sendPacketNoEvent(Packet<?> packet) {
      passthroughsPackets.add(packet);
      Minecraft.getInstance().getConnection().send(packet);
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
         if (e.getPacket() instanceof ClientboundPingPacket
                 || e.getPacket() instanceof ClientboundMoveEntityPacket
                 || e.getPacket() instanceof ClientboundSetTimePacket
                 || e.getPacket() instanceof ClientboundSetPlayerTeamPacket) {
            timer.reset();
         }

         // 转发逻辑
         if (!e.isCancelled()) {
            Packet<?> packet = e.getPacket();
            if (passthroughsPackets.remove(packet)) {
               return;
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