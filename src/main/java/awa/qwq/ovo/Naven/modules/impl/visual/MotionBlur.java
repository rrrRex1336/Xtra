package awa.qwq.ovo.Naven.modules.impl.visual;

import com.google.gson.JsonSyntaxException;
import org.mixin.accessors.PostChainAccessor;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.io.IOException;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;

@ModuleInfo(
   name = "MotionBlur",
   description = "Make your game smoother.",
   category = Category.VISUAL
)
public class MotionBlur extends Module {
   public static MotionBlur instance;
   private final FloatValue strength = ValueBuilder.create(this, "Strength")
      .setFloatStep(0.1F)
      .setDefaultFloatValue(7.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   @SuppressWarnings("removal")
   private final Identifier shaderLocation = new Identifier("shaders/post/motion_blur.json");
   public PostEffectProcessor shader;
   private int lastWidth;
   private float currentBlur;
   private int lastHeight;

   public MotionBlur() {
      instance = this;
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.POST) {
         if (mc.player != null && mc.world != null && mc.player.age > 10) {
            if ((this.shader == null || mc.getWindow().getFramebufferWidth() != this.lastWidth || mc.getWindow().getFramebufferHeight() != this.lastHeight)
               && mc.getWindow().getFramebufferWidth() > 0
               && mc.getWindow().getFramebufferHeight() > 0) {
               this.currentBlur = Float.NaN;

               try {
                  this.shader = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), this.shaderLocation);
                  this.shader.setupDimensions(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
               } catch (IOException | JsonSyntaxException var3) {
                  var3.printStackTrace();
               }
            }

            float blur = 1.0F - Math.min(this.strength.getCurrentValue() / 10.0F, 0.9F);
            if (this.currentBlur != blur && this.shader != null) {
               ((PostChainAccessor)this.shader).getPasses().forEach(shader -> {
                  GlUniform blendFactor = shader.getProgram().getUniformByName("BlurFactor");
                  if (blendFactor != null) {
                     blendFactor.set(blur, 0.0F, 0.0F);
                  }
               });
               this.currentBlur = blur;
            }

            this.lastWidth = mc.getWindow().getFramebufferWidth();
            this.lastHeight = mc.getWindow().getFramebufferHeight();
         }
      }
   }
}
