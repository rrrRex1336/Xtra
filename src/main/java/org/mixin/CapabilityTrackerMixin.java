package org.mixin;

import awa.qwq.ovo.Naven.utils.ICapabilityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
   targets = {"com.mojang.blaze3d.platform.GlStateManager$CapabilityTracker"},
   remap = false
)
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
   @Shadow(remap = false)
   private boolean state;

   @Shadow(remap = false)
   public abstract void setState(boolean var1);

   @Override
   public boolean get() {
      return this.state;
   }

   @Override
   public void set(boolean state) {
      this.setState(state);
   }
}
