package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.GetC03StatusUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class EventStuck {
    public static final EventStuck INSTANCE = new EventStuck();
    public static Boolean pre = false;
    private static int noMovePackets = 0;
    private static int lastSentPacketTick = 0;
    private static final Map<Object, StuckModuleState> activeModules = new HashMap<>();
    private static final Minecraft mc = Minecraft.getInstance();
    public static int noMoveTick = 0;
    public static double motionX = 0.0;
    public static double motionY = 0.0;
    public static double motionZ = 0.0;
    public static float fallDistance = 0.0F;
    public static int moveTicks = 0;
    public static int cancelTickCounter = 0;
    private static final int CANCEL_TICK_CYCLE = 20;

    public static void onMovementPacketSent() {
        lastSentPacketTick = mc.player != null ? mc.player.tickCount : 0;
        noMovePackets = 0;
    }

    private static class StuckModuleState {
        long activateTime;
        boolean hasInitializedState;
        int localTickCounter;

        StuckModuleState() {
            this.activateTime = System.currentTimeMillis();
            this.hasInitializedState = false;
            this.localTickCounter = 0;
        }
    }

    public EventStuck() {
    }

    public static float getSpeed() {
        Player player = mc.player;
        if (player == null) return 0;
        return (float) Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x +
                player.getDeltaMovement().z * player.getDeltaMovement().z);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static boolean isMove() {
        Player player = mc.player;
        if (player == null) return false;
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() ||
                mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    public static void strafe(float speed) {
        Player player = mc.player;
        if (player == null || !isMove()) return;

        double yaw = getDirection();
        player.setDeltaMovement(-Math.sin(yaw) * speed,
                player.getDeltaMovement().y,
                Math.cos(yaw) * speed);
    }

    public static void forward(double length) {
        Player player = mc.player;
        if (player == null) return;

        double yaw = Math.toRadians(player.getYRot());
        player.setPos(player.getX() + -Math.sin(yaw) * length,
                player.getY(),
                player.getZ() + Math.cos(yaw) * length);
    }

    public static double getDirection() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;

        float rotationYaw = player.getYRot();

        KeyMapping keyUp = Minecraft.getInstance().options.keyUp;
        KeyMapping keyDown = Minecraft.getInstance().options.keyDown;
        KeyMapping keyLeft = Minecraft.getInstance().options.keyLeft;
        KeyMapping keyRight = Minecraft.getInstance().options.keyRight;

        boolean up = keyUp.isDown();
        boolean down = keyDown.isDown();
        boolean left = keyLeft.isDown();
        boolean right = keyRight.isDown();

        if (down) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (down) {
            forward = -0.5F;
        } else if (up) {
            forward = 0.5F;
        }

        if (right) {
            rotationYaw -= 90.0F * forward;
        }

        if (left) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    private static final Object DEFAULT_MODULE = new Object();

    public static void cancelMove() {
        cancelMove(DEFAULT_MODULE);
    }

    public static void cancelMove(Object module) {
        Player player = mc.player;
        if (player == null) return;

        boolean isFirstModule = activeModules.isEmpty();

        if (!activeModules.containsKey(module)) {
            activeModules.put(module, new StuckModuleState());
        }

        if (isFirstModule) {
            motionX = player.getDeltaMovement().x;
            motionY = player.getDeltaMovement().y;
            motionZ = player.getDeltaMovement().z;
            fallDistance = player.fallDistance;
            moveTicks = 0;
            cancelTickCounter = 0;

            StuckModuleState state = activeModules.get(module);
            if (state != null) {
                state.hasInitializedState = true;
                state.localTickCounter = 0;
            }
        }
    }

    public static void resetMove() {
        resetMove(DEFAULT_MODULE);
    }

    public static void resetMove(Object module) {
        StuckModuleState removedState = activeModules.remove(module);

        if (removedState != null && removedState.hasInitializedState && !activeModules.isEmpty()) {
            Object oldestModule = null;
            long oldestTime = Long.MAX_VALUE;

            for (Map.Entry<Object, StuckModuleState> entry : activeModules.entrySet()) {
                if (entry.getValue().activateTime < oldestTime) {
                    oldestTime = entry.getValue().activateTime;
                    oldestModule = entry.getKey();
                }
            }

            if (oldestModule != null) {
                StuckModuleState state = activeModules.get(oldestModule);
                if (state != null) {
                    state.hasInitializedState = true;
                }
            }
        }

        if (activeModules.isEmpty()) {
            moveTicks = 0;
            cancelTickCounter = 0;
        }
    }

    public static boolean isMoveCancelled() {
        return !activeModules.isEmpty();
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

    @EventTarget
    public void onPostMotion() {
        pre = false;
    }

    @EventTarget
    public void onPreUpdate() {
        if (isMoveCancelled()) {
            Player player = mc.player;
            if (player == null || moveTicks > 0) return;

            player.setDeltaMovement(motionX, motionY, motionZ);
            player.fallDistance = fallDistance;
        }
    }

    @EventTarget
    public void onPacketSend(Object packet) {
        GetC03StatusUtil.packetEvent(packet);

        if (packet instanceof ServerboundMovePlayerPacket && isMoveCancelled() && moveTicks > 0) {
            Player player = mc.player;
            if (player == null) return;

            motionX = player.getDeltaMovement().x;
            motionY = player.getDeltaMovement().y;
            motionZ = player.getDeltaMovement().z;
            fallDistance = player.fallDistance;
            --moveTicks;
        }
    }



    @EventTarget
    public void onTick(EventStuckTick event) {
        if (mc.player == null) {
            activeModules.clear();
            moveTicks = 0;
            noMoveTick = 0;
            return;
        }

        pre = true;

        if (!isMoveCancelled()) {
            noMoveTick = 0;
            return;
        }
        noMoveTick++;
        if (noMoveTick >= 19) {
            moveTicks = 1;
            noMoveTick = 0;
        }

        if (moveTicks > 0) {
            return;
        }
        Player player = mc.player;
        motionX = player.getDeltaMovement().x;
        motionY = player.getDeltaMovement().y;
        motionZ = player.getDeltaMovement().z;
        fallDistance = player.fallDistance;
        ChatUtils.addChatMessage(String.valueOf(noMovePackets));
    }

    @EventTarget
    public void onMove() {
        if (isMoveCancelled()) {
            if (moveTicks > 0) {
            }
        }
    }

    @EventTarget
    public void onPacketReceive(Object packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            Player player = mc.player;
            if (player == null || velocityPacket.getId() != player.getId()) return;

            if (isMoveCancelled()) {
                player.setDeltaMovement(motionX, motionY, motionZ);
                player.fallDistance = fallDistance;
                moveTicks = Math.max(moveTicks, 1);
            }
        }
    }
}