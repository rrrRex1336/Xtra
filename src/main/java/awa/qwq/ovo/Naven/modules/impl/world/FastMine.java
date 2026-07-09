package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.player.Blink;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(
        name = "FastMine",
        description = "Fast break up blocks.",
        category = Category.WORLD
)
public class FastMine extends Module {
    FloatValue speed = ValueBuilder.create(this, "Speed").setDefaultFloatValue(1.3F).setMaxFloatValue(2.0F).setMinFloatValue(1.0F).setFloatStep(0.1F).build().getFloatValue();

    PlayerActionC2SPacket packet;
    float damage;

    private boolean isSendingOwnPackets = false;

    @EventTarget
    public void onPacket(EventPacket event) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (isSendingOwnPackets) {
            return;
        }

        if (!Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            if (event.getPacket() instanceof PlayerActionC2SPacket packet) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos position = new BlockPos((int) (mc.player.getX() + x), (int) (mc.player.getY() - y), (int) (mc.player.getZ() + z));
                            if (packet.getPos().equals(position)) {
                                return;
                            }
                        }
                    }
                }

                PlayerActionC2SPacket.Action action = packet.getAction();
                if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                    this.packet = packet;
                    damage = 0.0f;
                    isSendingOwnPackets = true;
                    try {
                        ClientConnection connection = mc.player.networkHandler.getConnection();
                        connection.send(event.getPacket());
                        PlayerActionC2SPacket abortPacket = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                        connection.send(abortPacket);
                        PlayerActionC2SPacket stopPacket = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                        connection.send(stopPacket);
                        event.setCancelled(true);

                    } finally {
                        isSendingOwnPackets = false;
                    }

                } else if (action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK
                        || action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                    this.packet = null;
                }
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (event.getType() == EventType.PRE && packet != null) {
            BlockState blockState = mc.player.getWorld().getBlockState(packet.getPos());
            float hardness = blockState.calcBlockBreakingDelta(mc.player, mc.player.getWorld(), packet.getPos());
            damage += hardness * speed.getCurrentValue();

            if (damage >= 1.0f) {
                mc.player.getWorld().removeBlock(packet.getPos(), false);
                isSendingOwnPackets = true;

                try {
                    ClientConnection connection = mc.player.networkHandler.getConnection();

                    PlayerActionC2SPacket abortPacket = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                    connection.send(abortPacket);

                    PlayerActionC2SPacket stopPacket = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                    connection.send(stopPacket);

                } finally {
                    isSendingOwnPackets = false;
                }

                damage = 0.0f;
                packet = null;
            }
        }
    }
}