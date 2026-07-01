package awa.qwq.ovo.Naven.viaversionfix.items.mace;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

public final class MaceTier implements Tier {
   public static final MaceTier INSTANCE = new MaceTier();

   private MaceTier() {
   }

   @Override
   public int getUses() {
      return 500;
   }

   @Override
   public float getSpeed() {
      return 6.0F;
   }

   @Override
   public float getAttackDamageBonus() {
      return 0.0F;
   }

   @Override
   public int getLevel() {
      return 1;
   }

   @Override
   public int getEnchantmentValue() {
      return 14;
   }

   @Override
   public Ingredient getRepairIngredient() {
      return Ingredient.of(Blocks.IRON_BLOCK);
   }
}
