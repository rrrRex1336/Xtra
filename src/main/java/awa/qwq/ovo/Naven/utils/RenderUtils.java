package awa.qwq.ovo.Naven.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private static final Tesselator TESSELATOR = Tesselator.getInstance();
   private static final float[] COLOR_CACHE = new float[4];

   private static final float[] SIN_CACHE = new float[360];
   private static final float[] COS_CACHE = new float[360];
   static {
      for (int i = 0; i < 360; i++) {
         double rad = Math.toRadians(i);
         SIN_CACHE[i] = (float) Math.sin(rad);
         COS_CACHE[i] = (float) Math.cos(rad);
      }
   }

   private static float[] getColor(int color) {
      COLOR_CACHE[0] = (float)(color >> 16 & 0xFF) / 255.0F;
      COLOR_CACHE[1] = (float)(color >> 8 & 0xFF) / 255.0F;
      COLOR_CACHE[2] = (float)(color & 0xFF) / 255.0F;
      COLOR_CACHE[3] = (float)(color >> 24 & 0xFF) / 255.0F;
      return COLOR_CACHE;
   }
   public static void blitPhysical(ResourceLocation texture, int x, int y, int width, int height) {
      Minecraft minecraft = Minecraft.getInstance();
      int windowWidth = minecraft.getWindow().getWidth();
      int windowHeight = minecraft.getWindow().getHeight();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TRANSFORM_BIT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);

      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      BufferBuilder buffer = Tesselator.getInstance().getBuilder();
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      buffer.vertex(x, y, 0).uv(0, 0).endVertex();
      buffer.vertex(x, y + height, 0).uv(0, 1).endVertex();
      buffer.vertex(x + width, y + height, 0).uv(1, 1).endVertex();
      buffer.vertex(x + width, y, 0).uv(1, 0).endVertex();
      BufferUploader.drawWithShader(buffer.end());
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glDisable(GL11.GL_TEXTURE_2D);

      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPopMatrix();

      GL11.glPopAttrib();
   }
   public static int reAlpha(int color, float alpha) {
      int col = MathUtils.clamp((int) (alpha * 255.0F), 0, 255) << 24;
      col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
      col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
      return col | MathUtils.clamp(color & 0xFF, 0, 255);
   }

   public static void drawCircle(PoseStack poseStack, float centerX, float centerY,
                                 float radius, int color, int segments) {
      int actualSegments = Math.min(segments, Math.max(12, (int)(radius * 2)));

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      BufferBuilder buffer = TESSELATOR.getBuilder();
      Matrix4f matrix = poseStack.last().pose();

      float[] rgba = getColor(color);

      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, centerX, centerY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();

      for (int i = 0; i <= actualSegments; i++) {
         double angle = 2.0 * Math.PI * i / actualSegments;
         float x = centerX + (float)(Math.cos(angle) * radius);
         float y = centerY + (float)(Math.sin(angle) * radius);
         buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      }

      TESSELATOR.end();
      RenderSystem.disableBlend();
   }

   public static void drawCircle(PoseStack poseStack, float centerX, float centerY,
                                 float radius, int color) {
      drawCircle(poseStack, centerX, centerY, radius, color, 36);
   }

   private static void drawCircleSector(PoseStack poseStack, float centerX, float centerY,
                                        float radius, float startAngle, float endAngle, int color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      BufferBuilder buffer = TESSELATOR.getBuilder();
      Matrix4f matrix = poseStack.last().pose();

      float[] rgba = getColor(color);

      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      int segments = 36;
      buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, centerX, centerY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();

      float angleRange = endAngle - startAngle;
      if (angleRange < 0) angleRange += 360;

      for (int i = 0; i <= segments; i++) {
         float angle = startAngle + (angleRange * i / segments);
         int ang = (int) angle % 360;
         float x = centerX + COS_CACHE[ang] * radius;
         float y = centerY + SIN_CACHE[ang] * radius;
         buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      }

      TESSELATOR.end();
      RenderSystem.disableBlend();
   }

   public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(2848);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix = poseStack.last().pose();
      float[] rgba = getColor(color);
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      TESSELATOR.end();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3042);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2848);
   }

   private static final int[] WATER_COLORS = {
           0x0CE8C7,
           0x0CA3E8
   };

   public static int getWaterOpaque(int index, float speed) {
      float mappedSpeed = 21.0F - (speed * 1.9F);
      float hue = (float)((System.currentTimeMillis() + (long)index * 50L) % (long)((int)(mappedSpeed * 1000))) / (mappedSpeed * 1000);
      float t = hue;

      int c1 = WATER_COLORS[0];
      int c2 = WATER_COLORS[1];

      int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
      int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
      int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);

      return (r << 16) | (g << 8) | b;
   }

   public static int getWaterOpaque(int index, float saturation, float brightness, float speed) {
      float mappedSpeed = 21.0F - (speed * 1.9F);
      float hue = (float)((System.currentTimeMillis() + (long)index * 50L) % (long)((int)(mappedSpeed * 1000))) / (mappedSpeed * 1000);
      float t = hue;
      int c1 = WATER_COLORS[0];
      int c2 = WATER_COLORS[1];

      int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
      int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
      int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      return Color.HSBtoRGB(hsb[0], saturation, brightness);
   }

   public static void drawOutlineBox(AABB box, PoseStack poseStack) {
      Matrix4f matrix = poseStack.last().pose();
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);

      // 底面
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).endVertex();

      // 顶面
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).endVertex();

      // 垂直线
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).endVertex();

      BufferUploader.drawWithShader(bufferBuilder.end());
   }
   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getBlockPosition();
   }

   public static Vec3 getCameraPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getPosition();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
      Vec3 offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }

   public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      RenderDebug.logRenderState("BEFORE", "fill");
      RenderDebug.checkAlphaState("fill-start");
      RenderDebug.checkBlendState("fill-start");

      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);

      RenderDebug.logRenderState("AFTER", "fill");
      RenderDebug.checkAlphaState("fill-end");
      RenderDebug.checkBlendState("fill-end");
   }

   private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      if (pMinX < pMaxX) {
         float i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         float j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      float[] rgba = getColor(pColor);
      BufferBuilder bufferbuilder = TESSELATOR.getBuilder();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();

      // 关键改动：使用 BufferUploader 代替 TESSELATOR.end()
      BufferUploader.drawWithShader(bufferbuilder.end());

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
      RenderDebug.logRenderState("BEFORE", "drawRoundedRect");
      RenderDebug.checkBlendState("rounded-start");
      RenderDebug.checkAlphaState("rounded-start");
      if (width <= 0 || height <= 0) return; // 提前返回

      BufferBuilder buffer = TESSELATOR.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      float[] rgba = getColor(color);
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      buffer.vertex(matrix, x + width, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
      BufferUploader.drawWithShader(buffer.end());
      RenderDebug.logRenderState("AFTER", "drawRoundedRect");
      RenderDebug.checkBlendState("rounded-end");
   }

   private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      float[] rgba = getColor(color);
      buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
   }

   public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (color == 16777215) {
         color = ARGB32.color(255, 255, 255, 255);
      }

      edgeRadius = Math.max(0.0F, Math.min(edgeRadius, Math.min(width, height) / 2.0F)); // 简化边界检查

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(1.0F);

      drawRectBound(poseStack, x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0F, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + edgeRadius, y, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);

      BufferBuilder buffer = TESSELATOR.getBuilder();
      Matrix4f matrix = poseStack.last().pose();

      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float centerX = x + edgeRadius;
      float centerY = y + edgeRadius;
      color(buffer, matrix, centerX, centerY, color);
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 180) / (double)(vertices * 4);
         color(buffer, matrix,
                 (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                 (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                 color);
      }
      BufferUploader.drawWithShader(buffer.end());
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + edgeRadius;
      color(buffer, matrix, centerX, centerY, color);
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 90) / (double)(vertices * 4);
         color(buffer, matrix,
                 (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                 (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                 color);
      }
      BufferUploader.drawWithShader(buffer.end());
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)(i + 270) / (double)(vertices * 4);
         color(buffer, matrix,
                 (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                 (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                 color);
      }
      BufferUploader.drawWithShader(buffer.end());
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = (Math.PI * 2) * (double)i / (double)(vertices * 4);
         color(buffer, matrix,
                 (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                 (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                 color);
      }
      BufferUploader.drawWithShader(buffer.end());

      RenderSystem.disableBlend();
   }

   public static void drawSolidBox(PoseStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      Matrix4f matrix = matrixStack.last().pose();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawOutlinedBox(PoseStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      drawSolidBox(bb, bufferBuilder);
      BufferUploader.reset();
      vertexBuffer.bind();
      RenderedBuffer buffer = bufferBuilder.end();
      vertexBuffer.upload(buffer);
      VertexBuffer.unbind();
   }

   public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuilder();
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      drawOutlinedBox(bb, bufferBuilder);
      vertexBuffer.upload(bufferBuilder.end());
   }

   public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
      float right = left + width;
      float bottom = top + height;
      fill(stack, left, top, right, bottom, color);
   }

   public static void drawBoxWithCameraOffset(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawPlayerSolidBox(PoseStack poseStack, double x, double y, double z, int color) {
      drawEntitySolidBox(poseStack, x, y, z, 0.6F, 1.8F, color);
   }

   public static void drawEntitySolidBox(PoseStack poseStack, double x, double y, double z, float width, float height, int color) {
      Vec3 cameraPos = getCameraPos();
      float[] rgba = getColor(color);

      poseStack.pushPose();
      poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

      AABB box = new AABB(-width / 2.0, 0, -width / 2.0, width / 2.0, height, width / 2.0);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionShader);
      RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);

      drawSolidBox(box, poseStack);

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();

      poseStack.popPose();
   }
}