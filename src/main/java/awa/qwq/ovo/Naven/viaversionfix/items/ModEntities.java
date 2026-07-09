package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.items.windcharge.WindChargeProjectile;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
   public static final Identifier WIND_CHARGE_PROJECTILE_ID = new Identifier(ModItems.MOD_ID, "viaversionfix/entities/wind_charge");
   public static final EntityType<WindChargeProjectile> WIND_CHARGE_PROJECTILE = FabricEntityTypeBuilder
      .<WindChargeProjectile>create(SpawnGroup.MISC, WindChargeProjectile::new)
      .dimensions(EntityDimensions.fixed(0.3125F, 0.3125F))
      .trackRangeBlocks(4)
      .trackedUpdateRate(10)
      .build();

   private ModEntities() {
   }

   public static void init() {
      Registry.register(Registries.ENTITY_TYPE, WIND_CHARGE_PROJECTILE_ID, WIND_CHARGE_PROJECTILE);
   }
}
