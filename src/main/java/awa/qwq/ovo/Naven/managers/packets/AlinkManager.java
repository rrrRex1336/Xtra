package awa.qwq.ovo.Naven.managers.packets;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.vector.Vector3d;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AlinkManager {

    private static final Minecraft mc = Minecraft.getInstance();
    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);

    @Getter
    private static boolean isActive = false;
    @Getter
    private static Vec3 delayedVelocity = null;
    private static final Queue<Packet<? super ClientPacketListener>> cachedPackets = new ConcurrentLinkedQueue<>();

    @Getter @Setter
    private static long maxAlinkTime = 0;
    private static long startTime = 0;

    @Getter
    private static float progressPercent = 0.0F;
    private static final Map<Entity, Vector3d> trackedEntities = new HashMap<>();
    @Getter @Setter
    private static Player attackTarget = null;
    @Getter @Setter
    private static boolean receivedVelocity = false;
    @Getter
    private static Vec3 storedVelocity = null;

    @EventTarget
    public void onRenderWorld(EventRender event) {
        if (!isActive) return;
        PoseStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : trackedEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof Player)) continue;
            Vector3d pos = entry.getValue();
            if (entity.equals(attackTarget)) {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getBbWidth(), entity.getBbHeight(), new Color(200, 0, 0, 60).getRGB());
            } else {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
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
        this.progress.target = Mth.clamp(progressPercent * 100.0F, 0.0F, 100.0F);
    }

    @EventTarget
    public static boolean onPacketReceive(EventPacket event) {
        if (event.getType() != EventType.RECEIVE) return false;
        if (!isActive) return false;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket ||
                packet instanceof ClientboundPlayerLookAtPacket) {
            pendingRelease = true;
            return false;
        }
        if (packet instanceof ClientboundDisconnectPacket ||
                packet instanceof ClientboundRespawnPacket) {
            cachedPackets.clear();
            isActive = false;
            delayedVelocity = null;
            storedVelocity = null;
            trackedEntities.clear();
            attackTarget = null;
            return false;
        }

        if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            Entity entity = mc.level != null ? mc.level.getEntity(teleportPacket.getId()) : null;
            if (entity != null) {
                trackedEntities.put(entity, new Vector3d(
                        teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ()));
            }
            @SuppressWarnings("unchecked")
            Packet<? super ClientPacketListener> superPacket = (Packet<? super ClientPacketListener>) packet;
            cachedPackets.add(superPacket);
            event.setCancelled(true);
            return true;
        }

        @SuppressWarnings("unchecked")
        Packet<? super ClientPacketListener> superPacket = (Packet<? super ClientPacketListener>) packet;
        cachedPackets.add(superPacket);
        event.setCancelled(true);
        return true;
    }

    private static void releaseAllPackets() {
        while (!cachedPackets.isEmpty()) {
            Packet<? super ClientPacketListener> packet = cachedPackets.poll();
            if (packet != null && mc.getConnection() != null) {
                packet.handle(mc.getConnection());
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