package awa.qwq.ovo.Naven.modules.impl.World;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Player.Blink;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.state.BlockState;

@ModuleInfo(
        name = "FastMine",
        description = "Fast break up blocks.",
        category = Category.WORLD
)
public class FastMine extends Module {
    FloatValue speed = ValueBuilder.create(this, "Speed").setDefaultFloatValue(1.3F).setMaxFloatValue(2.0F).setMinFloatValue(1.0F).setFloatStep(0.1F).build().getFloatValue();

    ServerboundPlayerActionPacket packet;
    float damage;

    private boolean isSendingOwnPackets = false;

    @EventTarget
    public void onPacket(EventPacket event) {
        Minecraft mc = Minecraft.getInstance();

        if (isSendingOwnPackets) {
            return;
        }

        if (!Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
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

                ServerboundPlayerActionPacket.Action action = packet.getAction();
                if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                    this.packet = packet;
                    damage = 0.0f;
                    isSendingOwnPackets = true;
                    try {
                        Connection connection = mc.player.connection.getConnection();
                        connection.send(event.getPacket());
                        ServerboundPlayerActionPacket abortPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                        connection.send(abortPacket);
                        ServerboundPlayerActionPacket stopPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                        connection.send(stopPacket);
                        event.setCancelled(true);

                    } finally {
                        isSendingOwnPackets = false;
                    }

                } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                        || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    this.packet = null;
                }
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        Minecraft mc = Minecraft.getInstance();

        if (event.getType() == EventType.PRE && packet != null) {
            BlockState blockState = mc.player.level().getBlockState(packet.getPos());
            float hardness = blockState.getDestroyProgress(mc.player, mc.player.level(), packet.getPos());
            damage += hardness * speed.getCurrentValue();

            if (damage >= 1.0f) {
                mc.player.level().removeBlock(packet.getPos(), false);
                isSendingOwnPackets = true;

                try {
                    Connection connection = mc.player.connection.getConnection();

                    ServerboundPlayerActionPacket abortPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
                    connection.send(abortPacket);

                    ServerboundPlayerActionPacket stopPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), packet.getDirection());
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