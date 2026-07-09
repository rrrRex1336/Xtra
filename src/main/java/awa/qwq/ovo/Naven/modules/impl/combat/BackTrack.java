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
import java.awt.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;

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
    private final LinkedBlockingDeque<Packet<ClientPlayPacketListener>> interactInbound = new LinkedBlockingDeque<>();

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) return;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
                if (((PlayerEntity) entity).hurtTime > 0) {
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

        if (e.getPacket() instanceof EntityS2CPacket movePacket) {
            e.setCancelled(true);
            Entity entity = movePacket.getEntity(mc.world);
            if (entity != null && targets.containsKey(entity)) {
                Vector3d currentServerPos = serverPositions.getOrDefault(entity,
                        new Vector3d(entity.getX(), entity.getY(), entity.getZ()));

                if (movePacket.isPositionChanged()) {
                    double dx = movePacket.getDeltaX() / 4096.0D;
                    double dy = movePacket.getDeltaY() / 4096.0D;
                    double dz = movePacket.getDeltaZ() / 4096.0D;

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

        if (e.getPacket() instanceof EntityPositionS2CPacket teleportPacket) {
            e.setCancelled(true);
            Entity entity = mc.world.getEntityById(teleportPacket.getId());
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
            if (!(entity instanceof PlayerEntity) || entity == mc.player || ((PlayerEntity) entity).hurtTime == 0) {
                targets.remove(entity);
                serverPositions.remove(entity);
            }
        }
    }

    private void releasePacket() {
        while (!movePacketQueue.isEmpty()) {
            Packet<?> p = movePacketQueue.poll();
            if (p != null && mc.getNetworkHandler() != null)
                ((Packet<ClientPlayNetworkHandler>) p).apply(mc.getNetworkHandler());
        }
        while (!packetQueue.isEmpty()) {
            Packet<?> p = packetQueue.poll();
            if (p != null && mc.getNetworkHandler() != null)
                ((Packet<ClientPlayNetworkHandler>) p).apply(mc.getNetworkHandler());
        }
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (targets.isEmpty()) return;

        MatrixStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : targets.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof PlayerEntity)) continue;

            Vector3d pos = entry.getValue();
            Vector3d serverPos = serverPositions.get(entity);
            if (serverPos != null) {
                double distance = pos.distance(serverPos);
                if (distance >= releaseDistance.getCurrentValue()) {
                    RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                            entity.getWidth(), entity.getHeight(), new Color(255, 0, 0, 80).getRGB());
                } else {
                    RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                            entity.getWidth(), entity.getHeight(), new Color(0, 200, 0, 60).getRGB());
                }
            } else {
                RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                        entity.getWidth(), entity.getHeight(), new Color(0, 200, 0, 60).getRGB());
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