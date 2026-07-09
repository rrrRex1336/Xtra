package awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import net.minecraft.entity.projectile.thrown.PotionEntity;

public class EntityPotionData extends BasicProjectileData {
   public EntityPotionData() {
      super(new HashSet<>(Collections.singleton(PotionEntity.class)), new Color(255, 66, 249));
   }

   @Override
   public float getGravity() {
      return 0.05F;
   }
}
