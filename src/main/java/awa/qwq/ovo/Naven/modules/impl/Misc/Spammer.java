package awa.qwq.ovo.Naven.modules.impl.Misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
   name = "Spammer",
   description = "Spam chat!",
   category = Category.MISC
)
public class Spammer extends Module {
   Random random = new Random();
   FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(6000.0F)
      .setFloatStep(100.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(15000.0F)
      .build()
      .getFloatValue();
   ModeValue prefix = ValueBuilder.create(this, "Prefix").setDefaultModeIndex(0).setModes("None", "@", "/shout ").build().getModeValue();
   private final List<BooleanValue> values = new ArrayList<>();
   private final TimeHelper timer = new TimeHelper();

   @EventTarget
   public void onMotion(EventRunTicks e) {
      if (e.getType() == EventType.POST && this.timer.delay((double)this.delay.getCurrentValue())) {
         String prefix = this.prefix.isCurrentMode("None") ? "" : this.prefix.getCurrentMode();
         List<String> styles = this.values.stream().filter(BooleanValue::getCurrentValue).map(Value::getName).toList();
         if (styles.isEmpty()) {
            return;
         }

         String style = styles.get(this.random.nextInt(styles.size()));
         String message = prefix + style;
         mc.player.connection.sendChat(message);
         this.timer.reset();
      }
   }

   public List<BooleanValue> getValues() {
      return this.values;
   }
}
