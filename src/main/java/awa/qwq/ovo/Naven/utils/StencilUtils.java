package awa.qwq.ovo.Naven.utils;

import org.mixin.accessors.RenderTargetAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class StencilUtils {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static void write(boolean renderClipLayer) {
      setupFBO();
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
      if (!renderClipLayer) {
         RenderSystem.colorMask(false, false, false, false);
      }
   }

   public static void erase(boolean invert) {
      RenderSystem.colorMask(true, true, true, true);
      GL11.glStencilFunc(invert ? 514 : 517, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
   }

   public static void dispose() {
      GL11.glDisable(2960);
   }

   public static void setupFBO() {
      if (mc.getFramebuffer().getDepthAttachment() > -1) {
         setupFBO(mc.getFramebuffer());
         ((RenderTargetAccessor)mc.getFramebuffer()).setDepthBufferId(-1);
      }
   }

   public static void setupFBO(Framebuffer fbo) {
      EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.getDepthAttachment());
      int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
      EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
      EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
   }

   public static void initStencilToWrite() {
      StencilUtils.write(false);
   }

   public static void readStencilBuffer() {
      StencilUtils.erase(true);
   }

   public static void beginRoundedStencil() {
      write(false);
   }

   public static void endRoundedStencil() {
      erase(true);
   }
}