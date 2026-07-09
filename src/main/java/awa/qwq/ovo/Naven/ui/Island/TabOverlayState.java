package awa.qwq.ovo.Naven.ui.Island;

import net.minecraft.text.Text;

public final class TabOverlayState {
   private static volatile Text header;
   private static volatile Text footer;

   private TabOverlayState() {
   }

   public static void setHeader(Text component) {
      header = component;
   }

   public static void setFooter(Text component) {
      footer = component;
   }

   public static Text getHeader() {
      return header;
   }

   public static Text getFooter() {
      return footer;
   }
}
