package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

public class BlockUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static AABB getBoundingBox(BlockPos pos) {
      return getOutlineShape(pos).bounds().move(pos);
   }

   private static VoxelShape getOutlineShape(BlockPos pos) {
      return getState(pos).getShape(mc.level, pos);
   }

   public static BlockState getState(BlockPos pos) {
      return mc.level.getBlockState(pos);
   }

   public static boolean canBeClicked(BlockPos pos) {
      return getOutlineShape(pos) != Shapes.empty();
   }

   public static boolean isAirBlock(BlockPos blockPos) {
      if (mc.level != null && mc.player != null) {
         Block block = mc.level.getBlockState(blockPos).getBlock();
         return block instanceof AirBlock;
      } else {
         return false;
      }
   }
   public static Map<BlockPos, Block> searchBlocks(final int radius) {
      Minecraft mc = Minecraft.getInstance();
      final Map<BlockPos, Block> blocks = new HashMap<>();

      for (int x = radius; x > -radius; --x) {
         for (int y = radius; y > -radius; --y) {
            for (int z = radius; z > -radius; --z) {
               final BlockPos blockPos = new BlockPos(
                       (int) (mc.player.xo + x),
                       (int) (mc.player.yo + y),
                       (int) (mc.player.zo + z)
               );
               final Block block = getBlock(blockPos);
               blocks.put(blockPos, block);
            }
         }
      }
      return blocks;
   }

   private static Block getBlock(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player.level().getBlockState(pos).getBlock();
   }
}
