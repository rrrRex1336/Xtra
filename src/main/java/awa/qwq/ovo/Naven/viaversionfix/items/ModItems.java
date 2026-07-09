package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceItem;
import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceTier;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearItem;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearMaterial;
import awa.qwq.ovo.Naven.viaversionfix.items.windcharge.WindChargeItem;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
   public static final String MOD_ID = "naven-modern";
   public static final Identifier MACE_ID = new Identifier(MOD_ID, "viaversionfix/items/mace");
   public static final Identifier WIND_CHARGE_ID = new Identifier(MOD_ID, "viaversionfix/items/wind_charge");
   public static final Item MACE = new MaceItem(new Item.Settings().maxCount(1).maxDamage(MaceTier.INSTANCE.getDurability()).rarity(Rarity.RARE));
   public static final Item WIND_CHARGE = new WindChargeItem(new Item.Settings().maxCount(64).rarity(Rarity.COMMON));
   public static final ItemStack MACE_RENDER_STACK = new ItemStack(MACE);
   public static final ItemStack WIND_CHARGE_RENDER_STACK = new ItemStack(WIND_CHARGE);
   private static final Map<SpearMaterial, SpearItem> SPEARS = new EnumMap<>(SpearMaterial.class);
   private static final Map<SpearMaterial, ItemStack> SPEAR_RENDER_STACKS = new EnumMap<>(SpearMaterial.class);
   public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(MOD_ID, "viaversionfix"));

   static {
      for (SpearMaterial material : SpearMaterial.values()) {
         Item.Settings properties = new Item.Settings().maxCount(1).maxDamage(material.tier().getDurability()).rarity(material.rarity());
         if (material.isNetherite()) {
            properties.fireproof();
         }

         SpearItem spear = new SpearItem(material, properties);
         SPEARS.put(material, spear);
         SPEAR_RENDER_STACKS.put(material, new ItemStack(spear));
      }
   }

   private ModItems() {
   }

   public static void init() {
      ModSounds.init();
      ModEntities.init();
      Registry.register(Registries.ITEM, MACE_ID, MACE);
      Registry.register(Registries.ITEM, WIND_CHARGE_ID, WIND_CHARGE);
      for (SpearMaterial material : SpearMaterial.values()) {
         Registry.register(Registries.ITEM, material.itemId(MOD_ID), getSpear(material));
      }

      Registry.register(
         Registries.ITEM_GROUP,
         ITEM_GROUP,
         FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.naven-modern.viaversionfix"))
            .icon(() -> new ItemStack(MACE))
            .entries((parameters, output) -> {
               output.add(MACE);
               output.add(WIND_CHARGE);
               for (SpearMaterial material : SpearMaterial.values()) {
                  output.add(getSpear(material));
               }
            })
            .build()
      );
   }

   public static SpearItem getSpear(SpearMaterial material) {
      return SPEARS.get(material);
   }

   public static SpearMaterial getSpearMaterial(Item item) {
      for (SpearMaterial material : SpearMaterial.values()) {
         if (getSpear(material) == item) {
            return material;
         }
      }

      return null;
   }

   public static ItemStack getSpearRenderStack(SpearMaterial material) {
      ItemStack stack = SPEAR_RENDER_STACKS.get(material);
      return stack == null ? ItemStack.EMPTY : stack;
   }
}
