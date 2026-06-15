package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ProjectionUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        name = "BedPlates",
        description = "Renders Defense A Bed Has",
        category = Category.VISUAL)
public class BedPlates extends Module {

    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(10.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(5.0f)
            .setMaxFloatValue(30.0f)
            .build()
            .getFloatValue();

    FloatValue layers = ValueBuilder.create(this, "Layers")
            .setDefaultFloatValue(1.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(5.0f)
            .build()
            .getFloatValue();

    private static final int[][] OFFSETS = {
            {0, 1, 0},
            {0, 2, 0},
            {0, 3, 0},
            {0, 4, 0},
            {0, 5, 0}
    };

    private final List<BlockInfo> obstructingBlocks = new ArrayList<>();

    @EventTarget
    public void onUpdate(EventRender event) {
        this.obstructingBlocks.clear();
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int far = (int) this.range.getCurrentValue();
        int maxLayers = (int) this.layers.getCurrentValue();

        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-far, -far, -far),
                playerPos.offset(far, far, far))) {

            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) ||
                    state.getValue(BedBlock.PART) != BedPart.FOOT) {
                continue;
            }

            for (int layer = 0; layer < maxLayers; layer++) {
                int[] offset = OFFSETS[layer];
                BlockPos offsetPos = pos.offset(offset[0], offset[1], offset[2]);
                BlockState offsetState = mc.level.getBlockState(offsetPos);

                if (offsetState.isAir() || offsetState.getBlock() instanceof BedBlock) {
                    continue;
                }

                Vec3 blockCenter = new Vec3(
                        offsetPos.getX() + 0.5,
                        offsetPos.getY() + 0.5,
                        offsetPos.getZ() + 0.5
                );

                Vector2f screenPos = ProjectionUtils.project(
                        blockCenter.x, blockCenter.y, blockCenter.z,
                        event.getRenderPartialTicks()
                );

                this.obstructingBlocks.add(new BlockInfo(
                        offsetState.getBlock().getName().getString(),
                        screenPos,
                        offsetPos,
                        layer
                ));
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        PoseStack stack = event.getStack();
        for (BlockInfo blockInfo : this.obstructingBlocks) {
            stack.pushPose();
            String text = blockInfo.getName();
            if (blockInfo.getLayer() > 0) {
                text = "L" + (blockInfo.getLayer() + 1) + " " + text;
            }
            Fonts.harmony.render(
                    stack,
                    text,
                    blockInfo.getScreenPos().x,
                    blockInfo.getScreenPos().y,
                    Color.WHITE,
                    true,
                    0.25f
            );
            stack.popPose();
        }
    }

    public static List<BlockPos> getBedSurroundingBlocks(BlockPos bedFoot, int maxLayers) {
        List<BlockPos> blocks = new ArrayList<>();
        if (mc.player == null || mc.level == null) return blocks;

        for (int layer = 0; layer < maxLayers; layer++) {
            int[] offset = OFFSETS[layer];
            BlockPos offsetPos = bedFoot.offset(offset[0], offset[1], offset[2]);
            BlockState offsetState = mc.level.getBlockState(offsetPos);

            if (!offsetState.isAir() && !(offsetState.getBlock() instanceof BedBlock)) {
                blocks.add(offsetPos);
            }
        }

        return blocks;
    }

    public static List<BlockPos> getFullBedSurroundingBlocks(BlockPos bedFoot, int maxLayers) {
        List<BlockPos> blocks = new ArrayList<>();
        if (mc.player == null || mc.level == null) return blocks;
        for (int layer = 0; layer < maxLayers; layer++) {
            BlockPos above = bedFoot.above(layer + 1);
            BlockState aboveState = mc.level.getBlockState(above);
            if (!aboveState.isAir() && !(aboveState.getBlock() instanceof BedBlock)) {
                blocks.add(above);
            }
        }
        int[][] sideOffsets = {
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
        };
        for (int[] off : sideOffsets) {
            BlockPos side = bedFoot.offset(off[0], off[1], off[2]);
            BlockState sideState = mc.level.getBlockState(side);
            if (!sideState.isAir() && !(sideState.getBlock() instanceof BedBlock)) {
                blocks.add(side);
            }
        }

        return blocks;
    }

    public static boolean hasBedProtection(BlockPos bedFoot) {
        if (mc.level == null) return false;
        BlockPos above = bedFoot.above();
        BlockState aboveState = mc.level.getBlockState(above);
        return !aboveState.isAir() && !(aboveState.getBlock() instanceof BedBlock);
    }

    private static class BlockInfo {
        private final String name;
        private final Vector2f screenPos;
        private final BlockPos pos;
        private final int layer;

        public BlockInfo(String name, Vector2f screenPos, BlockPos pos, int layer) {
            this.name = name;
            this.screenPos = screenPos;
            this.pos = pos;
            this.layer = layer;
        }

        public String getName() { return name; }
        public Vector2f getScreenPos() { return screenPos; }
        public BlockPos getPos() { return pos; }
        public int getLayer() { return layer; }
    }
}