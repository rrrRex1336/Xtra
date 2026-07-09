package awa.qwq.ovo.Naven.managers.rotation.utils;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class RayTraceUtils {
   public static HitResult rayCast(float partialTicks, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = MinecraftClient.getInstance().getCameraEntity();
      if (entity != null && MinecraftClient.getInstance().world != null) {
         double distance = (double)MinecraftClient.getInstance().interactionManager.getReachDistance();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(double range, float partialTicks, boolean hitFluids, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = MinecraftClient.getInstance().getCameraEntity();
      if (entity != null && MinecraftClient.getInstance().world != null) {
         objectMouseOver = pick(range, partialTicks, hitFluids, rotations.getYaw(), rotations.getPitch());
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
      Vec3d vec3 = new Vec3d(MinecraftClient.getInstance().player.getX(), MinecraftClient.getInstance().player.getY() + 1.62, MinecraftClient.getInstance().player.getZ());
      Vec3d vec31 = calculateViewVector(pXRot, pYRot);
      Vec3d vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
      return MinecraftClient.getInstance()
         .player
         .getWorld()
         .raycast(new RaycastContext(vec3, vec32, ShapeType.OUTLINE, pHitFluids ? FluidHandling.ANY : FluidHandling.NONE, MinecraftClient.getInstance().player));
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
      return MinecraftClient.getInstance().world.raycast(var9);
   }

   public static EntityHitResult calculateIntercept(Box instance, Vec3d var1, Vec3d var2) {
      Optional<Vec3d> e = instance.raycast(var1, var2);
      return e.<EntityHitResult>map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
   }

   public static HitResult rayCast(Rotation rotation, double range, float expand, Entity filterEntity, Entity targetEntity, boolean throughWalls) {
      if (filterEntity != null && MinecraftClient.getInstance().world != null) {
         float partialTicks = MinecraftClient.getInstance().getTickDelta();
         Vec3d eyePosition = filterEntity.getCameraPosVec(partialTicks);
         Vec3d lookVector = RotationUtils.getVectorForRotation(rotation);
         Vec3d targetVec = eyePosition.add(lookVector.x * range, lookVector.y * range, lookVector.z * range);
         BlockHitResult blockHit = null;
         double blockDistance = range;
         if (!throughWalls) {
            blockHit = MinecraftClient.getInstance().world.raycast(new RaycastContext(eyePosition, targetVec, ShapeType.OUTLINE, FluidHandling.NONE, filterEntity));
            blockDistance = blockHit.getType() == Type.BLOCK ? eyePosition.distanceTo(blockHit.getPos()) : range;
         }

         double expandedRange = Math.min(range, blockDistance) + (double)expand;
         Box searchBox = new Box(
            eyePosition.x - expandedRange,
            eyePosition.y - expandedRange,
            eyePosition.z - expandedRange,
            eyePosition.x + expandedRange,
            eyePosition.y + expandedRange,
            eyePosition.z + expandedRange
         );
         List<Entity> entities = MinecraftClient.getInstance()
            .world
            .getEntitiesByClass(
               Entity.class,
               searchBox,
               ex -> ex != filterEntity && (targetEntity == null || ex == targetEntity) && EntityPredicates.EXCEPT_SPECTATOR.test(ex) && ex.canHit()
            );
         Entity pointedEntity = null;
         Vec3d hitVec = null;
         double closestDistance = Math.min(range, blockDistance);
         closestDistance *= closestDistance;

         for (Entity e : entities) {
            Box entityBox = e.getBoundingBox().expand((double)expand);
            Optional<Vec3d> intercept = entityBox.raycast(eyePosition, targetVec);
            if (intercept.isPresent()) {
               Vec3d interceptPoint = intercept.get();
               double distSq = eyePosition.squaredDistanceTo(interceptPoint);
               if (distSq < closestDistance) {
                  boolean canHit = true;
                  if (!throughWalls) {
                     Vec3d entityCenter = entityBox.getCenter();
                     BlockHitResult wallCheck = MinecraftClient.getInstance()
                        .world
                        .raycast(new RaycastContext(eyePosition, entityCenter, ShapeType.OUTLINE, FluidHandling.NONE, filterEntity));
                     if (wallCheck.getType() == Type.BLOCK && eyePosition.squaredDistanceTo(wallCheck.getPos()) <= distSq) {
                        canHit = false;
                     }
                  }

                  if (canHit) {
                     closestDistance = distSq;
                     pointedEntity = e;
                     hitVec = interceptPoint;
                  }
               }
            }
         }

         if (pointedEntity != null) {
            return new EntityHitResult(pointedEntity, hitVec);
         } else {
            return !throughWalls && blockHit != null
               ? blockHit
               : MinecraftClient.getInstance().world.raycast(new RaycastContext(eyePosition, targetVec, ShapeType.OUTLINE, FluidHandling.NONE, filterEntity));
         }
      } else {
         return null;
      }
   }

   private RayTraceUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
