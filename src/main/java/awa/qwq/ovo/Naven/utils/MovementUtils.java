package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMove;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MovementUtils implements MinecraftInstance {
    public static final MovementUtils INSTANCE = new MovementUtils();
    public static Boolean pre = false;
    public static boolean lastOnGround = false;
    public static boolean cancelMove = false;

    private static final Minecraft mc = Minecraft.getInstance();
    private static double motionX = 0.0;
    private static double motionY = 0.0;
    private static double motionZ = 0.0;
    private static float fallDistance = 0.0F;
    private static int moveTicks = 0;
    private static boolean changeState = false;
    private static double posX = 0.0;
    private static double posY = 0.0;
    private static double posZ = 0.0;
    private static double lastPosX = 0.0;
    private static double lastPosY = 0.0;
    private static double lastPosZ = 0.0;

    private MovementUtils() {
    }

    public static float getSpeed() {
        if (mc.player == null) return 0.0F;
        Vec3 motion = mc.player.getDeltaMovement();
        return (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static boolean isMove() {
        return mc.player != null
                && (mc.player.input.forwardImpulse != 0.0F || mc.player.input.leftImpulse != 0.0F);
    }

    public static boolean hasMotion() {
        if (mc.player == null) return false;
        Vec3 motion = mc.player.getDeltaMovement();
        return motion.x != 0.0 && motion.z != 0.0 && motion.y != 0.0;
    }

    public static void strafe(float speed) {
        if (!isMove()) {
            return;
        }

        double yaw = getDirection();
        Vec3 motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(-Math.sin(yaw) * speed, motion.y, Math.cos(yaw) * speed);
    }

    public static void forward(double length) {
        if (mc.player == null) return;

        double yaw = Math.toRadians(mc.player.getYRot());
        mc.player.setPos(
                mc.player.getX() + -Math.sin(yaw) * length,
                mc.player.getY(),
                mc.player.getZ() + Math.cos(yaw) * length
        );
    }

    public static double getDirection() {
        if (mc.player == null) return 0.0;

        float rotationYaw = mc.player.getYRot();
        float moveForward = mc.player.input.forwardImpulse;
        float moveStrafing = mc.player.input.leftImpulse;

        if (moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0F) {
            forward = -0.5F;
        } else if (moveForward > 0.0F) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }

        if (moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static float[] getRotationsBlock(BlockPos block, Direction face) {
        if (mc.player == null) return new float[]{0.0F, 0.0F};

        double x = block.getX() + 0.5 - mc.player.getX() + face.getStepX() / 2.0;
        double z = block.getZ() + 0.5 - mc.player.getZ() + face.getStepZ() / 2.0;
        double y = block.getY() + 0.5;
        double diffY = mc.player.getEyeY() - y;
        double diffXZ = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (Math.atan2(diffY, diffXZ) * 180.0 / Math.PI);

        if (yaw < 0.0F) {
            yaw += 360.0F;
        }

        return new float[]{yaw, pitch};
    }

    public static void cancelMove() {
        if (mc.player == null || cancelMove) {
            return;
        }

        cancelMove = true;
        savePlayerState();
    }

    public static void resetMove() {
        cancelMove = false;
        moveTicks = 0;
    }

    public static boolean isMoveKeybind() {
        return mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
    }

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0) {
            forward = -0.5F;
        } else if (moveForward > 0.0) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0F * forward;
        }

        if (moveStrafing < 0.0) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(EventMoveInput event, float targetYaw, float motionYaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = Mth.wrapDegrees(Math.toDegrees(direction(motionYaw, forward, strafe)));

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

                double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(targetYaw, predictedForward, predictedStrafe)));
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
        if (event.getType() != EventType.PRE) {
            pre = false;
        }
    }

    public void onUpdate() {
        if (cancelMove) {
            if (moveTicks > 0) {
                return;
            }
            restorePlayerState();
        }
    }

    @EventTarget(0)
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE) return;

        if (mc.player == null || mc.level == null) {
            resetMove();
            return;
        }

        pre = true;
        if (!cancelMove) {
            return;
        }

        changeState = false;
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
        if (cancelMove && moveTicks <= 0) {
            event.setCancelled(true);
        }
    }

    @EventTarget(0)
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundMovePlayerPacket) {
            handleMovePacket();
            return;
        }

        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            handleVelocityPacket(packet);
        }
    }

    private static void handleMovePacket() {
        if (!cancelMove || !(mc.player != null)) {
            return;
        }

        if (moveTicks <= 0) {
            return;
        }

        lastPosX = posX;
        lastPosY = posY;
        lastPosZ = posZ;
        posX = mc.player.getX();
        posY = mc.player.getY();
        posZ = mc.player.getZ();
        savePlayerState();
        --moveTicks;
    }

    private static void handleVelocityPacket(ClientboundSetEntityMotionPacket packet) {
        Player player = mc.player;
        if (player == null || packet.getId() != player.getId() || !cancelMove) {
            return;
        }

        restorePlayerState();
        ++moveTicks;
    }

    private static void savePlayerState() {
        if (mc.player == null) return;

        Vec3 motion = mc.player.getDeltaMovement();
        motionX = motion.x;
        motionY = motion.y;
        motionZ = motion.z;
        fallDistance = mc.player.fallDistance;
    }

    private static void restorePlayerState() {
        if (mc.player == null) return;

        mc.player.setDeltaMovement(motionX, motionY, motionZ);
        mc.player.fallDistance = fallDistance;
    }
}
