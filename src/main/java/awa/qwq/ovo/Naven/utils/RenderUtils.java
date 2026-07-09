package awa.qwq.ovo.Naven.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.*;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper.Argb;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Box DEFAULT_BOX = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private static final Tessellator TESSELATOR = Tessellator.getInstance();
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
   public static void blitPhysical(Identifier texture, int x, int y, int width, int height) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      int windowWidth = minecraft.getWindow().getFramebufferWidth();
      int windowHeight = minecraft.getWindow().getFramebufferHeight();
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
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder buffer = Tessellator.getInstance().getBuffer();
      buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      buffer.vertex(x, y, 0).texture(0, 0).next();
      buffer.vertex(x, y + height, 0).texture(0, 1).next();
      buffer.vertex(x + width, y + height, 0).texture(1, 1).next();
      buffer.vertex(x + width, y, 0).texture(1, 0).next();
      BufferRenderer.drawWithGlobalProgram(buffer.end());
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

   public static void drawCircle(MatrixStack poseStack, float centerX, float centerY,
                                 float radius, int color, int segments) {
      int actualSegments = Math.min(segments, Math.max(12, (int)(radius * 2)));

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      BufferBuilder buffer = TESSELATOR.getBuffer();
      Matrix4f matrix = poseStack.peek().getPositionMatrix();

      float[] rgba = getColor(color);

      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, centerX, centerY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();

      for (int i = 0; i <= actualSegments; i++) {
         double angle = 2.0 * Math.PI * i / actualSegments;
         float x = centerX + (float)(Math.cos(angle) * radius);
         float y = centerY + (float)(Math.sin(angle) * radius);
         buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      }

      TESSELATOR.draw();
      RenderSystem.disableBlend();
   }

   public static void drawCircle(MatrixStack poseStack, float centerX, float centerY,
                                 float radius, int color) {
      drawCircle(poseStack, centerX, centerY, radius, color, 36);
   }

   private static void drawCircleSector(MatrixStack poseStack, float centerX, float centerY,
                                        float radius, float startAngle, float endAngle, int color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      BufferBuilder buffer = TESSELATOR.getBuffer();
      Matrix4f matrix = poseStack.peek().getPositionMatrix();

      float[] rgba = getColor(color);

      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      int segments = 36;
      buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, centerX, centerY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();

      float angleRange = endAngle - startAngle;
      if (angleRange < 0) angleRange += 360;

      for (int i = 0; i <= segments; i++) {
         float angle = startAngle + (angleRange * i / segments);
         int ang = (int) angle % 360;
         float x = centerX + COS_CACHE[ang] * radius;
         float y = centerY + SIN_CACHE[ang] * radius;
         buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      }

      TESSELATOR.draw();
      RenderSystem.disableBlend();
   }

   public static void drawTracer(MatrixStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glEnable(2848);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Matrix4f matrix = poseStack.peek().getPositionMatrix();
      float[] rgba = getColor(color);
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      TESSELATOR.draw();
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

   public static void drawOutlineBox(Box box, MatrixStack poseStack) {
      Matrix4f matrix = poseStack.peek().getPositionMatrix();
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);

      // 底面
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).next();

      // 顶面
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).next();

      // 垂直线
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).next();
      bufferBuilder.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).next();

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }
   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getBlockPos();
   }

   public static Vec3d getCameraPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getPos();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(MatrixStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(MatrixStack matrixStack, RegionPos region) {
      Vec3d offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }

   public static void fill(MatrixStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      RenderDebug.logRenderState("BEFORE", "fill");
      RenderDebug.checkAlphaState("fill-start");
      RenderDebug.checkBlendState("fill-start");

      innerFill(pPoseStack.peek().getPositionMatrix(), pMinX, pMinY, pMaxX, pMaxY, pColor);

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
      BufferBuilder bufferbuilder = TESSELATOR.getBuffer();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      bufferbuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();

      // 关键改动：使用 BufferUploader 代替 TESSELATOR.end()
      BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   public static void drawRectBound(MatrixStack poseStack, float x, float y, float width, float height, int color) {
      RenderDebug.logRenderState("BEFORE", "drawRoundedRect");
      RenderDebug.checkBlendState("rounded-start");
      RenderDebug.checkAlphaState("rounded-start");
      if (width <= 0 || height <= 0) return; // 提前返回

      BufferBuilder buffer = TESSELATOR.getBuffer();
      Matrix4f matrix = poseStack.peek().getPositionMatrix();
      float[] rgba = getColor(color);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      buffer.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      buffer.vertex(matrix, x + width, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderDebug.logRenderState("AFTER", "drawRoundedRect");
      RenderDebug.checkBlendState("rounded-end");
   }

   private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      float[] rgba = getColor(color);
      buffer.vertex(matrix, x, y, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]).next();
   }

   public static void drawRoundedRect(MatrixStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (color == 16777215) {
         color = Argb.getArgb(255, 255, 255, 255);
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

      BufferBuilder buffer = TESSELATOR.getBuffer();
      Matrix4f matrix = poseStack.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      buffer.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      buffer.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
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
      BufferRenderer.drawWithGlobalProgram(buffer.end());

      RenderSystem.disableBlend();
   }

   public static void drawSolidBox(MatrixStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(Box bb, MatrixStack matrixStack) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   public static void drawOutlinedBox(MatrixStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(Box bb, MatrixStack matrixStack) {
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      bufferBuilder.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).next();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).next();
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   public static void drawSolidBox(Box bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION);
      drawSolidBox(bb, bufferBuilder);
      BufferRenderer.reset();
      vertexBuffer.bind();
      BuiltBuffer buffer = bufferBuilder.end();
      vertexBuffer.upload(buffer);
      VertexBuffer.unbind();
   }

   public static void drawSolidBox(Box bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
   }

   public static void drawOutlinedBox(Box bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = TESSELATOR.getBuffer();
      bufferBuilder.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION);
      drawOutlinedBox(bb, bufferBuilder);
      vertexBuffer.upload(bufferBuilder.end());
   }

   public static void drawOutlinedBox(Box bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
   }

   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(MatrixStack stack, float left, float top, float width, float height, int color) {
      float right = left + width;
      float bottom = top + height;
      fill(stack, left, top, right, bottom, color);
   }

   public static void drawBoxWithCameraOffset(BufferBuilder bufferBuilder, Matrix4f matrix, Box box) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
      bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(matrix, minX, minY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).next();
      bufferBuilder.vertex(matrix, minX, minY, minZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, minY, minZ).next();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).next();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).next();
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   public static void drawPlayerSolidBox(MatrixStack poseStack, double x, double y, double z, int color) {
      drawEntitySolidBox(poseStack, x, y, z, 0.6F, 1.8F, color);
   }

   public static void drawEntitySolidBox(MatrixStack poseStack, double x, double y, double z, float width, float height, int color) {
      Vec3d cameraPos = getCameraPos();
      float[] rgba = getColor(color);

      poseStack.push();
      poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

      Box box = new Box(-width / 2.0, 0, -width / 2.0, width / 2.0, height, width / 2.0);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);

      drawSolidBox(box, poseStack);

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();

      poseStack.pop();
   }
}
