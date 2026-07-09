package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventUpdateFoV;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayerEntity.class})
public abstract class MixinAbstractClientPlayer {
   @Inject(
      method = {"getFovMultiplier"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookFoV(CallbackInfoReturnable<Float> cir) {
      Float returnValue = (Float)cir.getReturnValue();
      EventUpdateFoV event = new EventUpdateFoV(returnValue);
      Naven.getInstance().getEventManager().call(event);
      cir.setReturnValue(event.getFov());
   }
}
