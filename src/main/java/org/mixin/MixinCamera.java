package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.ViewClip;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class MixinCamera {
   @Inject(
      at = {@At("HEAD")},
      method = {"clipToSpace"},
      cancellable = true
   )
   private void getMaxZoom(double pStartingDistance, CallbackInfoReturnable<Double> cir) {
      if (Naven.getInstance() != null && Naven.getInstance().getModuleManager() != null) {
         ViewClip module = (ViewClip)Naven.getInstance().getModuleManager().getModule(ViewClip.class);
         if (module.isEnabled()) {
            cir.setReturnValue(pStartingDistance * (double)module.scale.getCurrentValue() * (double)module.personViewAnimation.value / 100.0);
            cir.cancel();
         }
      }
   }
}
