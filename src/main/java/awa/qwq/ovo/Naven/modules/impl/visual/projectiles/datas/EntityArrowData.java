package awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import net.minecraft.entity.projectile.ArrowEntity;

public class EntityArrowData extends BasicProjectileData {
   public EntityArrowData() {
      super(new HashSet<>(Collections.singletonList(ArrowEntity.class)), new Color(255, 0, 0));
   }

   @Override
   public float getData1() {
      return 0.25F;
   }

   @Override
   public float getData2() {
      return 0.5F;
   }

   @Override
   public float getGravity() {
      return 0.05F;
   }
}
