package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.vector.Vector3d;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(name = "BackTrack", description = "Delay!", category = Category.COMBAT)
public class BackTrack extends Module {

    private final BooleanValue infinity = ValueBuilder.create(this, "Infinity")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue resetVelocity = ValueBuilder.create(this, "Reset Velocity")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final FloatValue releaseDistance = ValueBuilder.create(this, "Release Distance")
            .setDefaultFloatValue(5.2F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private final FloatValue releaseTick = ValueBuilder.create(this, "Release Tick")
            .setVisibility(() -> !this.infinity.getCurrentValue())
            .setDefaultFloatValue(100.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(200.0F)
            .build()
            .getFloatValue();

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> movePacketQueue = new ConcurrentLinkedQueue<>();
    private final Map<Entity, Vector3d> targets = new ConcurrentHashMap<>();
    private final Map<Entity, Vector3d> serverPositions = new ConcurrentHashMap<>();
    private final LinkedBlockingDeque<Packet<ClientGamePacketListener>> interactInbound = new LinkedBlockingDeque<>();

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.level == null) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && entity != mc.player) {
                if (((Player) entity).hurtTime > 0) {
                    if (!targets.containsKey(entity)) {
                        Vector3d serverPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
                        serverPositions.put(entity, serverPos);
                        targets.put(entity, serverPos);
                    }
                } else {
                    targets.remove(entity);
                    serverPositions.remove(entity);
                }
            }
        }

        if (e.getPacket() instanceof ClientboundMoveEntityPacket movePacket) {
            e.setCancelled(true);
            Entity entity = movePacket.getEntity(mc.level);
            if (entity != null && targets.containsKey(entity)) {
                Vector3d currentServerPos = serverPositions.getOrDefault(entity,
                        new Vector3d(entity.getX(), entity.getY(), entity.getZ()));

                if (movePacket.hasPosition()) {
                    double dx = movePacket.getXa() / 4096.0D;
                    double dy = movePacket.getYa() / 4096.0D;
                    double dz = movePacket.getZa() / 4096.0D;

                    Vector3d newServerPos = new Vector3d(
                            currentServerPos.getX() + dx,
                            currentServerPos.getY() + dy,
                            currentServerPos.getZ() + dz
                    );
                    serverPositions.put(entity, newServerPos);
                    Vector3d targetPos = targets.get(entity);
                    double distance = targetPos.distance(newServerPos);

                    if (distance >= releaseDistance.getCurrentValue()) {
                        targets.put(entity, newServerPos);
                    }
                }
            }
        }

        if (e.getPacket() instanceof ClientboundTeleportEntityPacket teleportPacket) {
            e.setCancelled(true);
            Entity entity = mc.level.getEntity(teleportPacket.getId());
            if (entity != null && targets.containsKey(entity)) {
                Vector3d newServerPos = new Vector3d(
                        teleportPacket.getX(),
                        teleportPacket.getY(),
                        teleportPacket.getZ()
                );
                serverPositions.put(entity, newServerPos);

                Vector3d targetPos = targets.get(entity);
                double distance = targetPos.distance(newServerPos);

                if (distance >= releaseDistance.getCurrentValue()) {
                    targets.put(entity, newServerPos);
                }
            }
        }
        for (Entity entity : targets.keySet()) {
            if (!(entity instanceof Player) || entity == mc.player || ((Player) entity).hurtTime == 0) {
                targets.remove(entity);
                serverPositions.remove(entity);
            }
        }
    }

    private void releasePacket() {
        while (!movePacketQueue.isEmpty()) {
            Packet<?> p = movePacketQueue.poll();
            if (p != null && mc.getConnection() != null)
                ((Packet<ClientPacketListener>) p).handle(mc.getConnection());
        }
        while (!packetQueue.isEmpty()) {
            Packet<?> p = packetQueue.poll();
            if (p != null && mc.getConnection() != null)
                ((Packet<ClientPacketListener>) p).handle(mc.getConnection());
        }
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (targets.isEmpty()) return;

        PoseStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : targets.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof Player)) continue;

            Vector3d pos = entry.getValue();
            Vector3d serverPos = serverPositions.get(entity);
            if (serverPos != null) {
                double distance = pos.distance(serverPos);
                if (distance >= releaseDistance.getCurrentValue()) {
                    RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                            entity.getBbWidth(), entity.getBbHeight(), new Color(255, 0, 0, 80).getRGB());
                } else {
                    RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                            entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
                }
            } else {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        targets.clear();
        serverPositions.clear();
    }
}