package awa.qwq.ovo.Naven.utils;

import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Stream<BlockEntity> getLoadedBlockEntities() {
      return getLoadedChunks().flatMap(chunk -> chunk.getBlockEntities().values().stream());
   }

   public static Stream<WorldChunk> getLoadedChunks() {
      int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = mc.player.getChunkPos();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.<ChunkPos>iterate(min, pos -> {
         int x = pos.x;
         int z = pos.z;
         if (++x > max.x) {
            x = min.x;
            z++;
         }

         if (z > max.z) {
            throw new IllegalStateException("Stream limit didn't work.");
         } else {
            return new ChunkPos(x, z);
         }
      }).limit((long)diameter * (long)diameter).filter(c -> mc.world.isChunkLoaded(c.x, c.z)).map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }
}
