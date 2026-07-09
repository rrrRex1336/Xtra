package awa.qwq.ovo.Naven.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public class RegionPos {
   private int x;
   private int z;

   public static RegionPos of(BlockPos pos) {
      return new RegionPos(pos.getX() >> 9 << 9, pos.getZ() >> 9 << 9);
   }

   public static RegionPos of(ChunkPos pos) {
      return new RegionPos(pos.x >> 5 << 9, pos.z >> 5 << 9);
   }

   public RegionPos negate() {
      return new RegionPos(-this.x, -this.z);
   }

   public Vec3d toVec3() {
      return new Vec3d((double)this.x, 0.0, (double)this.z);
   }

   public BlockPos toBlockPos() {
      return new BlockPos(this.x, 0, this.z);
   }

   public RegionPos(int x, int z) {
      this.x = x;
      this.z = z;
   }
}
