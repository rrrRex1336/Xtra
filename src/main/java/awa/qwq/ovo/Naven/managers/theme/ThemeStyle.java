package awa.qwq.ovo.Naven.managers.theme;

import java.awt.Color;

public class ThemeStyle {
   public static final ThemeStyle WHITE = new ThemeStyle("White", true, false, 0xFFFFFF);
   public static final ThemeStyle RAINBOW = new ThemeStyle("Rainbow", false, true, 0xFF5555, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF);

   public static final ThemeStyle[] VALUES = new ThemeStyle[]{
      WHITE,
      RAINBOW,
      new ThemeStyle("Water", 0x0CE8C7, 0x0CA3E8),
      new ThemeStyle("Snow", 0xF7FCFF, 0xDEEFFF, 0xC0D8F5, 0x9EBBEA, 0x78A8E8),
      new ThemeStyle("Aubergine", new Color(170, 7, 107), new Color(97, 4, 95)),
      new ThemeStyle("Aqua", new Color(185, 250, 255), new Color(79, 199, 200)),
      new ThemeStyle("Banana", new Color(253, 236, 177), Color.WHITE),
      new ThemeStyle("Blend", new Color(71, 148, 253), new Color(71, 253, 160)),
      new ThemeStyle("Blossom", new Color(226, 208, 249), new Color(49, 119, 115)),
      new ThemeStyle("Bubblegum", new Color(243, 145, 216), new Color(152, 165, 243)),
      new ThemeStyle("Candy Cane", Color.WHITE, Color.RED),
      new ThemeStyle("Cherry", new Color(187, 55, 125), new Color(251, 211, 233)),
      new ThemeStyle("Christmas", new Color(255, 64, 64), Color.WHITE, new Color(64, 255, 64)),
      new ThemeStyle("Coral", new Color(244, 168, 150), new Color(52, 133, 151)),
      new ThemeStyle("Digital Horizon", new Color(95, 195, 228), new Color(229, 93, 135)),
      new ThemeStyle("Express", new Color(173, 83, 137), new Color(60, 16, 83)),
      new ThemeStyle("Lime Water", new Color(18, 255, 247), new Color(179, 255, 171)),
      new ThemeStyle("Lush", new Color(168, 224, 99), new Color(86, 171, 47)),
      new ThemeStyle("Halogen", new Color(255, 65, 108), new Color(255, 75, 43)),
      new ThemeStyle("Hyper", new Color(236, 110, 173), new Color(52, 148, 230)),
      new ThemeStyle("Magic", new Color(74, 0, 224), new Color(142, 45, 226)),
      new ThemeStyle("May", new Color(253, 219, 245), new Color(238, 79, 238)),
      new ThemeStyle("Orange Juice", new Color(252, 74, 26), new Color(247, 183, 51)),
      new ThemeStyle("Pastel", new Color(243, 155, 178), new Color(207, 196, 243)),
      new ThemeStyle("Pumpkin", new Color(241, 166, 98), new Color(255, 216, 169), new Color(227, 139, 42)),
      new ThemeStyle("Satin", new Color(215, 60, 67), new Color(140, 23, 39)),
      new ThemeStyle("Snowy Sky", new Color(1, 171, 179), new Color(234, 234, 234), new Color(18, 232, 232)),
      new ThemeStyle("Steel Fade", new Color(66, 134, 244), new Color(55, 59, 68)),
      new ThemeStyle("Sundae", new Color(206, 74, 126), new Color(122, 44, 77)),
      new ThemeStyle("Sunkist", new Color(242, 201, 76), new Color(242, 153, 74)),
      new ThemeStyle("Winter", Color.WHITE, Color.WHITE),
      new ThemeStyle("Wood", new Color(79, 109, 81), new Color(170, 139, 87), new Color(240, 235, 206)),
      new ThemeStyle("Cotton Candy", new Color(255, 159, 243), new Color(84, 160, 255)),
      new ThemeStyle("Cyber Lime", new Color(0, 255, 163), new Color(0, 92, 151)),
      new ThemeStyle("Lavender", new Color(195, 176, 255), new Color(255, 214, 255)),
      new ThemeStyle("Rose Gold", new Color(183, 110, 121), new Color(255, 221, 195)),
      new ThemeStyle("Volcano", new Color(255, 81, 47), new Color(221, 36, 118))
   };

