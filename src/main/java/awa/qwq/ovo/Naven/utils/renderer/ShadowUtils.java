package awa.qwq.ovo.Naven.utils.renderer;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ShadowUtils {
   private static final TimeHelper shadowTimer = new TimeHelper();
   private static Framebuffer mainRenderBuffer;
   private static Framebuffer blurBuffer;
   private static Shader blurShader;

   // 错误处理和降级模式
   private static boolean initialized = false;
   private static boolean hasFailed = false;
   private static int lastWidth = -1;
   private static int lastHeight = -1;

   // 性能优化：缓存上一帧的结果
   private static boolean useCache = true;

   /**
    * 主渲染方法 - 修复模板缓冲区冲突版本
    * @param e 渲染事件
    * @param fps 目标 FPS
    * @param strength Bloom 强度 (1.0-10.0)
    */
   public static void onRenderAfterWorld(EventRender2D e, float fps, float strength) {
      // 如果之前初始化失败，直接返回
      if (hasFailed) {
         return;
      }

      try {
         Window window = Minecraft.getInstance().getWindow();
         int width = window.getWidth();
         int height = window.getHeight();

         // 初始化或重新初始化（如果窗口大小改变）
         if (!initialized || needsResize(width, height)) {
            cleanup();
            if (!initialize()) {
               return;
            }
            lastWidth = width;
            lastHeight = height;
         }

         // 检查是否需要刷新渲染
         boolean shouldRefresh = false;
         if (shadowTimer.delay((double)(1000.0F / fps))) {
            shouldRefresh = true;
            shadowTimer.reset();
         }

         // ===== 关键修复：在渲染前清理模板缓冲区状态 =====
         saveAndResetStencilState();

         // 渲染新内容到缓冲区
         if (shouldRefresh) {
            renderToBuffer(e, window);
         }

         // 应用模糊效果并输出到屏幕
         if (shouldRefresh || !useCache) {
            applyBlurEffect(e, window, strength);
         } else {
            // 使用缓存的结果直接渲染
            renderCachedResult(e, window, strength);
         }

         // ===== 恢复模板缓冲区状态 =====
         restoreStencilState();

      } catch (Exception ex) {
         System.err.println("[ShadowUtils] 渲染时发生错误: " + ex.getMessage());
         ex.printStackTrace();
         hasFailed = true;
         cleanup();
      }
   }

   // 保存的模板状态
   private static boolean stencilWasEnabled = false;
   private static int savedStencilFunc = GL11.GL_ALWAYS;
   private static int savedStencilRef = 0;
   private static int savedStencilMask = 0xFFFFFFFF;

   /**
    * 保存并重置模板状态（模拟聊天栏的清理效果）
    */
   private static void saveAndResetStencilState() {
      try {
         // 保存当前模板状态
         stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

         if (stencilWasEnabled) {
            // 保存模板参数
            savedStencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
            savedStencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
            savedStencilMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);

            // 关键：临时禁用模板测试，相当于"打开聊天栏"的效果
            GL11.glDisable(GL11.GL_STENCIL_TEST);
         }

         // 清理模板缓冲区中的脏数据（可选，聊胜于无）
         // 这样能确保模板缓冲区是干净的
         GL11.glClearStencil(0);
         GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

      } catch (Exception e) {
         // 忽略异常，降级处理
      }
   }

   /**
    * 恢复模板状态
    */
   private static void restoreStencilState() {
      try {
         if (stencilWasEnabled) {
            // 恢复模板参数
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glStencilFunc(savedStencilFunc, savedStencilRef, savedStencilMask);
         }
      } catch (Exception e) {
         // 忽略异常
      }
   }

   /**
    * 初始化渲染资源
    */
   private static boolean initialize() {
      try {
         // 清除之前的 OpenGL 错误
         GL11.glGetError();

         // 创建着色器
         blurShader = new Shader("shadow.vert", "shadow.frag");

         // 创建帧缓冲（只需要两个，而不是三个）
         mainRenderBuffer = new Framebuffer();
         blurBuffer = new Framebuffer();

         // 检查是否有 OpenGL 错误
         int error = GL11.glGetError();
         if (error != GL11.GL_NO_ERROR) {
            System.err.println("[ShadowUtils] 初始化时发生 OpenGL 错误: " + error);
            cleanup();
            hasFailed = true;
            return false;
         }

         initialized = true;
         return true;

      } catch (Exception e) {
         System.err.println("[ShadowUtils] 初始化失败: " + e.getMessage());
         e.printStackTrace();
         cleanup();
         hasFailed = true;
         return false;
      }
   }

   /**
    * 检查是否需要调整大小
    */
   private static boolean needsResize(int width, int height) {
      return width != lastWidth || height != lastHeight;
   }

   /**
    * 渲染内容到主缓冲区
    */
   private static void renderToBuffer(EventRender2D e, Window window) {
      try {
         // 绑定主渲染缓冲区
         mainRenderBuffer.bind();

         // 清除缓冲区
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

         // 设置着色器
         RenderSystem.setShader(GameRenderer::getPositionColorShader);

         // 调用事件绘制阴影内容
         Naven.getInstance().getEventManager().call(
                 new EventShader(e.getStack(), e.getGuiGraphics(), EventType.SHADOW)
         );

         // 解绑
         mainRenderBuffer.unbind();

      } catch (Exception ex) {
         System.err.println("[ShadowUtils] 渲染到缓冲区时出错: " + ex.getMessage());
         throw ex;
      }
   }

   /**
    * 应用模糊效果（优化的双通道模糊）
    * @param strength Bloom 强度，用于控制扩散和亮度 (1.0-10.0)
    */
   private static void applyBlurEffect(EventRender2D e, Window window, float strength) {
      try {
         GL.enableBlend();
         GL11.glDisable(GL11.GL_DEPTH_TEST);  // 禁用深度测试避免冲突

         blurShader.bind();
         blurShader.set("u_Size", (double)window.getWidth(), (double)window.getHeight());

         // 优化的强度映射
         // strength 1.0-10.0 -> 更合理的半径和强度范围
         float blurRadius = 2.0f + (strength * 1.2f); // 半径范围: 3.2 - 14.0
         float intensity = 0.5f + (strength * 0.15f);  // 强度范围: 0.65 - 2.0

         blurShader.set("u_Radius", (double)blurRadius);
         blurShader.set("u_Intensity", (double)intensity);

         PostProcessRenderer.beginRender(e.getStack());

         // 第一次模糊：水平方向（mainRenderBuffer -> blurBuffer）
         blurBuffer.bind();
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
         GL.bindTexture(mainRenderBuffer.texture);
         blurShader.set("u_Direction", 1.0, 0.0);
         PostProcessRenderer.render(e.getStack());

         // 第二次模糊：垂直方向（blurBuffer -> 屏幕）
         blurBuffer.unbind();
         GL.bindTexture(blurBuffer.texture);
         blurShader.set("u_Direction", 0.0, 1.0);
         PostProcessRenderer.render(e.getStack());

         PostProcessRenderer.endRender();
         GL.disableBlend();
         GL11.glEnable(GL11.GL_DEPTH_TEST);  // 恢复深度测试

      } catch (Exception ex) {
         System.err.println("[ShadowUtils] 应用模糊效果时出错: " + ex.getMessage());
         throw ex;
      }
   }

   /**
    * 渲染缓存的结果
    * @param strength Bloom 强度参数
    */
   private static void renderCachedResult(EventRender2D e, Window window, float strength) {
      try {
         GL.enableBlend();
         GL11.glDisable(GL11.GL_DEPTH_TEST);

         blurShader.bind();
         blurShader.set("u_Size", (double)window.getWidth(), (double)window.getHeight());

         // 使用相同的强度映射
         float blurRadius = 2.0f + (strength * 1.2f);
         float intensity = 0.5f + (strength * 0.15f);
         blurShader.set("u_Radius", (double)blurRadius);
         blurShader.set("u_Intensity", (double)intensity);

         PostProcessRenderer.beginRender(e.getStack());

         // 直接使用缓存的模糊结果
         GL.bindTexture(blurBuffer.texture);
         blurShader.set("u_Direction", 0.0, 1.0);
         PostProcessRenderer.render(e.getStack());

         PostProcessRenderer.endRender();
         GL.disableBlend();
         GL11.glEnable(GL11.GL_DEPTH_TEST);

      } catch (Exception ex) {
         System.err.println("[ShadowUtils] 渲染缓存结果时出错: " + ex.getMessage());
         throw ex;
      }
   }

   /**
    * 清理资源
    */
   private static void cleanup() {
      try {
         if (mainRenderBuffer != null) {
            mainRenderBuffer.dispose();
         }
         if (blurBuffer != null) {
            blurBuffer.dispose();
         }
         mainRenderBuffer = null;
         blurBuffer = null;
         initialized = false;
      } catch (Exception e) {
         System.err.println("[ShadowUtils] 清理资源时出错: " + e.getMessage());
      }
   }

   /**
    * 重置状态（用于外部调用以恢复功能）
    */
   public static void reset() {
      cleanup();
      hasFailed = false;
      initialized = false;
      lastWidth = -1;
      lastHeight = -1;
   }

   /**
    * 检查是否可用
    */
   public static boolean isAvailable() {
      return initialized && !hasFailed;
   }
}