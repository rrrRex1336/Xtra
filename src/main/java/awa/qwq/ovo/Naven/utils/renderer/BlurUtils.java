package awa.qwq.ovo.Naven.utils.renderer;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntDoubleImmutablePair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class BlurUtils {
   private static Shader shaderDown;
   private static Shader shaderUp;
   private static final TimeHelper blurTimer = new TimeHelper();
   private static final Framebuffer[] fbos = new Framebuffer[6];
   private static final boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean hasBlurredFrame;
   private static final IntDoubleImmutablePair[] strengths = new IntDoubleImmutablePair[]{
      IntDoubleImmutablePair.of(1, 1.25),
      IntDoubleImmutablePair.of(1, 2.25),
      IntDoubleImmutablePair.of(2, 2.0),
      IntDoubleImmutablePair.of(2, 3.0),
      IntDoubleImmutablePair.of(2, 4.25),
      IntDoubleImmutablePair.of(3, 2.5),
      IntDoubleImmutablePair.of(3, 3.25),
      IntDoubleImmutablePair.of(3, 4.25),
      IntDoubleImmutablePair.of(3, 5.5),
      IntDoubleImmutablePair.of(4, 3.25),
      IntDoubleImmutablePair.of(4, 4.0),
      IntDoubleImmutablePair.of(4, 5.0),
      IntDoubleImmutablePair.of(4, 6.0),
      IntDoubleImmutablePair.of(4, 7.25),
      IntDoubleImmutablePair.of(4, 8.25),
      IntDoubleImmutablePair.of(5, 4.5),
      IntDoubleImmutablePair.of(5, 5.25),
      IntDoubleImmutablePair.of(5, 6.25),
      IntDoubleImmutablePair.of(5, 7.25),
      IntDoubleImmutablePair.of(5, 8.5)
   };

   public static void onRenderAfterWorld(EventRender2D e, float fps, int strengthIndex) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int width = mc.getWindow().getFramebufferWidth();
      int height = mc.getWindow().getFramebufferHeight();

      try {
         StencilUtils.write(false);
         Naven.getInstance().getEventManager().call(new EventShader(e.getStack(), e.getGuiGraphics(), EventType.BLUR));
         StencilUtils.erase(true);
         if (shaderDown == null) {
            shaderDown = new Shader("blur.vert", "blur_down.frag");
            shaderUp = new Shader("blur.vert", "blur_up.frag");
         }

         boolean resized = width != lastWidth || height != lastHeight;
         for (int i = 0; i < fbos.length; i++) {
            if (fbos[i] == null) {
               fbos[i] = new Framebuffer(1.0 / Math.pow(2.0, (double)i));
               resized = true;
            } else if (resized) {
               fbos[i].resize();
            }
         }
         if (resized) {
            lastWidth = width;
            lastHeight = height;
            hasBlurredFrame = false;
         }

         IntDoubleImmutablePair strength = strengths[Math.max(0, Math.min(strengthIndex, strengths.length - 1))];
         int iterations = strength.leftInt();
         double offset = strength.rightDouble();

         if (shouldRefresh(fps)) {
            PostProcessRenderer.beginRender(e.getStack());
            renderToFbo(e.getStack(), fbos[0], mc.getFramebuffer().getColorAttachment(), shaderDown, offset);

            for (int ix = 0; ix < iterations; ix++) {
               renderToFbo(e.getStack(), fbos[ix + 1], fbos[ix].texture, shaderDown, offset);
            }

            for (int ix = iterations; ix >= 1; ix--) {
               renderToFbo(e.getStack(), fbos[ix - 1], fbos[ix].texture, shaderUp, offset);
            }

            blurTimer.reset();
            PostProcessRenderer.endRender();
            hasBlurredFrame = true;
         }

         mc.getFramebuffer().beginWrite(false);
         GL.viewport(0, 0, width, height);
         shaderUp.bind();
         GL.bindTexture(fbos[0].texture);
         shaderUp.set("uTexture", 0);
         shaderUp.set("uHalfTexelSize", 0.5 / (double)fbos[0].width, 0.5 / (double)fbos[0].height);
         shaderUp.set("uOffset", 0.0);
         PostProcessRenderer.beginRender(e.getStack());
         PostProcessRenderer.render(e.getStack());
         PostProcessRenderer.endRender();
      } finally {
         RenderSystem.colorMask(true, true, true, true);
         StencilUtils.dispose();
         GL.resetTextureSlot();
      }
   }

   private static boolean shouldRefresh(float fps) {
      float safeFps = Math.max(1.0F, fps);
      return sodiumLoaded || !hasBlurredFrame || blurTimer.delay((double)(1000.0F / safeFps));
   }

   private static void renderToFbo(MatrixStack stack, Framebuffer targetFbo, int sourceText, Shader shader, double offset) {
      targetFbo.bind();
      targetFbo.setViewport();
      boolean stencilEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
      GL11.glDisable(GL11.GL_STENCIL_TEST);
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
      shader.bind();
      GL.bindTexture(sourceText);
      shader.set("uTexture", 0);
      shader.set("uHalfTexelSize", 0.5 / (double)targetFbo.width, 0.5 / (double)targetFbo.height);
      shader.set("uOffset", offset);
      PostProcessRenderer.render(stack);
      if (stencilEnabled) {
         GL11.glEnable(GL11.GL_STENCIL_TEST);
      }
   }
}
