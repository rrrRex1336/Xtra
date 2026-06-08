package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

import java.math.BigInteger;

@Getter
public class EventShader implements Event {
   public static Object trash = new BigInteger("fffffffffffffffffffffffffffffffaaffffffffffffffafffaffff09ffcfff", 16);
   private final PoseStack stack;
   private final EventType type;
   private final GuiGraphics graphics;

    public EventShader(PoseStack stack, GuiGraphics graphics, EventType type) {
      this.stack = stack;
      this.graphics = graphics;
      this.type = type;
   }
}
