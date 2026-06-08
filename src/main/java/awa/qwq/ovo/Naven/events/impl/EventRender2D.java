package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class EventRender2D implements Event {
   private final PoseStack stack;
   private final GuiGraphics guiGraphics;
   private final float partialTicks;
   private float RenderPartialTicks;

   public EventRender2D(PoseStack stack, GuiGraphics guiGraphics, float partialTicks) {
      this.stack = stack;
      this.guiGraphics = guiGraphics;
      this.partialTicks = partialTicks;
   }

   public EventRender2D(PoseStack stack, GuiGraphics guiGraphics) {
      this(stack, guiGraphics, 0.0f);
   }

   public PoseStack getStack() {
      return this.stack;
   }

   public GuiGraphics getGuiGraphics() {
      return this.guiGraphics;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public float getRenderPartialTicks() {
      return this.RenderPartialTicks;
   }
}