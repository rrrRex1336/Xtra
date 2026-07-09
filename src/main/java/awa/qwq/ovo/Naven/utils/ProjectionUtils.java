package awa.qwq.ovo.Naven.utils;

import org.mixin.accessors.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectionUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static Vector2f project(double x, double y, double z, float renderPartialTicks) {
      Vec3d camera_pos = mc.getEntityRenderDispatcher().camera.getPos();
      Quaternionf cameraRotation = new Quaternionf(mc.getEntityRenderDispatcher().getRotation());
      cameraRotation.conjugate();
      Vector3f result3f = new Vector3f((float)(camera_pos.x - x), (float)(camera_pos.y - y), (float)(camera_pos.z - z));
      result3f.rotate(cameraRotation);
      if ((Boolean)mc.options.getBobView().getValue() && mc.getCameraEntity() instanceof PlayerEntity playerentity) {
         calculateViewBobbing(playerentity, result3f, renderPartialTicks);
      }

      double fov = ((GameRendererAccessor)mc.gameRenderer).invokeGetFov(mc.getEntityRenderDispatcher().camera, renderPartialTicks, true);
      return calculateScreenPosition(result3f, fov);
   }

   private static void calculateViewBobbing(PlayerEntity playerentity, Vector3f result3f, float renderPartialTicks) {
      float walked = playerentity.horizontalSpeed;
      float f = walked - playerentity.prevHorizontalSpeed;
      float f1 = -(walked + f * renderPartialTicks);
      float f2 = MathHelper.lerp(renderPartialTicks, playerentity.prevStrideDistance, playerentity.strideDistance);
      Quaternionf quaternion = new Quaternionf().rotationX(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F * (float) (Math.PI / 180.0));
      quaternion.conjugate();
      result3f.rotate(quaternion);
      Quaternionf quaternion1 = new Quaternionf().rotationZ(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F * (float) (Math.PI / 180.0));
      quaternion1.conjugate();
      result3f.rotate(quaternion1);
      Vector3f bobTranslation = new Vector3f(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
      bobTranslation.y = -bobTranslation.y;
      result3f.add(bobTranslation);
   }

   private static Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
      float halfHeight = (float)mc.getWindow().getScaledHeight() / 2.0F;
      float scaleFactor = halfHeight / (result3f.z() * (float)Math.tan(Math.toRadians(fov / 2.0)));
      return result3f.z() < 0.0F
         ? new Vector2f(
            -result3f.x() * scaleFactor + (float)mc.getWindow().getScaledWidth() / 2.0F,
            (float)mc.getWindow().getScaledHeight() / 2.0F - result3f.y() * scaleFactor
         )
         : new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
   }
}
