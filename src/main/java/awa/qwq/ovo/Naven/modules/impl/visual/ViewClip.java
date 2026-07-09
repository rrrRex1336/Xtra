package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.option.Perspective;

@ModuleInfo(
   name = "ViewClip",
   description = "Allows you to see through blocks",
   category = Category.VISUAL
)
public class ViewClip extends Module {
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setMinFloatValue(0.5F)
      .setMaxFloatValue(2.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
   public BooleanValue animation = ValueBuilder.create(this, "Animation").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue animationSpeed = ValueBuilder.create(this, "Animation Speed")
      .setMinFloatValue(0.01F)
      .setMaxFloatValue(0.5F)
      .setDefaultFloatValue(0.3F)
      .setFloatStep(0.01F)
      .setVisibility(() -> this.animation.getCurrentValue())
      .build()
      .getFloatValue();

   public BooleanValue fixSkipTickUpdateAnimation = ValueBuilder.create(this, "Fix Skip Tick Update Animation")
           .setDefaultBooleanValue(false).build().getBooleanValue();
   public SmoothAnimationTimer personViewAnimation = new SmoothAnimationTimer(100.0F);
   Perspective lastPersonView;

   @EventTarget
   public void onRender(EventRender2D e) {
      if (this.lastPersonView != mc.options.getPerspective()) {
         this.lastPersonView = mc.options.getPerspective();
         if (this.lastPersonView == Perspective.FIRST_PERSON || this.lastPersonView == Perspective.THIRD_PERSON_BACK) {
            this.personViewAnimation.value = 0.0F;
         }
      }

      this.personViewAnimation.speed = this.animationSpeed.getCurrentValue();
      this.personViewAnimation.update(true);
   }
}
