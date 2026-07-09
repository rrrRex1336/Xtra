package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class RayTraceUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static HitResult rayCast(float partialTicks, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.world != null) {
         double distance = (double)mc.interactionManager.getReachDistance();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(double range, float partialTicks, boolean hitFluids, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.world != null) {
         objectMouseOver = pick(range, partialTicks, hitFluids, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(float partialTicks, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.world != null) {
         double distance = (double)mc.interactionManager.getReachDistance();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
      }

      return objectMouseOver;
   }

   public static Vec3d calculateViewVector(float pXRot, float pYRot) {
      float f = pXRot * (float) (Math.PI / 180.0);
      float f1 = -pYRot * (float) (Math.PI / 180.0);
      float f2 = MathHelper.cos(f1);
      float f3 = MathHelper.sin(f1);
      float f4 = MathHelper.cos(f);
      float f5 = MathHelper.sin(f);
      return new Vec3d((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
   }

   public static HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids, float pYRot, float pXRot) {
      Vec3d vec3 = new Vec3d(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
      Vec3d vec31 = calculateViewVector(pXRot, pYRot);
      Vec3d vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
      return mc.player.getWorld().raycast(new RaycastContext(vec3, vec32, ShapeType.OUTLINE, pHitFluids ? FluidHandling.ANY : FluidHandling.NONE, mc.player));
   }

   public static HitResult rayTraceBlocks(
      Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Entity var6
   ) {
      ShapeType var7;
      if (ignoreBlockWithoutBoundingBox) {
         var7 = ShapeType.COLLIDER;
      } else {
         var7 = returnLastUncollidableBlock ? ShapeType.VISUAL : ShapeType.OUTLINE;
      }

      FluidHandling var8 = stopOnLiquid ? FluidHandling.ANY : FluidHandling.NONE;
      RaycastContext var9 = new RaycastContext(vec31, vec32, var7, var8, var6);
      return mc.world.raycast(var9);
   }

   public static EntityHitResult calculateIntercept(Box instance, Vec3d var1, Vec3d var2) {
      Optional<Vec3d> e = instance.raycast(var1, var2);
      return e.<EntityHitResult>map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
   }
}
