package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(
        name = "Flight",
        description = "Attempts to desync brief keepalive timing after knockback",
        category = Category.MOVEMENT
)
public class Flight extends Module {
    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private boolean intercepting;
    private boolean shouldStartFallFlying;
    private int ticksUntilFallFlying;

    @Override
    public void onEnable() {
        resetState(false);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushDelayedPackets();
        resetState(false);
        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn event) {
        if (this.isEnabled()) {
            this.setEnabled(false);
        }
        resetState(true);
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (!shouldStartFallFlying || mc.player == null) {
            return;
        }

        ticksUntilFallFlying++;
        if (ticksUntilFallFlying < 8) {
            return;
        }

        NetworkUtils.sendPacketNoEvent(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
        );
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.SEND) {
            if (intercepting && packet instanceof CommonPongC2SPacket) {
                event.setCancelled(true);
                if (delayedPackets.isEmpty()) {
                    shouldStartFallFlying = true;
                    ticksUntilFallFlying = 0;
                }
                delayedPackets.add(packet);
                if (delayedPackets.size() > 200) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof PlayerInteractEntityC2SPacket) {
                if (intercepting && !delayedPackets.isEmpty()) {
                    flushAndStopIntercepting();
                }
                return;
            }
        }

        if (event.getType() == EventType.RECEIVE) {
            if (packet instanceof PlayerPositionLookS2CPacket) {
                if (intercepting && !delayedPackets.isEmpty()) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof EntityVelocityUpdateS2CPacket motionPacket && motionPacket.getId() == mc.player.getId()) {
                if (intercepting || !delayedPackets.isEmpty()) {
                    return;
                }
                intercepting = true;
                shouldStartFallFlying = false;
                ticksUntilFallFlying = 0;
                event.setCancelled(true);
            }
        }
    }

    private void flushAndStopIntercepting() {
        flushDelayedPackets();
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    private void flushDelayedPackets() {
        while (!delayedPackets.isEmpty()) {
            NetworkUtils.sendPacketNoEvent(delayedPackets.poll());
        }
    }

    private void resetState(boolean clearQueue) {
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
        if (clearQueue) {
            delayedPackets.clear();
        }
    }
}