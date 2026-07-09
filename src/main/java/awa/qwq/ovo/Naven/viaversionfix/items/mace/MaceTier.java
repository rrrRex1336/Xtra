package awa.qwq.ovo.Naven.viaversionfix.items.mace;

import net.minecraft.block.Blocks;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public final class MaceTier implements ToolMaterial {
   public static final MaceTier INSTANCE = new MaceTier();

   private MaceTier() {
   }

   @Override
   public int getDurability() {
      return 500;
   }

   @Override
   public float getMiningSpeedMultiplier() {
      return 6.0F;
   }

   @Override
   public float getAttackDamage() {
      return 0.0F;
   }

   @Override
   public int getMiningLevel() {
      return 1;
   }

   @Override
   public int getEnchantability() {
      return 14;
   }

   @Override
   public Ingredient getRepairIngredient() {
      return Ingredient.ofItems(Blocks.IRON_BLOCK);
   }
}
