package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "IRCChat", description = "Displays IRC messages in a separate HUD.", category = Category.VISUAL)
public class IrcChatHUD extends Module {
   private static final int MAX_MESSAGES = 300;
   private static final CopyOnWriteArrayList<String> MESSAGES = new CopyOnWriteArrayList<>();
   private static int scrollOffset;

   private final FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
   private final FloatValue yOffset = DragManager.createHiddenPositionValue(this, "Drag Y", 0.0F);
   private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);

   public final FloatValue width = ValueBuilder.create(this, "Width")
           .setDefaultFloatValue(260.0F)
           .setMinFloatValue(120.0F)
           .setMaxFloatValue(640.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   public final FloatValue height = ValueBuilder.create(this, "Height")
           .setDefaultFloatValue(120.0F)
           .setMinFloatValue(40.0F)
           .setMaxFloatValue(360.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   public final BooleanValue shadow = ValueBuilder.create(this, "Shadow")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public static boolean routeMessage(String text) {
      if (text == null || text.isBlank()) {
         return false;
      }

      IrcChatHUD module = module();
      if (module == null || !module.isEnabled()) {
         return false;
      }

      MESSAGES.add(text);
      while (MESSAGES.size() > MAX_MESSAGES) {
         MESSAGES.remove(0);
      }
      scrollOffset = 0;
      return true;
   }

   public static void scrollBy(double amount) {
      if (amount == 0.0D || MESSAGES.isEmpty()) {
         return;
      }
      scrollOffset += amount > 0.0D ? 1 : -1;
      if (scrollOffset < 0) {
         scrollOffset = 0;
      }
      if (scrollOffset > MESSAGES.size() - 1) {
         scrollOffset = MESSAGES.size() - 1;
      }
   }

   @EventTarget
   public void onRender(EventRender2D event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.font == null) {
         return;
      }

      GuiGraphics graphics = event.getGuiGraphics();
      float baseX = 8.0F;
      float baseY = graphics.guiHeight() - this.height.getCurrentValue() - 48.0F;
      float w = this.width.getCurrentValue();
      float h = this.height.getCurrentValue();
      this.dragManager.update(baseX, baseY, w, h);
      float x = this.dragManager.getX(baseX);
      float y = this.dragManager.getY(baseY);

      graphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0x88000000);
      graphics.fill((int) x, (int) y, (int) (x + w), (int) (y + 12.0F), 0xAA1B1B1B);
      graphics.drawString(mc.font, "IRC Chat", (int) x + 4, (int) y + 3, 0xFF55FFFF, this.shadow.getCurrentValue());

      List<String> lines = wrappedLines(mc.font, (int) (w - 8.0F));
      int maxLines = Math.max(1, ((int) h - 18) / 10);
      int end = Math.max(0, lines.size() - scrollOffset);
      int start = Math.max(0, end - maxLines);
      int drawY = (int) y + 16;
      for (int i = start; i < end; i++) {
         graphics.drawString(mc.font, lines.get(i), (int) x + 4, drawY, 0xFFFFFFFF, this.shadow.getCurrentValue());
         drawY += 10;
      }

      if (lines.size() > maxLines) {
         float ratio = maxLines / (float) lines.size();
         int barHeight = Math.max(14, (int) ((h - 18.0F) * ratio));
         int barTravel = Math.max(1, (int) (h - 18.0F - barHeight));
         int barY = (int) y + 16 + (int) (barTravel * (1.0F - (end / (float) lines.size())));
         graphics.fill((int) (x + w - 3), barY, (int) (x + w - 1), barY + barHeight, 0xAA55FFFF);
      }
   }

   private static List<String> wrappedLines(Font font, int width) {
      List<String> result = new ArrayList<>();
      for (String message : MESSAGES) {
         wrap(font, message, width, result);
      }
      return result;
   }

   private static void wrap(Font font, String text, int width, List<String> out) {
      StringBuilder line = new StringBuilder();
      String activeColor = "";
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '\u00a7' && i + 1 < text.length()) {
            activeColor = "\u00a7" + text.charAt(i + 1);
            line.append(c).append(text.charAt(++i));
            continue;
         }

         String next = line.toString() + c;
         if (font.width(next) > width && line.length() > 0) {
            out.add(line.toString());
            line.setLength(0);
            line.append(activeColor).append(c);
         } else {
            line.append(c);
         }
      }
      if (line.length() > 0) {
         out.add(line.toString());
      }
   }

   private static IrcChatHUD module() {
      try {
         if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) {
            return null;
         }
         return (IrcChatHUD) Naven.getInstance().getModuleManager().getModule(IrcChatHUD.class);
      } catch (Exception ignored) {
         return null;
      }
   }
}
