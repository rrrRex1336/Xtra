package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class PlayerUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean movementInput() {
      return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
   }

   public static int getMoveSpeedEffectAmplifier() {
      return mc.player.hasStatusEffect(StatusEffects.SPEED) ? mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1 : 0;
   }

   public static Vec3d getVectorForRotation(Vector2f rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getY() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getY() * (float) (Math.PI / 180.0)));
      return new Vec3d((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static HitResult pickCustom(double blockReachDistance, float yaw, float pitch) {
      if (mc.player != null && mc.world != null) {
         Vec3d vec3 = mc.player.getCameraPosVec(1.0F);
         Vec3d vec31 = getVectorForRotation(new Vector2f(yaw, pitch));
         Vec3d vec32 = vec3.add(vec31.x * blockReachDistance, vec31.y * blockReachDistance, vec31.z * blockReachDistance);
         return mc.world.raycast(new RaycastContext(vec3, vec32, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      } else {
         return null;
      }
   }
}
