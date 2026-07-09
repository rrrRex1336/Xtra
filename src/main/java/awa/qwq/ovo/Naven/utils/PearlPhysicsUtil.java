package awa.qwq.ovo.Naven.utils;

import java.util.List;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public final class PearlPhysicsUtil {

    public static final double PEARL_INITIAL_VELOCITY = 1.5;
    public static final double PEARL_GRAVITY = 0.03;
    public static final double PEARL_DRAG = 0.99;

    private PearlPhysicsUtil() {}

    public static Object[] predictPearlLandingWithTicks(Entity pearl, ClientWorld level) {
        double pX = pearl.getX(), pY = pearl.getY(), pZ = pearl.getZ();
        double mX = pearl.getVelocity().x, mY = pearl.getVelocity().y, mZ = pearl.getVelocity().z;

        List<Entity> nearbyEntities = level.getOtherEntities(pearl, pearl.getBoundingBox().expand(128.0),
                e -> e instanceof LivingEntity && !(e instanceof EndermanEntity) && e.isCollidable() && e != ((EnderPearlEntity) pearl).getOwner());

        int ticks = 0;
        for (int i = 0; i < 1000; i++) {
            Vec3d curr = new Vec3d(pX, pY, pZ), next = new Vec3d(pX + mX, pY + mY, pZ + mZ);
            ticks++;

            HitResult hit = RayTraceUtils.rayTraceBlocks(curr, next, false, false, false, pearl);
            if (hit.getType() != HitResult.Type.MISS) return new Object[]{hit.getPos(), ticks};

            for (Entity e : nearbyEntities) {
                HitResult entityHit = RayTraceUtils.calculateIntercept(e.getBoundingBox(), curr, next);
                if (entityHit != null) return new Object[]{entityHit.getPos(), ticks};
            }

            pX += mX; pY += mY; pZ += mZ;
            mX *= PEARL_DRAG; mY *= PEARL_DRAG; mZ *= PEARL_DRAG;
            mY -= PEARL_GRAVITY;
        }
        return new Object[]{null, ticks};
    }

    public static Vector2f calculateOptimalRotations(Vec3d eyePos, Vec3d targetPos) {
        double dX = targetPos.x - eyePos.x, dY = targetPos.y - eyePos.y, dZ = targetPos.z - eyePos.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F);
        double hD = Math.sqrt(dX * dX + dZ * dZ);
        if (hD == 0) return new Vector2f(yaw, dY > 0 ? -90f : 90f);

        double v = PEARL_INITIAL_VELOCITY;
        double g = PEARL_GRAVITY;
        double disc = v * v * v * v - g * (g * hD * hD + 2 * dY * v * v);

        if (disc < 0) return null;
        float pitch = (float) -Math.toDegrees(Math.atan((v * v - Math.sqrt(disc)) / (g * hD)));
        return new Vector2f(yaw, pitch);
    }

    public static boolean isTrajectoryClear(ClientWorld level, PlayerEntity player, Vec3d target) {
        Vector2f rots = calculateOptimalRotations(player.getEyePos(), target);
        if (rots == null) return false;

        Vec3d eyePos = player.getEyePos();
        double pX = eyePos.x, pY = eyePos.y, pZ = eyePos.z;
        float yawRad = (float) Math.toRadians(rots.x + 90.0f);
        float pitchRad = (float) Math.toRadians(-rots.y);

        double mX = Math.cos(yawRad) * Math.cos(pitchRad) * PEARL_INITIAL_VELOCITY;
        double mY = Math.sin(pitchRad) * PEARL_INITIAL_VELOCITY;
        double mZ = Math.sin(yawRad) * Math.cos(pitchRad) * PEARL_INITIAL_VELOCITY;

        for (int i = 0; i < 300; i++) {
            Vec3d currentPos = new Vec3d(pX, pY, pZ);
            Vec3d nextPos = new Vec3d(pX + mX, pY + mY, pZ + mZ);

            if (currentPos.squaredDistanceTo(eyePos) > target.squaredDistanceTo(eyePos)) break;
            if (RayTraceUtils.rayTraceBlocks(currentPos, nextPos, false, false, false, player).getType() != HitResult.Type.MISS) return false;

            for (Entity entity : level.getEntities()) {
                if (entity.equals(player) || !entity.canHit() || !(entity instanceof LivingEntity)) continue;
                if (entity.getBoundingBox().expand(0.3).raycast(currentPos, nextPos).isPresent()) return false;
            }

            pX += mX; pY += mY; pZ += mZ;
            mX *= PEARL_DRAG; mY *= PEARL_DRAG; mZ *= PEARL_DRAG;
            mY -= PEARL_GRAVITY;
        }
        return true;
    }
}