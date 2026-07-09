package awa.qwq.ovo.Naven.managers.packets;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.vector.Vector3d;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.LookAtS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AlinkManager {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);

    @Getter
    private static boolean isActive = false;
    @Getter
    private static Vec3d delayedVelocity = null;
    private static final Queue<Packet<? super ClientPlayNetworkHandler>> cachedPackets = new ConcurrentLinkedQueue<>();

    @Getter @Setter
    private static long maxAlinkTime = 0;
    private static long startTime = 0;

    @Getter
    private static float progressPercent = 0.0F;
    private static final Map<Entity, Vector3d> trackedEntities = new HashMap<>();
    @Getter @Setter
    private static PlayerEntity attackTarget = null;
    @Getter @Setter
    private static boolean receivedVelocity = false;
    @Getter
    private static Vec3d storedVelocity = null;

    @EventTarget
    public void onRenderWorld(EventRender event) {
        if (!isActive) return;
        MatrixStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : trackedEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof PlayerEntity)) continue;
            Vector3d pos = entry.getValue();
            if (entity.equals(attackTarget)) {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getWidth(), entity.getHeight(), new Color(200, 0, 0, 60).getRGB());
            } else {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getWidth(), entity.getHeight(), new Color(0, 200, 0, 60).getRGB());
            }
        }
    }

    public static void startAlink() {
        if (isActive) {
            stopAlink();
        }
        isActive = true;
        delayedVelocity = null;
        storedVelocity = null;
        receivedVelocity = false;
        cachedPackets.clear();
        trackedEntities.clear();
        attackTarget = null;
        startTime = System.currentTimeMillis();
    }

    public static void stopAlink() {
        if (!isActive) return;
        pendingRelease = true;
        isActive = false;
        delayedVelocity = null;
        storedVelocity = null;
        receivedVelocity = false;
        trackedEntities.clear();
        attackTarget = null;
    }

    private static volatile boolean pendingRelease = false;

    @EventTarget
    public void onTick(EventRunTicks e) {
        if (!isActive) return;
        if (e.getType() != EventType.PRE) return;
        if (pendingRelease) {
            pendingRelease = false;
            releaseAllPackets();
            isActive = false;
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (maxAlinkTime > 0) {
            if (elapsed >= maxAlinkTime) {
                stopAlink();
                return;
            }
            progressPercent = Math.min(1.0F, (float) elapsed / (float) maxAlinkTime);
        } else {
            progressPercent = Math.min(0.95F, (float) elapsed / 5000.0F);
        }
        this.progress.update(true);
        this.progress.target = MathHelper.clamp(progressPercent * 100.0F, 0.0F, 100.0F);
    }

    @EventTarget
    public static boolean onPacketReceive(EventPacket event) {
        if (event.getType() != EventType.RECEIVE) return false;
        if (!isActive) return false;

        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerPositionLookS2CPacket ||
                packet instanceof LookAtS2CPacket) {
            pendingRelease = true;
            return false;
        }
        if (packet instanceof DisconnectS2CPacket ||
                packet instanceof PlayerRespawnS2CPacket) {
            cachedPackets.clear();
            isActive = false;
            delayedVelocity = null;
            storedVelocity = null;
            trackedEntities.clear();
            attackTarget = null;
            return false;
        }

        if (packet instanceof EntityPositionS2CPacket teleportPacket) {
            Entity entity = mc.world != null ? mc.world.getEntityById(teleportPacket.getId()) : null;
            if (entity != null) {
                trackedEntities.put(entity, new Vector3d(
                        teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ()));
            }
            @SuppressWarnings("unchecked")
            Packet<? super ClientPlayNetworkHandler> superPacket = (Packet<? super ClientPlayNetworkHandler>) packet;
            cachedPackets.add(superPacket);
            event.setCancelled(true);
            return true;
        }

        @SuppressWarnings("unchecked")
        Packet<? super ClientPlayNetworkHandler> superPacket = (Packet<? super ClientPlayNetworkHandler>) packet;
        cachedPackets.add(superPacket);
        event.setCancelled(true);
        return true;
    }

    private static void releaseAllPackets() {
        while (!cachedPackets.isEmpty()) {
            Packet<? super ClientPlayNetworkHandler> packet = cachedPackets.poll();
            if (packet != null && mc.getNetworkHandler() != null) {
                packet.apply(mc.getNetworkHandler());
            }
        }
    }

    public static void reset() {
        isActive = false;
        delayedVelocity = null;
        pendingRelease = true;
        storedVelocity = null;
        receivedVelocity = false;
        cachedPackets.clear();
        trackedEntities.clear();
        attackTarget = null;
    }
}