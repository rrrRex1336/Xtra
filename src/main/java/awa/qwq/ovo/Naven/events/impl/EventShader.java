package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.math.BigInteger;

@Getter
public class EventShader implements Event {
   public static Object trash = new BigInteger("fffffffffffffffffffffffffffffffaaffffffffffffffafffaffff09ffcfff", 16);
   private final MatrixStack stack;
   private final EventType type;
   private final DrawContext graphics;

    public EventShader(MatrixStack stack, DrawContext graphics, EventType type) {
      this.stack = stack;
      this.graphics = graphics;
      this.type = type;
   }
}
