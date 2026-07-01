package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceItem;
import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceTier;
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
   public static final Item MACE = new MaceItem(new Item.Properties().stacksTo(1).durability(MaceTier.INSTANCE.getUses()).rarity(Rarity.RARE));
   public static final ItemStack MACE_RENDER_STACK = new ItemStack(MACE);
   public static final ResourceKey<CreativeModeTab> ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(MOD_ID, "viaversionfix"));

   private ModItems() {
   }

   public static void init() {
      ModSounds.init();
      Registry.register(BuiltInRegistries.ITEM, MACE_ID, MACE);
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         ITEM_GROUP,
         FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.naven-modern.viaversionfix"))
            .icon(() -> new ItemStack(MACE))
            .displayItems((parameters, output) -> output.accept(MACE))
            .build()
      );
   }
}
