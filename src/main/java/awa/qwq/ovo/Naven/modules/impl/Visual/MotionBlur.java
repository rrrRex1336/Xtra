package awa.qwq.ovo.Naven.modules.impl.Visual;

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
import com.mojang.blaze3d.shaders.Uniform;
import java.io.IOException;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

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
   private final ResourceLocation shaderLocation = new ResourceLocation("shaders/post/motion_blur.json");
   public PostChain shader;
   private int lastWidth;
   private float currentBlur;
   private int lastHeight;

   public MotionBlur() {
      instance = this;
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.POST) {
         if (mc.player != null && mc.level != null && mc.player.tickCount > 10) {
            if ((this.shader == null || mc.getWindow().getWidth() != this.lastWidth || mc.getWindow().getHeight() != this.lastHeight)
               && mc.getWindow().getWidth() > 0
               && mc.getWindow().getHeight() > 0) {
               this.currentBlur = Float.NaN;

               try {
                  this.shader = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), this.shaderLocation);
                  this.shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
               } catch (IOException | JsonSyntaxException var3) {
                  var3.printStackTrace();
               }
            }

            float blur = 1.0F - Math.min(this.strength.getCurrentValue() / 10.0F, 0.9F);
            if (this.currentBlur != blur && this.shader != null) {
               ((PostChainAccessor)this.shader).getPasses().forEach(shader -> {
                  Uniform blendFactor = shader.getEffect().getUniform("BlurFactor");
                  if (blendFactor != null) {
                     blendFactor.set(blur, 0.0F, 0.0F);
                  }
               });
               this.currentBlur = blur;
            }

            this.lastWidth = mc.getWindow().getWidth();
            this.lastHeight = mc.getWindow().getHeight();
         }
      }
   }
}
