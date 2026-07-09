package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;

@ModuleInfo(
   name = "Glow",
   description = "Glow effect for entities",
   category = Category.VISUAL
)
public class Glow extends Module {
   BooleanValue players = ValueBuilder.create(this, "Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue items = ValueBuilder.create(this, "Items").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue mobs = ValueBuilder.create(this, "Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue animals = ValueBuilder.create(this, "Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue arrows = ValueBuilder.create(this, "Arrows").setDefaultBooleanValue(false).build().getBooleanValue();

   public static boolean shouldGlow(Entity entity) {
      Glow module = (Glow)Naven.getInstance().getModuleManager().getModule(Glow.class);
      if (!module.isEnabled()) {
         return false;
      } else if (entity instanceof PlayerEntity && module.players.getCurrentValue()) {
         return true;
      } else if (entity instanceof ItemEntity && module.items.getCurrentValue()) {
         return true;
      } else if (entity instanceof MobEntity && module.mobs.getCurrentValue()) {
         return true;
      } else {
         return entity instanceof AnimalEntity && module.animals.getCurrentValue() ? true : entity instanceof ArrowEntity && module.arrows.getCurrentValue();
      }
   }
}
