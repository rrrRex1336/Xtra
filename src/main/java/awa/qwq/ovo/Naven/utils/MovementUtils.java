package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MovementUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static final MovementUtils INSTANCE = new MovementUtils();
    public static boolean pre = false;
    public static boolean lastOnGround = false;
    public static boolean cancelMove = false;

    public static double motionX = 0.0;
    public static double motionY = 0.0;
    public static double motionZ = 0.0;
    private static float fallDistance = 0.0f;
    public static int moveTicks = 0;

    private static double posX = 0.0;
    private static double posY = 0.0;
    private static double posZ = 0.0;
    private static double lastPosX = 0.0;
    private static double lastPosY = 0.0;
    private static double lastPosZ = 0.0;

    private static int noMovePackets = 0;

    public MovementUtils() {
        Naven.getInstance().getEventManager().register(this);
    }

    public static float getSpeed() {
        LocalPlayer player = mc.player;
        if (player == null) return 0;
        return (float) Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                player.getDeltaMovement().z * player.getDeltaMovement().z);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static boolean isMove() {
        LocalPlayer player = mc.player;
        if (player == null) return false;
        return player.input.forwardImpulse != 0.0f || player.input.leftImpulse != 0.0f;
    }

    public static void strafe(float speed) {
        if (!isMove()) return;
        double yaw = getDirection();
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setDeltaMovement(-Math.sin(yaw) * speed, player.getDeltaMovement().y, Math.cos(yaw) * speed);
        }
    }

    public static void forward(double length) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        double yaw = Math.toRadians(player.getYRot());
        player.setPos(player.getX() - Math.sin(yaw) * length, player.getY(), player.getZ() + Math.cos(yaw) * length);
    }

    public static double getDirection() {
        LocalPlayer player = mc.player;
        if (player == null) return 0;
        float rotationYaw = player.getYRot();
        if (player.input.forwardImpulse < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (player.input.forwardImpulse < 0.0f) {
            forward = -0.5f;
        } else if (player.input.forwardImpulse > 0.0f) {
            forward = 0.5f;
        }
        if (player.input.leftImpulse > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (player.input.leftImpulse < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static void cancelMove() {
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (cancelMove) return;
        cancelMove = true;
        Vec3 delta = player.getDeltaMovement();
        motionX = delta.x;
        motionY = delta.y;
        motionZ = delta.z;
        fallDistance = player.fallDistance;
    }

    public static void resetMove() {
        cancelMove = false;
        moveTicks = 0;
    }

    public static boolean isMoveKeybind() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() ||
                mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0) {
            forward = -0.5f;
        } else if (moveForward > 0.0) {
            forward = 0.5f;
        }
        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0f * forward;
        }
        if (moveStrafing < 0.0) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.POST) {
            pre = false;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        LocalPlayer player = mc.player;
        if (cancelMove && player != null) {
            if (moveTicks > 0) return;
            player.setDeltaMovement(motionX, motionY, motionZ);
            player.fallDistance = fallDistance;
        }
    }


    public void onPacket(EventPacket event) {
        if (event.getType() != EventType.SEND) return;
        if (event.getPacket() instanceof ServerboundMovePlayerPacket && cancelMove) {
            if (moveTicks > 0) {
                LocalPlayer player = mc.player;
                if (player != null) {
                    lastPosX = posX;
                    lastPosY = posY;
                    lastPosZ = posZ;
                    posX = player.getX();
                    posY = player.getY();
                    posZ = player.getZ();
                    Vec3 delta = player.getDeltaMovement();
                    motionX = delta.x;
                    motionY = delta.y;
                    motionZ = delta.z;
                    fallDistance = player.fallDistance;
                    moveTicks--;
                }
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null) {
            resetMove();
            return;
        }
        pre = true;
        if (cancelMove) {
            if (noMovePackets >= 20) {
                LocalPlayer player = mc.player;
                if (player != null) {
                    player.setDeltaMovement(motionX, motionY, motionZ);
                    player.fallDistance = fallDistance;
                }
            }
            moveTicks++;
            ChatUtils.addChatMessage("" + moveTicks);
            if (moveTicks > 0) return;
            LocalPlayer player = mc.player;
            if (player != null) {
                player.setDeltaMovement(motionX, motionY, motionZ);
                player.fallDistance = fallDistance;
            }
        }
    }

    @EventTarget
    public void onMove(EventMove event) {
        if (cancelMove) {
            if (moveTicks > 0) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacketReceive(ClientboundSetEntityMotionPacket packet) {
        LocalPlayer player = mc.player;
        if (cancelMove && player != null && packet.getId() == player.getId()) {
            if (player != null) {
                player.setDeltaMovement(motionX, motionY, motionZ);
                player.fallDistance = fallDistance;
            }
            moveTicks++;
        }
    }
}