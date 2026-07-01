package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
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
      this.forwardImpulse = this.up == this.down ? 0.0F : (this.up ? 1.0F : -1.0F);
      this.leftImpulse = this.left == this.right ? 0.0F : (this.left ? 1.0F : -1.0F);
      EventMoveInput eventMoveInput = new EventMoveInput(this.forwardImpulse, this.leftImpulse, this.jumping, this.shiftKeyDown, 0.3);
      Naven.getInstance().getEventManager().call(eventMoveInput);
      double sneakMultiplier = eventMoveInput.getSneakSlowDownMultiplier();
      float eventForward = eventMoveInput.getForward();
      float eventStrafe = eventMoveInput.getStrafe();
      this.forwardImpulse = eventForward;
      this.leftImpulse = eventStrafe;
      this.jumping = eventMoveInput.isJump();
      this.shiftKeyDown = eventMoveInput.isSneak();
      if (pIsMovingSlowly) {
         this.leftImpulse = (float)((double)this.leftImpulse * sneakMultiplier);
         this.forwardImpulse = (float)((double)this.forwardImpulse * sneakMultiplier);
      }

      if (ViaVersionFix.shouldApplyLegacyBlockingSlowdown()) {
         this.leftImpulse = eventStrafe * 0.2F;
         this.forwardImpulse = eventForward * 0.2F;
      }
   }
}
