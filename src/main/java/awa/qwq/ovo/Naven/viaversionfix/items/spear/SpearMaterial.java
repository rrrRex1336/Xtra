package awa.qwq.ovo.Naven.viaversionfix.items.spear;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public enum SpearMaterial {
   WOODEN("wooden_spear", ToolMaterials.WOOD, true, 0.65F, 0.7F, 0.75F, 5.0F, 14.0F, 10.0F, 5.1F, 15.0F, 4.6F),
   STONE("stone_spear", ToolMaterials.STONE, false, 0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F),
   COPPER("copper_spear", new CopperTier(), false, 0.85F, 0.82F, 0.65F, 4.0F, 12.0F, 8.25F, 5.1F, 12.5F, 4.6F),
   IRON("iron_spear", ToolMaterials.IRON, false, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F),
   GOLDEN("golden_spear", ToolMaterials.GOLD, false, 0.95F, 0.7F, 0.7F, 3.5F, 13.0F, 8.5F, 5.1F, 13.75F, 4.6F),
   DIAMOND("diamond_spear", ToolMaterials.DIAMOND, false, 1.05F, 1.075F, 0.5F, 3.0F, 10.0F, 6.5F, 5.1F, 10.0F, 4.6F),
   NETHERITE("netherite_spear", ToolMaterials.NETHERITE, false, 1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F);

   public static final float MIN_ATTACK_RANGE = 2.0F;
   public static final float MAX_ATTACK_RANGE = 4.5F;
   public static final float CREATIVE_ATTACK_RANGE = 6.5F;
   public static final float HITBOX_MARGIN = 0.125F;
   public static final int CONTACT_COOLDOWN_TICKS = 10;
   public static final float FORWARD_MOVEMENT = 0.38F;

   private final String id;
   private final ToolMaterial tier;
   private final boolean wood;
   private final float swingDurationSeconds;
   private final float damageMultiplier;
   private final int delayTicks;
   private final int dismountMaxTicks;
   private final float dismountMinSpeed;
   private final int knockbackMaxTicks;
   private final float knockbackMinSpeed;
   private final int damageMaxTicks;
   private final float damageMinRelativeSpeed;

   SpearMaterial(
      String id,
      ToolMaterial tier,
      boolean wood,
      float swingDurationSeconds,
      float damageMultiplier,
      float delaySeconds,
      float dismountMaxSeconds,
      float dismountMinSpeed,
      float knockbackMaxSeconds,
      float knockbackMinSpeed,
      float damageMaxSeconds,
      float damageMinRelativeSpeed
   ) {
      this.id = id;
      this.tier = tier;
      this.wood = wood;
      this.swingDurationSeconds = swingDurationSeconds;
      this.damageMultiplier = damageMultiplier;
      this.delayTicks = secondsToTicks(delaySeconds);
      this.dismountMaxTicks = secondsToTicks(dismountMaxSeconds);
      this.dismountMinSpeed = dismountMinSpeed;
      this.knockbackMaxTicks = secondsToTicks(knockbackMaxSeconds);
      this.knockbackMinSpeed = knockbackMinSpeed;
      this.damageMaxTicks = secondsToTicks(damageMaxSeconds);
      this.damageMinRelativeSpeed = damageMinRelativeSpeed;
   }

   public String id() {
      return this.id;
   }

   public String itemPath() {
      return "viaversionfix/items/spear/" + this.id;
   }

   public Identifier itemId(String modId) {
      return new Identifier(modId, this.itemPath());
   }

   public ToolMaterial tier() {
      return this.tier;
   }

   public boolean isWood() {
      return this.wood;
   }

   public boolean isNetherite() {
      return this == NETHERITE;
   }

   public Rarity rarity() {
      return this == NETHERITE ? Rarity.RARE : Rarity.COMMON;
   }

   public float attackDamageBonus() {
      return this.tier.getAttackDamage();
   }

   public float attackSpeedModifier() {
      return 1.0F / this.swingDurationSeconds - 4.0F;
   }

   public float damageMultiplier() {
      return this.damageMultiplier;
   }

   public int delayTicks() {
      return this.delayTicks;
   }

   public int dismountMaxTicks() {
      return this.dismountMaxTicks;
   }

   public float dismountMinSpeed() {
      return this.dismountMinSpeed;
   }

   public int knockbackMaxTicks() {
      return this.knockbackMaxTicks;
   }

   public float knockbackMinSpeed() {
      return this.knockbackMinSpeed;
   }

   public int damageMaxTicks() {
      return this.damageMaxTicks;
   }

   public float damageMinRelativeSpeed() {
      return this.damageMinRelativeSpeed;
   }

   public String translationKey() {
      return "item.naven-modern." + this.itemPath();
   }

   public boolean matches(Item item) {
      return item instanceof SpearItem spearItem && spearItem.getSpearMaterial() == this;
   }

   public static SpearMaterial fromServerName(String rawName) {
      if (rawName == null) {
         return null;
      }

      String name = stripVersionPrefix(rawName.trim().toLowerCase());
      if (name.equals("spear")) {
         return IRON;
      }

      return switch (name) {
         case "wooden spear", "wood spear" -> WOODEN;
         case "stone spear" -> STONE;
         case "copper spear" -> COPPER;
         case "iron spear" -> IRON;
         case "golden spear", "gold spear" -> GOLDEN;
         case "diamond spear" -> DIAMOND;
         case "netherite spear" -> NETHERITE;
         default -> null;
      };
   }

   private static String stripVersionPrefix(String name) {
      int firstSpace = name.indexOf(' ');
      if (firstSpace <= 0 || firstSpace == name.length() - 1) {
         return name;
      }

      String prefix = name.substring(0, firstSpace);
      if (!prefix.startsWith("1.")) {
         return name;
      }

      for (int i = 0; i < prefix.length(); i++) {
         char c = prefix.charAt(i);
         if ((c < '0' || c > '9') && c != '.') {
            return name;
         }
      }

      return name.substring(firstSpace + 1);
   }

   private static int secondsToTicks(float seconds) {
      return (int)(seconds * 20.0F);
   }

   private static final class CopperTier implements ToolMaterial {
      @Override
      public int getDurability() {
         return 190;
      }

      @Override
      public float getMiningSpeedMultiplier() {
         return 5.0F;
      }

      @Override
      public float getAttackDamage() {
         return 1.0F;
      }

      @Override
      public int getMiningLevel() {
         return 1;
      }

      @Override
      public int getEnchantability() {
         return 13;
      }

      @Override
      public Ingredient getRepairIngredient() {
         return Ingredient.ofItems(Items.COPPER_INGOT);
      }
   }
}
