package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RenderTickCounter.class})
public class MixinTimer {
   @Shadow
   public float tickDelta;
   @Shadow
   private long prevTimeMillis;
   @Final
   @Shadow
   private float tickTime;
   @Shadow
   public float lastFrameDuration;

   @Inject(
           method = {"beginRenderTick"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      if (Naven.TICK_TIMER != 1.0F) {
         float timerMultiplier = Naven.TICK_TIMER;

         this.lastFrameDuration = (float)(timeMillis - this.prevTimeMillis) / this.tickTime * timerMultiplier;
         this.prevTimeMillis = timeMillis;
         this.tickDelta = this.tickDelta + this.lastFrameDuration;
         int i = (int)this.tickDelta;
         this.tickDelta -= (float)i;
         cir.setReturnValue(i);
      }
   }
}
