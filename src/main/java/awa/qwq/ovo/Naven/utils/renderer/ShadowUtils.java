package awa.qwq.ovo.Naven.utils.renderer;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;

public class ShadowUtils {
   private static final TimeHelper shadowTimer = new TimeHelper();
   private static Framebuffer mainRenderBuffer;
   private static Framebuffer blurBuffer;
   private static Shader blurShader;
   private static boolean initialized = false;
   private static boolean hasFailed = false;
   private static boolean hasRenderedFrame = false;
   private static int lastWidth = -1;
   private static int lastHeight = -1;

   private static boolean stencilWasEnabled = false;
   private static int savedStencilFunc = GL11.GL_ALWAYS;
   private static int savedStencilRef = 0;
   private static int savedStencilMask = 0xFFFFFFFF;
   private static boolean depthWasEnabled = false;
   private static boolean blendWasEnabled = false;

   public static void onRenderAfterWorld(EventRender2D e, float fps, float strength) {
      if (hasFailed) {
         return;
      }

      boolean renderStateSaved = false;
      boolean stencilStateSaved = false;
      try {
         Window window = MinecraftClient.getInstance().getWindow();
         int width = window.getFramebufferWidth();
         int height = window.getFramebufferHeight();

         if (!initialized || width != lastWidth || height != lastHeight) {
            cleanup();
            if (!initialize()) {
               return;
            }
            lastWidth = width;
            lastHeight = height;
            hasRenderedFrame = false;
         }

         boolean shouldRefresh = !hasRenderedFrame || shadowTimer.delay(1000.0F / Math.max(1.0F, fps));
         saveRenderState();
         renderStateSaved = true;
         saveAndResetStencilState();
         stencilStateSaved = true;

         if (shouldRefresh) {
            renderToBuffer(e);
            updateBlurBuffer(e, window, strength);
            shadowTimer.reset();
            hasRenderedFrame = true;
         }

         renderCachedResult(e, window, strength);
      } catch (Exception ex) {
         System.err.println("[ShadowUtils] Render failed: " + ex.getMessage());
         ex.printStackTrace();
         hasFailed = true;
         cleanup();
      } finally {
         if (stencilStateSaved) {
            restoreStencilState();
         }
         if (renderStateSaved) {
            restoreRenderState();
         }
      }
   }

   private static boolean initialize() {
      try {
         GL11.glGetError();
         blurShader = new Shader("shadow.vert", "shadow.frag");
         mainRenderBuffer = new Framebuffer();
         blurBuffer = new Framebuffer();

         int error = GL11.glGetError();
         if (error != GL11.GL_NO_ERROR) {
            System.err.println("[ShadowUtils] OpenGL init error: " + error);
            cleanup();
            hasFailed = true;
            return false;
         }

         initialized = true;
         return true;
      } catch (Exception e) {
         System.err.println("[ShadowUtils] Init failed: " + e.getMessage());
         e.printStackTrace();
         cleanup();
         hasFailed = true;
         return false;
      }
   }

   private static void renderToBuffer(EventRender2D e) {
      mainRenderBuffer.bind();
      mainRenderBuffer.setViewport();
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), e.getGuiGraphics(), EventType.SHADOW));

      mainRenderBuffer.unbind();
   }

   private static void updateBlurBuffer(EventRender2D e, Window window, float strength) {
      GL.enableBlend();
      GL11.glDisable(GL11.GL_DEPTH_TEST);

      blurShader.bind();
      setBlurUniforms(window, strength);

      PostProcessRenderer.beginRender(e.getStack());
      blurBuffer.bind();
      blurBuffer.setViewport();
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
      GL.bindTexture(mainRenderBuffer.texture);
      blurShader.set("u_Texture", 0);
      blurShader.set("u_Direction", 1.0, 0.0);
      PostProcessRenderer.render(e.getStack());
      PostProcessRenderer.endRender();

      blurBuffer.unbind();
   }

   private static void renderCachedResult(EventRender2D e, Window window, float strength) {
      MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
      GL.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
      GL.enableBlend();
      GL11.glDisable(GL11.GL_DEPTH_TEST);

      blurShader.bind();
      setBlurUniforms(window, strength);

      PostProcessRenderer.beginRender(e.getStack());
      GL.bindTexture(blurBuffer.texture);
      blurShader.set("u_Texture", 0);
      blurShader.set("u_Direction", 0.0, 1.0);
      PostProcessRenderer.render(e.getStack());
      PostProcessRenderer.endRender();
   }

   private static void setBlurUniforms(Window window, float strength) {
      blurShader.set("u_Size", (double)window.getFramebufferWidth(), (double)window.getFramebufferHeight());
      blurShader.set("u_Radius", (double)(2.0F + strength * 1.2F));
      blurShader.set("u_Intensity", (double)(0.5F + strength * 0.15F));
   }

   private static void saveAndResetStencilState() {
      stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
      if (stencilWasEnabled) {
         savedStencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
         savedStencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
         savedStencilMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
         GL11.glDisable(GL11.GL_STENCIL_TEST);
      }

      GL11.glClearStencil(0);
      GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
   }

   private static void saveRenderState() {
      depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
   }

   private static void restoreRenderState() {
      RenderSystem.colorMask(true, true, true, true);
      if (depthWasEnabled) {
         GL11.glEnable(GL11.GL_DEPTH_TEST);
      } else {
         GL11.glDisable(GL11.GL_DEPTH_TEST);
      }

      if (blendWasEnabled) {
         GL11.glEnable(GL11.GL_BLEND);
         RenderSystem.defaultBlendFunc();
      } else {
         GL11.glDisable(GL11.GL_BLEND);
      }

      GL.resetTextureSlot();
   }

   private static void restoreStencilState() {
      if (stencilWasEnabled) {
         GL11.glEnable(GL11.GL_STENCIL_TEST);
         GL11.glStencilFunc(savedStencilFunc, savedStencilRef, savedStencilMask);
      }
   }

   private static void cleanup() {
      if (mainRenderBuffer != null) {
         mainRenderBuffer.dispose();
      }
      if (blurBuffer != null) {
         blurBuffer.dispose();
      }
      mainRenderBuffer = null;
      blurBuffer = null;
      initialized = false;
      hasRenderedFrame = false;
   }

   public static void reset() {
      cleanup();
      hasFailed = false;
      initialized = false;
      lastWidth = -1;
      lastHeight = -1;
      hasRenderedFrame = false;
   }

   public static boolean isAvailable() {
      return initialized && !hasFailed;
   }
}
