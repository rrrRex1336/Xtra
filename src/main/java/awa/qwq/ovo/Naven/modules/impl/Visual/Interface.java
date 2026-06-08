package awa.qwq.ovo.Naven.modules.impl.Visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;

@ModuleInfo(
        name = "Interface",
        description = "Displays information on your screen",
        category = Category.VISUAL
)
public class Interface extends Module {

   public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound")
           .setDefaultBooleanValue(true)
           .build().getBooleanValue();

   public BooleanValue notification = ValueBuilder.create(this, "Notification")
           .setDefaultBooleanValue(true)
           .build().getBooleanValue();

   public BooleanValue chatScreen = ValueBuilder.create(this, "Chat Screen")
           .setDefaultBooleanValue(false)
           .build().getBooleanValue();

   @EventTarget
   public void notification(EventRender2D e) {
      if (this.notification.getCurrentValue()) {
         Naven.getInstance().getNotificationManager().onRender(e);
      }
   }
}