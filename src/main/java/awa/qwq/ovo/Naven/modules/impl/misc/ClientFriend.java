package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.utils.TimeHelper;

@ModuleInfo(
   name = "ClientFriend",
   description = "Treat other users as friend!",
   category = Category.MISC
)
public class ClientFriend extends Module {
   public static TimeHelper attackTimer = new TimeHelper();

   @Override
   public void onDisable() {
      attackTimer.reset();
      Notification notification = new Notification(NotificationLevel.INFO, "You can attack other players after 15 seconds.", 15000L);
      Naven.getInstance().getNotificationManager().addNotification(notification);
   }
}
