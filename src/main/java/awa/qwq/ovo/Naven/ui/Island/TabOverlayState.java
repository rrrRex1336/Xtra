package awa.qwq.ovo.Naven.ui.Island;

import net.minecraft.network.chat.Component;

public final class TabOverlayState {
   private static volatile Component header;
   private static volatile Component footer;

   private TabOverlayState() {
   }

   public static void setHeader(Component component) {
      header = component;
   }

   public static void setFooter(Component component) {
      footer = component;
   }

   public static Component getHeader() {
      return header;
   }

   public static Component getFooter() {
      return footer;
   }
}
