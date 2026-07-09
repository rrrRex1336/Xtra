package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@ModuleInfo(
   name = "TimeChanger",
   description = "Change the time of the world",
   category = Category.VISUAL
)
public class TimeChanger extends Module {
   FloatValue time = ValueBuilder.create(this, "World Time")
      .setDefaultFloatValue(8000.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(24000.0F)
      .build()
      .getFloatValue();

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.world.setTimeOfDay((long)this.time.getCurrentValue());
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         event.setCancelled(true);
      }
   }
}
