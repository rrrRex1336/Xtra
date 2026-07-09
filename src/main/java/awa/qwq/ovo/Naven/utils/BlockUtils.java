package awa.qwq.ovo.Naven.utils;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class BlockUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Box getBoundingBox(BlockPos pos) {
      return getOutlineShape(pos).getBoundingBox().offset(pos);
   }

   private static VoxelShape getOutlineShape(BlockPos pos) {
      return getState(pos).getOutlineShape(mc.world, pos);
   }

   public static BlockState getState(BlockPos pos) {
      return mc.world.getBlockState(pos);
   }

   public static boolean canBeClicked(BlockPos pos) {
      return getOutlineShape(pos) != VoxelShapes.empty();
   }

   public static boolean isAirBlock(BlockPos blockPos) {
      if (mc.world != null && mc.player != null) {
         Block block = mc.world.getBlockState(blockPos).getBlock();
         return block instanceof AirBlock;
      } else {
         return false;
      }
   }
   public static Map<BlockPos, Block> searchBlocks(final int radius) {
      MinecraftClient mc = MinecraftClient.getInstance();
      final Map<BlockPos, Block> blocks = new HashMap<>();

      for (int x = radius; x > -radius; --x) {
         for (int y = radius; y > -radius; --y) {
            for (int z = radius; z > -radius; --z) {
               final BlockPos blockPos = new BlockPos(
                       (int) (mc.player.prevX + x),
                       (int) (mc.player.prevY + y),
                       (int) (mc.player.prevZ + z)
               );
               final Block block = getBlock(blockPos);
               blocks.put(blockPos, block);
            }
         }
      }
      return blocks;
   }

   private static Block getBlock(BlockPos pos) {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc.player.getWorld().getBlockState(pos).getBlock();
   }
}
