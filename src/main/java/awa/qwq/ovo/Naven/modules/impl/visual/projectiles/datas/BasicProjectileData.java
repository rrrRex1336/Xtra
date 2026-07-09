package awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas;

import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.ProjectileData;
import java.awt.Color;
import java.util.Set;
import net.minecraft.entity.Entity;

public class BasicProjectileData implements ProjectileData {
   private final Color color;
   private final Set<Class<?>> entityClass;

   public BasicProjectileData(Set<Class<?>> var1) {
      this(var1, new Color(255, 255, 255));
   }

   public BasicProjectileData(Set<Class<?>> var1, Color var2) {
      this.entityClass = var1;
      this.color = var2;
   }

   @Override
   public Color getColor(Object var1) {
      return this.color;
   }

   @Override
   public boolean isTargetEntity(Entity var1) {
      for (Class<?> var3 : this.entityClass) {
         if (var3.isInstance(var1)) {
            return true;
         }
      }

      return false;
   }
}
