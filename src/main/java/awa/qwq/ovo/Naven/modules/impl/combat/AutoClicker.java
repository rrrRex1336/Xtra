package awa.qwq.ovo.Naven.modules.impl.combat;

import org.mixin.accessors.MinecraftAccessor;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(
   name = "AutoClicker",
   description = "Automatically clicks for you",
   category = Category.COMBAT
)
public class AutoClicker extends Module {
   private final FloatValue cps = ValueBuilder.create(this, "CPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   private final BooleanValue itemCheck = ValueBuilder.create(this, "Item Check").setDefaultBooleanValue(true).build().getBooleanValue();
   private float counter = 0.0F;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         Item item = mc.player.getMainHandStack().getItem();
         if (mc.options.attackKey.isPressed()
            && (item instanceof SwordItem || item instanceof AxeItem || !this.itemCheck.getCurrentValue())
            && mc.crosshairTarget.getType() != Type.BLOCK) {
            this.counter = this.counter + this.cps.getCurrentValue() / 20.0F;
            if (this.counter >= 1.0F / this.cps.getCurrentValue()) {
               accessor.setMissTime(0);
               KeyBinding.onKeyPressed(mc.options.attackKey.getDefaultKey());
               this.counter--;
            }
         } else {
            this.counter = 0.0F;
         }
      }
   }
}
