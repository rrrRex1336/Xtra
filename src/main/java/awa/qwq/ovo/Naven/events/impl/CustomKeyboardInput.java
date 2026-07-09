package awa.qwq.ovo.Naven.events.impl;

import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.GameOptions;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)

public class CustomKeyboardInput extends Input {
   private final GameOptions options;
   private boolean cancel;

   public CustomKeyboardInput(GameOptions p_108580_) {
      this.options = p_108580_;
      this.cancel = false;
   }

   public void tick(boolean p_108582_) {
      this.pressingForward = this.options.forwardKey.isPressed();
      this.pressingBack = this.options.backKey.isPressed();
      this.pressingLeft = this.options.leftKey.isPressed();
      this.pressingRight = this.options.rightKey.isPressed();
      this.movementForward = this.pressingForward == this.pressingBack ? 0.0F : (this.pressingForward ? 1.0F : -1.0F);
      this.movementSideways = this.pressingLeft == this.pressingRight ? 0.0F : (this.pressingLeft ? 1.0F : -1.0F);
      this.jumping = this.options.jumpKey.isPressed();
      this.sneaking = this.options.sneakKey.isPressed();
      if (p_108582_) {
         this.movementSideways = (float)((double)this.movementSideways * 0.3);
         this.movementForward = (float)((double)this.movementForward * 0.3);
      }

      if (this.cancel) {
         super.movementSideways *= 5.0F;
         super.movementForward *= 5.0F;
      }
   }

   public void setCancel(boolean cancel) {
      this.cancel = cancel;
   }
}