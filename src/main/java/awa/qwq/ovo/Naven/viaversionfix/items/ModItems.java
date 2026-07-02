package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceItem;
import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceTier;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearItem;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearMaterial;
import awa.qwq.ovo.Naven.viaversionfix.items.windcharge.WindChargeItem;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {
   public static final String MOD_ID = "naven-modern";
   public static final ResourceLocation MACE_ID = new ResourceLocation(MOD_ID, "viaversionfix/items/mace");
   public static final ResourceLocation WIND_CHARGE_ID = new ResourceLocation(MOD_ID, "viaversionfix/items/wind_charge");
   public static final Item MACE = new MaceItem(new Item.Properties().stacksTo(1).durability(MaceTier.INSTANCE.getUses()).rarity(Rarity.RARE));
   public static final Item WIND_CHARGE = new WindChargeItem(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
   public static final ItemStack MACE_RENDER_STACK = new ItemStack(MACE);
   public static final ItemStack WIND_CHARGE_RENDER_STACK = new ItemStack(WIND_CHARGE);
   private static final Map<SpearMaterial, SpearItem> SPEARS = new EnumMap<>(SpearMaterial.class);
   private static final Map<SpearMaterial, ItemStack> SPEAR_RENDER_STACKS = new EnumMap<>(SpearMaterial.class);
   public static final ResourceKey<CreativeModeTab> ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(MOD_ID, "viaversionfix"));

   static {
      for (SpearMaterial material : SpearMaterial.values()) {
         Item.Properties properties = new Item.Properties().stacksTo(1).durability(material.tier().getUses()).rarity(material.rarity());
         if (material.isNetherite()) {
            properties.fireResistant();
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
      Registry.register(BuiltInRegistries.ITEM, MACE_ID, MACE);
      Registry.register(BuiltInRegistries.ITEM, WIND_CHARGE_ID, WIND_CHARGE);
      for (SpearMaterial material : SpearMaterial.values()) {
         Registry.register(BuiltInRegistries.ITEM, material.itemId(MOD_ID), getSpear(material));
      }

      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         ITEM_GROUP,
         FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.naven-modern.viaversionfix"))
            .icon(() -> new ItemStack(MACE))
            .displayItems((parameters, output) -> {
               output.accept(MACE);
               output.accept(WIND_CHARGE);
               for (SpearMaterial material : SpearMaterial.values()) {
                  output.accept(getSpear(material));
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
