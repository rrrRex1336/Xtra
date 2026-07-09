package awa.qwq.ovo.Naven.managers.rotation.utils;

import awa.qwq.ovo.Naven.events.impl.EventAttackYaw;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.utils.MathHelper;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.apache.commons.lang3.RandomUtils;

public class RotationUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static EventAttackYaw targetRotation;

   public static float getAngleDifference(float a, float b) {
      return ((a - b) % 360.0F + 540.0F) % 360.0F - 180.0F;
   }

   public static Vec3d getLook() {
      return getLook(mc.player.getYaw(), mc.player.getPitch());
   }

   public static Vector2f getFixedRotation(float yaw, float pitch, float lastYaw, float lastPitch) {
      float f = (float)((Double)mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F);
      float gcd = f * f * f * 1.2F;
      float deltaYaw = yaw - lastYaw;
      float deltaPitch = pitch - lastPitch;
      float fixedDeltaYaw = deltaYaw - deltaYaw % gcd;
      float fixedDeltaPitch = deltaPitch - deltaPitch % gcd;
      float fixedYaw = lastYaw + fixedDeltaYaw;
      float fixedPitch = lastPitch + fixedDeltaPitch;
      return new Vector2f(fixedYaw, fixedPitch);
   }


   public static Rotation getRotationBlock(final BlockPos pos, float predict) {
      return new Rotation(mc.player.getCameraPosVec(predict), new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
   }

   public static Vec3d getLook(float yaw, float pitch) {
      float f = net.minecraft.util.math.MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = net.minecraft.util.math.MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -net.minecraft.util.math.MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = net.minecraft.util.math.MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
   }

   public static boolean isVecInside(Box self, Vec3d vec) {
      return vec.x > self.minX && vec.x < self.maxX && vec.y > self.minY && vec.y < self.maxY && vec.z > self.minZ && vec.z < self.maxZ;
   }

   public static Rotation getRotations(Vec3d eye, Vec3d target) {
      double x = target.x - eye.x;
      double y = target.y - eye.y;
      double z = target.z - eye.z;
      double diffXZ = Math.sqrt(x * x + z * z);
      float yaw = (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(y, diffXZ)));
      return new Rotation(net.minecraft.util.math.MathHelper.wrapDegrees(yaw), net.minecraft.util.math.MathHelper.wrapDegrees(pitch));
   }

   public static Rotation getRotations(BlockPos pos, float partialTicks) {
      Vec3d playerVector = new Vec3d(
         mc.player.getX() + mc.player.getVelocity().x * (double)partialTicks,
         mc.player.getY() + (double)mc.player.getStandingEyeHeight() + mc.player.getVelocity().getY() * (double)partialTicks,
         mc.player.getZ() + mc.player.getVelocity().getZ() * (double)partialTicks
      );
      double x = (double)pos.getX() - playerVector.x + 0.5;
      double y = (double)pos.getY() - playerVector.y + 0.5;
      double z = (double)pos.getZ() - playerVector.z + 0.5;
      return diffCalc(randomization(x), randomization(y), randomization(z));
   }

   public static Rotation diffCalc(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Rotation(net.minecraft.util.math.MathHelper.wrapDegrees(yaw), net.minecraft.util.math.MathHelper.wrapDegrees(pitch));
   }

   private static double randomization(double value) {
      return value + MathUtils.getRandomDoubleInRange(0.05, 0.08) * (MathUtils.getRandomDoubleInRange(0.0, 1.0) * 2.0 - 1.0);
   }

   public static double getMinDistance(Entity target, Vector2f rotations) {
      double minDistance = Double.MAX_VALUE;
      Iterator var4 = getPossibleEyeHeights().iterator();

      while (var4.hasNext()) {
         double eye = (double)((Float)var4.next()).floatValue();
         Vec3d playerPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
         Vec3d eyePos = playerPosition.add(0.0, eye, 0.0);
         minDistance = Math.min(minDistance, getDistance(target, eyePos, rotations));
      }

      return minDistance;
   }

   public static double getDistance(Entity target, Vec3d eyePos, Vector2f rotations) {
      Box targetBox = getTargetBoundingBox(target);
      HitResult position = getIntercept(targetBox, rotations, eyePos, 6.0);
      if (position != null) {
         Vec3d intercept = position.getPos();
         return intercept.distanceTo(eyePos);
      } else {
         return 1000.0;
      }
   }

   public static HitResult getIntercept(Box targetBox, Vector2f rotations, Vec3d eyePos, double reach) {
      Vec3d vec31 = getLook(rotations.x, rotations.y);
      Vec3d vec32 = eyePos.add(vec31.x * reach, vec31.y * reach, vec31.z * reach);
      return ProjectileUtil.raycast(
         mc.player, eyePos, vec32, targetBox, p_172770_ -> !p_172770_.isSpectator() && p_172770_.canHit(), reach * reach
      );
   }

   public static HitResult getIntercept(Box targetBox, Vector2f rotations, Vec3d eyePos) {
      return getIntercept(targetBox, rotations, eyePos, 6.0);
   }

   public static Vector2f diffCalcVector(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Vector2f(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch));
   }

   public static Vector2f getRotationsVector(Vec3d vec) {
      Vec3d playerVector = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getStandingEyeHeight(), mc.player.getZ());
      double x = vec.x - playerVector.x;
      double y = vec.y - playerVector.y;
      double z = vec.z - playerVector.z;
      return diffCalcVector(x, y, z);
   }

   private static boolean checkHitResult(Vec3d eyePos, HitResult result, Entity target) {
      if (result.getType() == Type.ENTITY && ((EntityHitResult)result).getEntity() == target) {
         Vec3d intercept = result.getPos();
         return isVecInside(getTargetBoundingBox(target), eyePos) || intercept.distanceTo(eyePos) <= 3.0;
      } else {
         return false;
      }
   }

   private static HitResult rayTrace(Rotation rotations) {
      double d0 = (double)mc.interactionManager.getReachDistance();
      HitResult hitResult = RayTraceUtils.rayCast(d0, 1.0F, false, rotations);
      Vec3d vec3 = mc.player.getCameraPosVec(1.0F);
      boolean flag = false;
      double d1 = d0;
      if (mc.interactionManager.hasExtendedReach()) {
         d1 = 6.0;
         d0 = d1;
      } else if (d0 > 3.0) {
         flag = true;
      }

      d1 *= d1;
      if (hitResult != null) {
         d1 = hitResult.getPos().squaredDistanceTo(vec3);
      }

      Vec3d vec31 = getLook(rotations.getYaw(), rotations.getPitch());
      Vec3d vec32 = vec3.add(vec31.x * d0, vec31.y * d0, vec31.z * d0);
      Box aabb = mc.player.getBoundingBox().stretch(vec31.multiply(d0)).expand(1.0, 1.0, 1.0);
      EntityHitResult entityhitresult = ProjectileUtil.raycast(
         mc.player, vec3, vec32, aabb, p_172770_ -> !p_172770_.isSpectator() && p_172770_.canHit(), d1
      );
      if (entityhitresult != null) {
         Vec3d vec33 = entityhitresult.getPos();
         double d2 = vec3.squaredDistanceTo(vec33);
         if (flag && d2 > 9.0) {
            hitResult = BlockHitResult.createMissed(vec33, Direction.getFacing(vec31.x, vec31.y, vec31.z), BlockPos.ofFloored(vec33));
         } else if (d2 < d1 || hitResult == null) {
            hitResult = entityhitresult;
         }
      }

      return hitResult;
   }

   public static Vec3d getVectorForRotation(Rotation rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getPitch() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getPitch() * (float) (Math.PI / 180.0)));
      return new Vec3d((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static RotationUtils.Data getRotationDataToEntity(Entity target) {
      Vec3d playerPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      Vec3d eyePos = playerPosition.add(0.0, (double)mc.player.getStandingEyeHeight(), 0.0);
      Box targetBox = getTargetBoundingBox(target);
      double minX = targetBox.minX;
      double minY = targetBox.minY;
      double minZ = targetBox.minZ;
      double maxX = targetBox.maxX;
      double maxY = targetBox.maxY;
      double maxZ = targetBox.maxZ;
      double spacing = 0.1;
      Set<Vec3d> points = new OrderedHashSet();
      points.add(new Vec3d(minX + maxX / 2.0, minY + maxY / 2.0, minZ + maxZ / 2.0));
      points.add(getClosestPoint(eyePos, targetBox));

      for (double x = minX; x <= maxX; x += spacing) {
         for (double y = minY; y <= maxY; y += spacing) {
            points.add(new Vec3d(x, y, minZ));
            points.add(new Vec3d(x, y, maxZ));
         }
      }

      for (double x = minX; x <= maxX; x += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3d(x, minY, z));
            points.add(new Vec3d(x, maxY, z));
         }
      }

      for (double y = minY; y <= maxY; y += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3d(minX, y, z));
            points.add(new Vec3d(maxX, y, z));
         }
      }

      for (Vec3d point : points) {
         Rotation bruteRotations = getRotations(eyePos, point);
         HitResult bruteHitResult = rayTrace(bruteRotations);
         if (checkHitResult(eyePos, bruteHitResult, target)) {
            Vec3d location = bruteHitResult.getPos();
            return new RotationUtils.Data(
               eyePos,
               location,
               location.distanceTo(eyePos),
               getFixedRotation(bruteRotations.getYaw(), bruteRotations.getPitch(), RotationManager.lastRotations.x, RotationManager.lastRotations.y)
            );
         }
      }

      return new RotationUtils.Data(eyePos, eyePos, 1000.0, null);
   }

   private static Box getTargetBoundingBox(Entity entity) {
      return entity.getBoundingBox();
   }

   public static List<Float> getPossibleEyeHeights() {
      return List.of(mc.player.getStandingEyeHeight());
   }

   public static Vec3d getClosestPoint(Vec3d vec, Box aabb) {
      double closestX = Math.max(aabb.minX, Math.min(vec.x, aabb.maxX));
      double closestY = Math.max(aabb.minY, Math.min(vec.y, aabb.maxY));
      double closestZ = Math.max(aabb.minZ, Math.min(vec.z, aabb.maxZ));
      return new Vec3d(closestX, closestY, closestZ);
   }

   public static Vector2f getRotations(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         double diffX = entity.getX() - mc.player.getX();
         double diffZ = entity.getZ() - mc.player.getZ();
         double diffY = entity.getY() + (double)entity.getStandingEyeHeight() - (mc.player.getY() + (double)mc.player.getStandingEyeHeight());
         return diffCalcVector(diffX, diffY, diffZ);
      }
   }
   public static Vector2f getRotations(Vec3d target) {
      Vec3d eyesPos = mc.player.getCameraPosVec(1.0F);
      double diffX = target.x - eyesPos.x;
      double diffY = target.y - eyesPos.y;
      double diffZ = target.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

      float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

      return new Vector2f(yaw, pitch);
   }

   public static float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
      return updateRotation(currentYaw, calcYaw, yawSpeed);
   }

   public static float updateRotation(float current, float calc, float maxDelta) {
      float f = MathHelper.wrapDegrees(calc - current);
      if (f > maxDelta) {
         f = maxDelta;
      }

      if (f < -maxDelta) {
         f = -maxDelta;
      }

      return current + f;
   }

   public static boolean inFoV(Entity entity, float fov) {
      Vector2f rotations = getRotations(entity);
      float diff = Math.abs(mc.player.getYaw() % 360.0F - rotations.x);
      float minDiff = Math.abs(Math.min(diff, 360.0F - diff));
      return minDiff <= fov;
   }

   public static float getDistanceBetweenAngles(float angle1, float angle2) {
      float angle3 = Math.abs(angle1 - angle2) % 360.0F;
      if (angle3 > 180.0F) {
         angle3 = 0.0F;
      }

      return angle3;
   }

   public static Vec3d getEyesPos() {
      return new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
   }

    public static float rotateToPitch(float speed, float currentPitch, float targetPitch) {
        float delta = net.minecraft.util.math.MathHelper.wrapDegrees(targetPitch - currentPitch);
        if (delta > speed) delta = speed;
        if (delta < -speed) delta = -speed;
        return currentPitch + delta;
    }

    /* ——可选重载：保留你原来那版带随机抖动的实现（如果别处用到了这个签名）——
     * 参数形式：rotateToPitch(pitchSpeed, currentRots[yaw,pitch], calcPitch)
     * 注意：需要你项目里已有 RandomUtils。
     */
    public static float rotateToPitch(float pitchSpeed, float[] currentRots, float calcPitch) {
        // 先用上面的简单版做一步平滑（顺手加一点随机速度）
        float pitch = rotateToPitch(
                pitchSpeed + RandomUtils.nextFloat(0.0F, 15.0F),
                currentRots[1],
                calcPitch
        );

        // 若未完全到达目标，按你原逻辑增加轻微抖动
        if (pitch != calcPitch) {
            pitch += (float) (RandomUtils.nextFloat(1.0F, 2.0F)
                    * Math.sin(currentRots[0] * Math.PI));
        }
        return pitch;
    }

   public static Vec3d getVectorForRotation(float pitch, float yaw) {
      return null;
   }

   public static BlockHitResult rayCast(float v, float v1, float v2) {
       return null;
   }

   public static class Data {
      private final Vec3d eye;
      private final Vec3d hitVec;
      private final double distance;
      private final Vector2f rotation;

      public Data(Vec3d eye, Vec3d hitVec, double distance, Vector2f rotation) {
         this.eye = eye;
         this.hitVec = hitVec;
         this.distance = distance;
         this.rotation = rotation;
      }

      public Vec3d getEye() {
         return this.eye;
      }

      public Vec3d getHitVec() {
         return this.hitVec;
      }

      public double getDistance() {
         return this.distance;
      }

      public Vector2f getRotation() {
         return this.rotation;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof RotationUtils.Data other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getDistance(), other.getDistance()) != 0) {
            return false;
         } else {
            Object this$eye = this.getEye();
            Object other$eye = other.getEye();
            if (this$eye == null ? other$eye == null : this$eye.equals(other$eye)) {
               Object this$hitVec = this.getHitVec();
               Object other$hitVec = other.getHitVec();
               if (this$hitVec == null ? other$hitVec == null : this$hitVec.equals(other$hitVec)) {
                  Object this$rotation = this.getRotation();
                  Object other$rotation = other.getRotation();
                  return this$rotation == null ? other$rotation == null : this$rotation.equals(other$rotation);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof RotationUtils.Data;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $distance = Double.doubleToLongBits(this.getDistance());
         result = result * 59 + (int)($distance >>> 32 ^ $distance);
         Object $eye = this.getEye();
         result = result * 59 + ($eye == null ? 43 : $eye.hashCode());
         Object $hitVec = this.getHitVec();
         result = result * 59 + ($hitVec == null ? 43 : $hitVec.hashCode());
         Object $rotation = this.getRotation();
         return result * 59 + ($rotation == null ? 43 : $rotation.hashCode());
      }

      @Override
      public String toString() {
         return "RotationUtils.Data(eye="
            + this.getEye()
            + ", hitVec="
            + this.getHitVec()
            + ", distance="
            + this.getDistance()
            + ", rotation="
            + this.getRotation()
            + ")";
      }
   }
}
