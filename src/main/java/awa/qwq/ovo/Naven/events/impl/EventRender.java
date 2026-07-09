package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.client.util.math.MatrixStack;

public class EventRender implements Event {
   private final float renderPartialTicks;
   private final MatrixStack pMatrixStack;

   public float getRenderPartialTicks() {
      return this.renderPartialTicks;
   }

   public MatrixStack getPMatrixStack() {
      return this.pMatrixStack;
   }

   public EventRender(float renderPartialTicks, MatrixStack pMatrixStack) {
      this.renderPartialTicks = renderPartialTicks;
      this.pMatrixStack = pMatrixStack;
   }
}
