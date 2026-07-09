package awa.qwq.ovo.Naven.modules;

import awa.qwq.ovo.Naven.utils.FontIcons;

public enum Category {
   COMBAT("Combat", FontIcons.SWORD),
   MOVEMENT("Movement", FontIcons.RUNNING),
   WORLD("World", FontIcons.CRAFT),
   VISUAL("Visual", FontIcons.EYE),
   THEME("Theme", FontIcons.CLIENT),
   PLAYER("Player", FontIcons.PLER),
   MISC("Misc", FontIcons.OTHER);

   private final String displayName;
   private final String icon;

   private Category(final String displayName, final String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }
}
