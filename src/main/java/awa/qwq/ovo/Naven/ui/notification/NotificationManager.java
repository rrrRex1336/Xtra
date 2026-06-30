package awa.qwq.ovo.Naven.ui.notification;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

   @EventTarget
   public void onRenderShadow(EventShader e) {
      this.onRenderShadow(e, null);
   }

   public void onRenderShadow(EventShader e, DragManager dragManager) {
      float[] offsets = this.getOffsets(dragManager);
      for (Notification notification : this.notifications) {
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         Window window = Minecraft.getInstance().getWindow();
         notification.renderShader(
            e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F + offsets[0], (float)window.getGuiScaledHeight() - heightTimer.value + offsets[1]
         );
      }
   }

   public void onRender(EventRender2D e) {
      this.onRender(e, null);
   }

   public void onRender(EventRender2D e, DragManager dragManager) {
      float height = 5.0F;
      float maxWidth = 0.0F;
      float totalHeight = 5.0F;

      for (Notification notification : this.notifications) {
         maxWidth = Math.max(maxWidth, notification.getWidth());
         totalHeight += notification.getHeight();
      }

      Window window = Minecraft.getInstance().getWindow();
      float baseX = (float) window.getGuiScaledWidth() - maxWidth + 2.0F;
      float baseY = (float) window.getGuiScaledHeight() - totalHeight;
      if (dragManager != null) {
         dragManager.update(baseX, baseY, maxWidth, totalHeight);
      }
      float xOffset = dragManager == null ? 0.0F : dragManager.getX(baseX) - baseX;
      float yOffset = dragManager == null ? 0.0F : dragManager.getY(baseY) - baseY;

      for (Notification notification : this.notifications) {
         e.getStack().pushPose();
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         notification.render(e.getStack(), (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F + xOffset, (float)window.getGuiScaledHeight() - heightTimer.value + yOffset);
         e.getStack().popPose();
      }
   }

   private float[] getOffsets(DragManager dragManager) {
      if (dragManager == null || this.notifications.isEmpty()) {
         return new float[]{0.0F, 0.0F};
      }

      float maxWidth = 0.0F;
      float totalHeight = 5.0F;
      for (Notification notification : this.notifications) {
         maxWidth = Math.max(maxWidth, notification.getWidth());
         totalHeight += notification.getHeight();
      }

      Window window = Minecraft.getInstance().getWindow();
      float baseX = (float) window.getGuiScaledWidth() - maxWidth + 2.0F;
      float baseY = (float) window.getGuiScaledHeight() - totalHeight;
      return new float[]{dragManager.getX(baseX) - baseX, dragManager.getY(baseY) - baseY};
   }
}
