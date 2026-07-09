package awa.qwq.ovo.Naven.modules.impl.visual.projectiles;

import java.awt.Color;
import net.minecraft.entity.Entity;

public interface ProjectileData {
   Color getColor(Object var1);

   default float getData1() {
      return 0.125F;
   }

   boolean isTargetEntity(Entity var1);

   default float getData2() {
      return 0.25F;
   }

   default float getGravity() {
      return 0.03F;
   }
}