   private final String name;
   private final int[] colors;
   private final boolean white;
   private final boolean rainbow;

   private ThemeStyle(String name, boolean white, boolean rainbow, int... colors) {
      this.name = name;
      this.white = white;
      this.rainbow = rainbow;
      this.colors = colors;
   }

   public ThemeStyle(String name, int... colors) {
      this(name, false, false, colors);
   }

   public ThemeStyle(String name, Color... colors) {
      this.name = name;
      this.white = false;
      this.rainbow = false;
      this.colors = new int[colors.length];
      for (int i = 0; i < colors.length; i++) {
         this.colors[i] = colors[i].getRGB() & 0x00FFFFFF;
      }
   }

   public String getName() {
      return this.name;
   }

   public boolean isWhite() {
      return this.white;
   }

   public boolean isRainbow() {
      return this.rainbow;
   }

   public int getFirstColor() {
      return this.colors.length == 0 ? 0xFFFFFF : this.colors[0];
   }

   public int getSecondColor() {
      return this.colors.length <= 1 ? this.getFirstColor() : this.colors[1];
   }

   public int getColor(int index, float speed) {
      if (this.white) {
         return 0xFFFFFF;
      }

      float mappedSpeed = Math.max(0.25F, 21.0F - speed * 1.9F);
      if (this.rainbow) {
         long period = Math.max(250L, (long)(mappedSpeed * 1000.0F));
         float hue = (float)Math.floorMod(System.currentTimeMillis() + (long)index, period) / (float)period;
         return Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0x00FFFFFF;
      }

      long period = Math.max(800L, (long)(mappedSpeed * 1000.0F));
      float progress = (float)Math.floorMod(System.currentTimeMillis() + (long)index * 50L, period) / (float)period;
      return this.sample(progress);
   }

   public int sample(float progress) {
      if (this.colors.length == 0) {
         return 0xFFFFFF;
      }

      if (this.colors.length == 1 || this.white) {
         return this.colors[0];
      }

      if (this.rainbow) {
         return Color.HSBtoRGB(wrap(progress), 1.0F, 1.0F) & 0x00FFFFFF;
      }

      float scaled = wrap(progress) * this.colors.length;
      int index = (int)Math.floor(scaled);
      float local = smooth(scaled - index);
      int first = this.colors[index % this.colors.length];
      int second = this.colors[(index + 1) % this.colors.length];
      return mix(first, second, local);
   }

   public static String[] names() {
      String[] names = new String[VALUES.length];
      for (int i = 0; i < VALUES.length; i++) {
         names[i] = VALUES[i].name;
      }
      return names;
   }

   public static ThemeStyle byName(String name) {
      for (ThemeStyle style : VALUES) {
         if (style.name.equalsIgnoreCase(name)) {
            return style;
         }
      }
      return WHITE;
   }

   private static float wrap(float value) {
      float wrapped = value % 1.0F;
      return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
   }

   private static float smooth(float value) {
      float t = Math.max(0.0F, Math.min(1.0F, value));
      return t * t * (3.0F - 2.0F * t);
   }

   public static int mix(int first, int second, float progress) {
      float t = Math.max(0.0F, Math.min(1.0F, progress));
      int r = (int)(((first >> 16) & 0xFF) + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * t);
      int g = (int)(((first >> 8) & 0xFF) + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * t);
      int b = (int)((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
      return (r << 16) | (g << 8) | b;
   }
}
