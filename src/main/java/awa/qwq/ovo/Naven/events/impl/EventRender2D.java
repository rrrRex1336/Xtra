package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class EventRender2D implements Event {
   private final MatrixStack stack;
   private final DrawContext guiGraphics;
   private final float partialTicks;
   private float RenderPartialTicks;

   public EventRender2D(MatrixStack stack, DrawContext guiGraphics, float partialTicks) {
      this.stack = stack;
      this.guiGraphics = guiGraphics;
      this.partialTicks = partialTicks;
   }

   public EventRender2D(MatrixStack stack, DrawContext guiGraphics) {
      this(stack, guiGraphics, 0.0f);
   }

   public MatrixStack getStack() {
      return this.stack;
   }

   public DrawContext getGuiGraphics() {
      return this.guiGraphics;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public float getRenderPartialTicks() {
      return this.RenderPartialTicks;
   }
}