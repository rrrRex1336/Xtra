package awa.qwq.ovo.Naven.managers.theme;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.values.HasValue;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

public class ThemeManager extends HasValue {
   public final ModeValue theme = ValueBuilder.create(this, "Color")
      .setModes(ThemeStyle.names())
      .setDefaultModeIndex(defaultIndex("Water"))
      .build()
      .getModeValue();

   public final FloatValue colorSpeed = ValueBuilder.create(this, "Color Speed")
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();

   public final FloatValue colorOffset = ValueBuilder.create(this, "Color Offset")
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();

   public ThemeManager() {
      this.setName("Theme");
      Naven.getInstance().getHasValueManager().registerHasValue(this);
   }

   public ThemeStyle getCurrentStyle() {
      return ThemeStyle.VALUES[this.theme.getCurrentValue()];
   }

   public int getColor(float offset) {
      int index = (int)(-offset * this.colorOffset.getCurrentValue());
      return this.getCurrentStyle().getColor(index, this.colorSpeed.getCurrentValue());
   }

   public int getPrimaryColor() {
      return this.getCurrentStyle().getFirstColor();
   }

   public int getSecondaryColor() {
      return this.getCurrentStyle().getSecondColor();
   }

   public void setStyle(ThemeStyle style) {
      for (int i = 0; i < ThemeStyle.VALUES.length; i++) {
         if (ThemeStyle.VALUES[i] == style) {
            this.theme.setCurrentValue(i);
            return;
         }
      }
   }

   private static int defaultIndex(String name) {
      for (int i = 0; i < ThemeStyle.VALUES.length; i++) {
         if (ThemeStyle.VALUES[i].getName().equalsIgnoreCase(name)) {
            return i;
         }
      }
      return 0;
   }
}
