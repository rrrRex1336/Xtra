package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class MixinKeyboardInput extends Input {
   @Inject(
      at = {@At("TAIL")},
      method = {"tick"}
   )
   private void onTickTail(boolean pIsMovingSlowly, float p_234119_, CallbackInfo ci) {
      this.movementForward = this.pressingForward == this.pressingBack ? 0.0F : (this.pressingForward ? 1.0F : -1.0F);
      this.movementSideways = this.pressingLeft == this.pressingRight ? 0.0F : (this.pressingLeft ? 1.0F : -1.0F);
      EventMoveInput eventMoveInput = new EventMoveInput(this.movementForward, this.movementSideways, this.jumping, this.sneaking, 0.3);
      Naven.getInstance().getEventManager().call(eventMoveInput);
      double sneakMultiplier = eventMoveInput.getSneakSlowDownMultiplier();
      float eventForward = eventMoveInput.getForward();
      float eventStrafe = eventMoveInput.getStrafe();
      this.movementForward = eventForward;
      this.movementSideways = eventStrafe;
      this.jumping = eventMoveInput.isJump();
      this.sneaking = eventMoveInput.isSneak();
      if (pIsMovingSlowly) {
         this.movementSideways = (float)((double)this.movementSideways * sneakMultiplier);
         this.movementForward = (float)((double)this.movementForward * sneakMultiplier);
      }

      if (ViaVersionFix.shouldApplyLegacyBlockingSlowdown()) {
         this.movementSideways = eventStrafe * 0.2F;
         this.movementForward = eventForward * 0.2F;
      }
   }
}
