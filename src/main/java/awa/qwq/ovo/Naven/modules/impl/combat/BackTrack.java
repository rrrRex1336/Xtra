package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.vector.Vector3d;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
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
    private final Map<Entity, Vector3d> targets = new HashMap<>();
    private final LinkedBlockingDeque<Packet<ClientGamePacketListener>> interactInbound = new LinkedBlockingDeque<>();

    @EventTarget
    public void onPacket(EventPacket e) {

        if (e.getPacket() instanceof ClientboundMoveEntityPacket movePacket) {
            e.setCancelled(true);
            Entity entity = movePacket.getEntity(mc.level);
            if (entity != null) {
                Vector3d currentPos = targets.getOrDefault(entity, new Vector3d(entity.getX(), entity.getY(), entity.getZ()));

                if (movePacket.hasPosition()) {
                    double dx = movePacket.getXa() / 4096.0D;
                    double dy = movePacket.getYa() / 4096.0D;
                    double dz = movePacket.getZa() / 4096.0D;

                    targets.put(entity, new Vector3d(currentPos.getX() + dx, currentPos.getY() + dy, currentPos.getZ() + dz));
                }
            }
        }
        if (e.getPacket()  instanceof ClientboundTeleportEntityPacket teleportPacket) {
            e.setCancelled(true);
            Entity entity = mc.level.getEntity(teleportPacket.getId());
            if (entity != null) {
                targets.put(entity, new Vector3d(teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ()));
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
}
