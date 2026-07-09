package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventHandlePacket;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMove;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mixin.accessors.LocalPlayerAccessor;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;

public final class MovementUtils {
   public static final MovementUtils INSTANCE = new MovementUtils();
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean pre = false;
   public static boolean lastOnGround = false;
   public static boolean cancelMove = false;

   private static double motionX = 0.0;
   private static double motionY = 0.0;
   private static double motionZ = 0.0;
   private static float fallDistance = 0.0F;
   private static int moveTicks = 0;
   private static double posX = 0.0;
   private static double posY = 0.0;
   private static double posZ = 0.0;
   private static double lastPosX = 0.0;
   private static double lastPosY = 0.0;
   private static double lastPosZ = 0.0;

   private MovementUtils() {
   }

   public static float getSpeed() {
      if (mc.player == null) {
         return 0.0F;
      }

      Vec3d velocity = mc.player.getVelocity();
      return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
   }

   public static void strafe() {
      strafe(getSpeed());
   }

   public static boolean isMove() {
      return mc.player != null
              && (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F);
   }

   public static boolean hasMotion() {
      if (mc.player == null) {
         return false;
      }

      Vec3d velocity = mc.player.getVelocity();
      return velocity.x != 0.0D || velocity.y != 0.0D || velocity.z != 0.0D;
   }

   public static void strafe(float speed) {
      if (mc.player == null || !isMove()) {
         return;
      }

      double yaw = getDirection();
      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(-Math.sin(yaw) * speed, velocity.y, Math.cos(yaw) * speed);
   }

   public static void forward(double length) {
      if (mc.player == null) {
         return;
      }

      double yaw = Math.toRadians(mc.player.getYaw());
      mc.player.setPosition(
              mc.player.getX() + -Math.sin(yaw) * length,
              mc.player.getY(),
              mc.player.getZ() + Math.cos(yaw) * length
      );
   }

   public static double getDirection() {
      if (mc.player == null) {
         return 0.0D;
      }

      return direction(mc.player.getYaw(), mc.player.input.movementForward, mc.player.input.movementSideways);
   }

   public static void cancelMove() {
      if (mc.player == null || cancelMove) {
         return;
      }

      cancelMove = true;
      moveTicks = 0;
      savePlayerState();
      setPositionReminder(0);
   }

   public static void resetMove() {
      cancelMove = false;
      moveTicks = 0;
      setPositionReminder(GetC03StatusUtil.noMovePackets);
   }

   public static boolean isMoveCancelled() {
      return cancelMove;
   }

   public static boolean isMoveKeybind() {
      return mc.options.forwardKey.isPressed()
              || mc.options.backKey.isPressed()
              || mc.options.leftKey.isPressed()
              || mc.options.rightKey.isPressed();
   }

   public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0D) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0D) {
         forward = -0.5F;
      } else if (moveForward > 0.0D) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0D) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0D) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   public static void fixMovement(EventMoveInput event, float targetYaw, float motionYaw) {
      float forward = event.getForward();
      float strafe = event.getStrafe();
      double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(motionYaw, forward, strafe)));
      if (forward == 0.0F && strafe == 0.0F) {
         return;
      }

      float closestForward = 0.0F;
      float closestStrafe = 0.0F;
      float closestDifference = Float.MAX_VALUE;
      for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
         for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
            if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
               continue;
            }

            double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(targetYaw, predictedForward, predictedStrafe)));
            double difference = Math.abs(angle - predictedAngle);
            if (difference < closestDifference) {
               closestDifference = (float) difference;
               closestForward = predictedForward;
               closestStrafe = predictedStrafe;
            }
         }
      }

      event.setForward(closestForward);
      event.setStrafe(closestStrafe);
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      if (event.getType() == EventType.POST) {
         pre = false;
      } else if (event.getType() == EventType.PRE) {
         pre = true;
      }
      lastOnGround = event.isOnGround();
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (!cancelMove || mc.player == null) {
         return;
      }

      setPositionReminder(0);
      if (moveTicks > 0) {
         return;
      }

      restorePlayerState();
   }

   @EventTarget
   public void onRunTicks(EventRunTicks event) {
      if (event.getType() != EventType.PRE) {
         return;
      }

      if (mc.player == null) {
         resetMove();
         return;
      }

      pre = true;
      if (!cancelMove) return;

      if (GetC03StatusUtil.noMovePackets >= 20) {
         restorePlayerState();
      }

      if (++moveTicks > 0) {
         return;
      }

      restorePlayerState();
   }

   @EventTarget
   public void onMove(EventMove event) {
      if (!cancelMove) {
         return;
      }

      if (moveTicks > 0) {
         return;
      }

      event.setCancelled(true);
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getType() != EventType.SEND || !cancelMove || mc.player == null) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (!(packet instanceof PlayerMoveC2SPacket movePacket)) {
         return;
      }

      lastOnGround = ((ServerboundMovePlayerPacketAccessor) movePacket).isOnGround();
      if (moveTicks > 0) {
         trackPositionAndState();
         moveTicks--;
      }
   }

   @EventTarget
   public void onPacketReceiveSync(EventHandlePacket event) {
      if (!cancelMove || mc.player == null) {
         return;
      }

      if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocityPacket
              && velocityPacket.getId() == mc.player.getId()) {
         restorePlayerState();
         moveTicks++;
      }
   }

   private static void savePlayerState() {
      Vec3d velocity = mc.player.getVelocity();
      motionX = velocity.x;
      motionY = velocity.y;
      motionZ = velocity.z;
      fallDistance = mc.player.fallDistance;
      posX = mc.player.getX();
      posY = mc.player.getY();
      posZ = mc.player.getZ();
      lastPosX = posX;
      lastPosY = posY;
      lastPosZ = posZ;
   }

   private static void restorePlayerState() {
      mc.player.setVelocity(motionX, motionY, motionZ);
      mc.player.fallDistance = fallDistance;
   }

   private static void trackPositionAndState() {
      lastPosX = posX;
      lastPosY = posY;
      lastPosZ = posZ;
      posX = mc.player.getX();
      posY = mc.player.getY();
      posZ = mc.player.getZ();
      Vec3d velocity = mc.player.getVelocity();
      motionX = velocity.x;
      motionY = velocity.y;
      motionZ = velocity.z;
      fallDistance = mc.player.fallDistance;
   }

   private static void setPositionReminder(int ticks) {
      if (mc.player != null) {
         ((LocalPlayerAccessor) mc.player).setPositionReminder(ticks);
      }
   }
}
