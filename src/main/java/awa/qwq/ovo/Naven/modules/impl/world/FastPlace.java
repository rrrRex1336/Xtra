package awa.qwq.ovo.Naven.modules.impl.world;

import org.mixin.accessors.MinecraftAccessor;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.item.BlockItem;

@ModuleInfo(
   name = "FastPlace",
   description = "Place blocks faster",
   category = Category.WORLD
)
public class FastPlace extends Module {
   private final FloatValue cps = ValueBuilder.create(this, "CPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   private float counter = 0.0F;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         if (mc.options.useKey.isPressed() && mc.player.getMainHandStack().getItem() instanceof BlockItem) {
            this.counter = this.counter + this.cps.getCurrentValue() / 20.0F;
            if (this.counter >= 1.0F / this.cps.getCurrentValue()) {
               accessor.setRightClickDelay(0);
               this.counter--;
            }
         } else {
            this.counter = 0.0F;
         }
      }
   }
}
