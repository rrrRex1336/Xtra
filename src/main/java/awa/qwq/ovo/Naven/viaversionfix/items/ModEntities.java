package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.items.windcharge.WindChargeProjectile;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final ResourceLocation WIND_CHARGE_PROJECTILE_ID = new ResourceLocation(ModItems.MOD_ID, "viaversionfix/entities/wind_charge");
   public static final EntityType<WindChargeProjectile> WIND_CHARGE_PROJECTILE = FabricEntityTypeBuilder
      .<WindChargeProjectile>create(MobCategory.MISC, WindChargeProjectile::new)
      .dimensions(EntityDimensions.fixed(0.3125F, 0.3125F))
      .trackRangeBlocks(4)
      .trackedUpdateRate(10)
      .build();

   private ModEntities() {
   }

   public static void init() {
      Registry.register(BuiltInRegistries.ENTITY_TYPE, WIND_CHARGE_PROJECTILE_ID, WIND_CHARGE_PROJECTILE);
   }
}
