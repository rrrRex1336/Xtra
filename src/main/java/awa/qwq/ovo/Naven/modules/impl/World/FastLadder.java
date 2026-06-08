package awa.qwq.ovo.Naven.modules.impl.World;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Combat.KillAura;
import awa.qwq.ovo.Naven.utils.BlockUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

@ModuleInfo(name = "FastLadder", description = "Fall Fast When You On Ladder", category = Category.MOVEMENT)
public class FastLadder extends Module {
    @Override
    public void onEnable() {
    }

    @EventTarget
    public void onUpdate(final EventMotion event) {
        Minecraft mc = Minecraft.getInstance();

        if (KillAura.target != null)
            return;
        boolean isOnLadder = isPlayerOnLadder(mc.player);

        if (isOnLadder && mc.options.keyJump.isDown()) {
            if (mc.player.getDeltaMovement().y >= 0.0) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.1786, mc.player.getDeltaMovement().z);
            }
        }

        Map<BlockPos, Block> searchBlock = BlockUtils.searchBlocks(4);

        for (Map.Entry<BlockPos, Block> block : searchBlock.entrySet()) {
            if (isOnLadder && !mc.options.keyJump.isDown()) {
                if (mc.player.level().getBlockState(block.getKey()).getBlock() instanceof LadderBlock) {
                    Connection connection = mc.player.connection.getConnection();
                    ServerboundPlayerActionPacket abortPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, block.getKey(), Direction.DOWN);
                    connection.send(abortPacket);
                    ServerboundPlayerActionPacket stopPacket = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, block.getKey(), Direction.DOWN);
                    connection.send(stopPacket);

                    mc.player.level().removeBlock(block.getKey(), false);
                }
            }
        }
    }

    private boolean isPlayerOnLadder(Player player) {
        BlockPos playerPos = player.blockPosition();
        for (int y = -1; y <= 0; y++) {
            BlockPos checkPos = playerPos.offset(0, y, 0);
            BlockState state = player.level().getBlockState(checkPos);
            if (state.getBlock() instanceof LadderBlock) {
                return true;
            }
        }
        return false;
    }
}
