package awa.qwq.ovo.Naven.utils.renderer;

import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;

import java.awt.FontFormatException;
import java.io.IOException;

public class Fonts {
   public static CustomTextRenderer opensans;
   public static CustomTextRenderer opensans28;
   public static CustomTextRenderer harmony;
   public static CustomTextRenderer icons;
   public static CustomTextRenderer edit_icons;
   public static CustomTextRenderer misans;
   public static CustomTextRenderer misansScoreboard;
   public static CustomTextRenderer comfortaa;
   public static CustomTextRenderer axiforma_regular;
   public static CustomTextRenderer productSansMedium;

   public static void loadFonts() throws IOException, FontFormatException {
      opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
      opensans28 = new CustomTextRenderer("opensans", 28, 0, 255, 512);
      comfortaa = new CustomTextRenderer("comfortaa", 32, 0, 65535, 512);
      harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
      icons = new CustomTextRenderer("icon", 32, 59648, 59658, 512);
      edit_icons = new CustomTextRenderer("edit_icons", 32, 59648, 59659, 512);
      misans = new CustomTextRenderer("misans", 18, 0, 10003, 4096);
      misansScoreboard = new CustomTextRenderer("misans", 17, 0, 65535, 8192);
      axiforma_regular = new CustomTextRenderer("axiforma_regular", 25, 0, 255, 1024);
      productSansMedium = new CustomTextRenderer("product_sans_medium", 22, 0, 255, 1024);

   }
}
