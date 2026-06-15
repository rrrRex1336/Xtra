package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.renderer.BlurUtils;
import awa.qwq.ovo.Naven.utils.renderer.ShadowUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "PostProcess",
   description = "Post process effects",
   category = Category.VISUAL
)
public class PostProcess extends Module {
   private boolean disableBlur = false;
   private boolean disableBloom = false;
   private final BooleanValue blur = ValueBuilder.create(this, "Blur").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue blurFPS = ValueBuilder.create(this, "Blur FPS")
      .setVisibility(this.blur::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();
   private final FloatValue strength = ValueBuilder.create(this, "Blur Strength")
      .setVisibility(this.blur::getCurrentValue)
      .setDefaultFloatValue(2.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(19.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   private final BooleanValue bloom = ValueBuilder.create(this, "Bloom").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue bloomFPS = ValueBuilder.create(this, "Bloom FPS")
      .setVisibility(this.bloom::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();

   private final BooleanValue glowshader = ValueBuilder.create(this, "Use Glow Shader").setDefaultBooleanValue(true).build().getBooleanValue();

   @EventTarget(0)
   public void onRender(EventRender2D e) {
      if (this.blur.getCurrentValue() && !this.disableBlur) {
         GL11.glGetError();
         BlurUtils.onRenderAfterWorld(e, this.blurFPS.getCurrentValue(), (int)this.strength.getCurrentValue());
         if (GL11.glGetError() != 0) {
            ChatUtils.addChatMessage("Disabling blur due to error.");
            this.disableBlur = true;
         }
      }

      if (this.bloom.getCurrentValue() && !this.disableBloom) {
         if (!ShadowUtils.isAvailable()) {
            GL11.glGetError();
            ShadowUtils.onRenderAfterWorld(e, this.bloomFPS.getCurrentValue(), this.strength.getCurrentValue());
            if (GL11.glGetError() != 0 || !ShadowUtils.isAvailable()) {
               this.disableBloom = true;
            }
         } else {
            ShadowUtils.onRenderAfterWorld(e, this.bloomFPS.getCurrentValue(), this.strength.getCurrentValue());
         }
      }
   }
}
