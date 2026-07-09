package awa.qwq.ovo.Naven.utils;

import com.google.common.collect.Multimap;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearItem;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearMaterial;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BookItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class InventoryUtils {
   private static final int NBT_STRING = 8;
   public static final int INCLUDE_ARMOR_BEGIN = 5;
   public static final int EXCLUDE_ARMOR_BEGIN = 9;
   public static final int ONLY_HOT_BAR_BEGIN = 36;
   public static final int END = 45;
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean shouldDisableFeatures() {
      return getAllItems().stream().anyMatch(item -> {
         if (item.isEmpty()) {
            return false;
         } else {
            String string = item.toHoverableText().getString();
            return string.contains("长按点击") || string.contains("点击使用") || string.contains("离开游戏") || string.contains("选择一个队伍") || string.contains("再来一局");
         }
      });
   }

   public static double getItemDamage(ItemStack stack) {
      double damage = 0.0;
      Multimap<EntityAttribute, EntityAttributeModifier> attributeModifierMap = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

      for (EntityAttribute attributeName : attributeModifierMap.keySet()) {
         if (attributeName.getTranslationKey().equals("attribute.name.generic.attack_damage")) {
            Iterator<EntityAttributeModifier> attributeModifiers = attributeModifierMap.get(attributeName).iterator();
            if (attributeModifiers.hasNext()) {
               damage += attributeModifiers.next().getValue();
            }
            break;
         }
      }

      if (stack.hasGlint()) {
         damage += EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, stack);
         damage += EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) * 1.25;
      }

      return damage;
   }

   public static boolean isGoldenHead(ItemStack e) {
      if (e.isEmpty()) {
         return false;
      } else {
         if (e.getItem() instanceof BlockItem) {
            BlockItem item = (BlockItem)e.getItem();
            if (item.getBlock() instanceof SkullBlock) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isSharpnessAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (isMace(stack)) {
         return false;
      } else if (!(stack.getItem() instanceof AxeItem)) {
         return false;
      } else {
         int itemEnchantmentLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
         return itemEnchantmentLevel >= 8 && itemEnchantmentLevel < 50;
      }
   }

   public static boolean isMace(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      return stack.isOf(ModItems.MACE) || (ViaVersionFix.isHighVersionItemFixEnabled() && isServerMace(stack));
   }

   public static boolean isWindCharge(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      return stack.isOf(ModItems.WIND_CHARGE) || (ViaVersionFix.isHighVersionItemFixEnabled() && isServerWindCharge(stack));
   }

   public static boolean isSpear(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      return stack.getItem() instanceof SpearItem || (ViaVersionFix.isHighVersionItemFixEnabled() && isServerSpear(stack));
   }

   public static boolean isServerMace(ItemStack stack) {
      if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AxeItem)) {
         return false;
      }

      String name = Formatting.strip(getRawHoverName(stack));
      if (name == null) {
         return false;
      }

      String normalized = name.trim();
      return normalized.equalsIgnoreCase("1.21 Mace") || normalized.equalsIgnoreCase("Mace");
   }

   public static boolean isServerWindCharge(ItemStack stack) {
      if (stack == null || stack.isEmpty() || stack.getItem() != Items.SNOWBALL) {
         return false;
      }

      String name = Formatting.strip(getRawHoverName(stack));
      if (name == null) {
         return false;
      }

      String normalized = name.trim();
      String lower = normalized.toLowerCase();
      return lower.equals("wind charge") || lower.endsWith(" wind charge") && lower.startsWith("1.");
   }

   public static boolean isServerSpear(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      String name = Formatting.strip(getRawHoverName(stack));
      if (name == null) {
         return false;
      }

      return SpearMaterial.fromServerName(name) != null;
   }

   public static SpearMaterial getServerSpearMaterial(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      String name = Formatting.strip(getRawHoverName(stack));
      return SpearMaterial.fromServerName(name);
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

   private static String getRawHoverName(ItemStack stack) {
      NbtCompound display = stack.getSubNbt(ItemStack.DISPLAY_KEY);
      if (display != null && display.contains(ItemStack.NAME_KEY, NBT_STRING)) {
         String rawName = display.getString(ItemStack.NAME_KEY);
         try {
            Text component = Text.Serialization.fromJson(rawName);
            if (component != null) {
               return component.getString();
            }
         } catch (Exception ignored) {
         }

         if (rawName.length() >= 2 && rawName.startsWith("\"") && rawName.endsWith("\"")) {
            return rawName.substring(1, rawName.length() - 1);
         }

         return rawName;
      }

      Identifier key = Registries.ITEM.getId(stack.getItem());
      return key == null ? "" : key.toString();
   }

   public static ItemStack getMace() {
      for (ItemStack stack : mc.player.getInventory().main) {
         if (isMace(stack)) {
            return stack;
         }
      }

      return null;
   }

   public static ItemStack getWindCharge() {
      for (ItemStack stack : mc.player.getInventory().main) {
         if (isWindCharge(stack)) {
            return stack;
         }
      }

      return null;
   }

   public static boolean isGodAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.GOLDEN_AXE ? false : EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) > 100;
      }
   }

   public static boolean isEnchantedGApple(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
   }

   public static boolean isEndCrystal(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.END_CRYSTAL;
   }

   public static boolean isKBBall(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.SLIME_BALL ? false : EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static boolean isKBStick(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.STICK ? false : EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static int findEmptyInventory() {
      for (int i = 9; i < mc.player.getInventory().main.size(); i++) {
         if (((ItemStack)mc.player.getInventory().main.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int findEmptySlot() {
      for (int i = 0; i < 9; i++) {
         if (((ItemStack)mc.player.getInventory().main.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int getPunchLevel(ItemStack stack) {
      return EnchantmentHelper.getLevel(Enchantments.PUNCH, stack);
   }

   public static int getPowerLevel(ItemStack stack) {
      return EnchantmentHelper.getLevel(Enchantments.POWER, stack);
   }

   public static List<ItemStack> getAllItems() {
      ArrayList<ItemStack> list = new ArrayList<>(40);
      list.addAll(mc.player.getInventory().main);
      list.addAll(mc.player.getInventory().armor);
      return list;
   }

   public static float getBestArmorScore(EquipmentSlot slot) {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof ArmorItem && ((ArmorItem)item.getItem()).getSlotType() == slot)
              .map(InventoryUtils::getProtection)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static float getCurrentArmorScore(EquipmentSlot slot) {
      if (slot == EquipmentSlot.HEAD) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(3));
      } else if (slot == EquipmentSlot.CHEST) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(2));
      } else if (slot == EquipmentSlot.LEGS) {
         return getProtection((ItemStack)mc.player.getInventory().armor.get(1));
      } else {
         return slot == EquipmentSlot.FEET ? getProtection((ItemStack)mc.player.getInventory().armor.get(0)) : 0.0F;
      }
   }

   public static float getBestSwordDamage() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem)
              .map(InventoryUtils::getSwordDamage)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestSword() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem)
              .max(Comparator.comparingInt(s -> (int)(getSwordDamage(s) * 100.0F)))
              .orElse(null);
   }

   public static int getItemStackSlot(ItemStack stack) {
      if (stack == null) {
         return -1;
      } else {
         for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().main.get(i) == stack) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean isItemValid(ItemStack s) {
      if (!s.isEmpty()) {
         if (s.getItem() instanceof PlayerHeadItem) {
            return false;
         }

         String string = s.toHoverableText().getString();
         if (string.contains("Click")) {
            return false;
         }

         if (string.contains("Right")) {
            return false;
         }

         if (string.contains("点击")) {
            return false;
         }

         if (string.contains("Teleport")) {
            return false;
         }

         if (string.contains("使用")) {
            return false;
         }

         if (string.contains("传送")) {
            return false;
         }

         if (string.contains("再来")) {
            return false;
         }
      }

      return true;
   }

   public static int getItemSlot(Item item) {
      for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
         ItemStack itemStack = (ItemStack)mc.player.getInventory().main.get(i);
         if (item == ModItems.WIND_CHARGE && isWindCharge(itemStack)) {
            return i;
         }

         if (itemStack.getItem() == item && !isServerWindCharge(itemStack)) {
            return i;
         }
      }

      return -1;
   }

   public static ItemStack getBestProjectile() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && !isWindCharge(item) && isItemValid(item))
              .max(Comparator.comparingInt(ItemStack::getCount))
              .orElse(null);
   }

   public static ItemStack getFishingRod() {
      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof FishingRodItem && isItemValid(item)).findAny().orElse(null);
   }

   public static int getBlockCountInInventory() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
              .mapToInt(ItemStack::getCount)
              .sum();
   }

   public static ItemStack getWorstProjectile() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && !isWindCharge(item))
              .min(Comparator.comparingInt(ItemStack::getCount))
              .orElse(null);
   }

   public static ItemStack getWorstArrow() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof ArrowItem && isItemValid(item))
              .min(Comparator.comparingInt(ItemStack::getCount))
              .orElse(null);
   }

   public static ItemStack getWorstBlock() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
              .min(Comparator.comparingInt(ItemStack::getCount))
              .orElse(null);
   }

   public static ItemStack getBestBlock() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
              .max(Comparator.comparingInt(ItemStack::getCount))
              .orElse(null);
   }

   public static float getBestPickaxeScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && isItemValid(item))
              .map(InventoryUtils::getToolScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestPickaxe() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
              .orElse(null);
   }

   public static float getBestAxeScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && !isMace(item) && !isSharpnessAxe(item) && isItemValid(item))
              .map(InventoryUtils::getToolScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestAxe() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && !isMace(item) && !isSharpnessAxe(item) && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
              .orElse(null);
   }

   public static ItemStack getBestShapeAxe() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && !isMace(item) && isSharpnessAxe(item) && isItemValid(item) && !isGodAxe(item))
              .max(Comparator.comparingInt(s -> (int)(getAxeDamage(s) * 100.0F)))
              .orElse(null);
   }

   public static float getBestShovelScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof ShovelItem && isItemValid(item))
              .map(InventoryUtils::getToolScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestShovel() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof ShovelItem && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
              .orElse(null);
   }

   public static float getBestCrossbowScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
              .map(InventoryUtils::getCrossbowScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestCrossbow() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getCrossbowScore(s) * 100.0F)))
              .orElse(null);
   }

   public static float getBestPunchBowScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
              .map(InventoryUtils::getPunchBowScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestPunchBow() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getPunchBowScore(s) * 100.0F)))
              .orElse(null);
   }

   public static float getBestPowerBowScore() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
              .map(InventoryUtils::getPowerBowScore)
              .max(Float::compareTo)
              .orElse(0.0F);
   }

   public static ItemStack getBestPowerBow() {
      return getAllItems()
              .stream()
              .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
              .max(Comparator.comparingInt(s -> (int)(getPowerBowScore(s) * 100.0F)))
              .orElse(null);
   }

   public static boolean isPunchBow(ItemStack stack) {
      return getPunchBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean isPowerBow(ItemStack stack) {
      return getPowerBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean hasItem(Item checkItem) {
      if (checkItem == ModItems.WIND_CHARGE) {
         return getAllItems().stream().anyMatch(InventoryUtils::isWindCharge);
      }

      return getAllItems().stream().anyMatch(item -> !item.isEmpty() && item.getItem() == checkItem && !isServerWindCharge(item));
   }

   public static int getItemCount(Item checkItem) {
      if (checkItem == ModItems.WIND_CHARGE) {
         return getAllItems().stream().filter(InventoryUtils::isWindCharge).mapToInt(ItemStack::getCount).sum();
      }

      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem && !isServerWindCharge(item)).mapToInt(ItemStack::getCount).sum();
   }

   public static float getPunchBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += EnchantmentHelper.getLevel(Enchantments.PUNCH, stack);
         valence += EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
         valence += EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
         valence += EnchantmentHelper.getLevel(Enchantments.POWER, stack) / 10.0F;
         return valence + (float)stack.getDamage() / stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getPowerBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += EnchantmentHelper.getLevel(Enchantments.PUNCH, stack) / 10.0F;
         valence += EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
         valence += EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
         valence += EnchantmentHelper.getLevel(Enchantments.POWER, stack);
         return valence + (float)stack.getDamage() / stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getToolScore(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (isMace(stack)) {
         return 0.0F;
      } else if (isGodItem(stack)) {
         return 0.0F;
      } else if (isSharpnessAxe(stack)) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof PickaxeItem) {
            valence += stack.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
         } else if (stack.getItem() instanceof AxeItem) {
            valence += stack.getMiningSpeedMultiplier(Blocks.OAK_LOG.getDefaultState());
         } else {
            if (!(stack.getItem() instanceof ShovelItem)) {
               return 0.0F;
            }

            valence += stack.getMiningSpeedMultiplier(Blocks.DIRT.getDefaultState());
         }

         int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
         if (efficiency > 0) {
            valence += efficiency * 0.0075F;
         }

         return valence;
      }
   }

   public static float getAxeDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (isMace(stack)) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof AxeItem && isSharpnessAxe(stack)) {
            AxeItem axe = (AxeItem)stack.getItem();
            if (axe == Items.WOODEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.STONE_AXE) {
               valence += 5.0F;
            } else if (axe == Items.IRON_AXE) {
               valence += 6.0F;
            } else if (axe == Items.GOLDEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.DIAMOND_AXE) {
               valence += 7.0F;
            }
         }

         int itemEnchantmentLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            float damageBonus = Enchantments.SHARPNESS.getAttackDamage(itemEnchantmentLevel, EntityGroup.DEFAULT);
            valence += damageBonus;
         }

         return valence;
      }
   }

   public static float getSwordDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof SwordItem) {
            SwordItem sword = (SwordItem)stack.getItem();
            valence += sword.getAttackDamage() + 1.0F;
         }

         int itemEnchantmentLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            float damageBonus = Enchantments.SHARPNESS.getAttackDamage(itemEnchantmentLevel, EntityGroup.DEFAULT);
            valence += damageBonus;
         }

         return valence;
      }
   }

   public static float getProtection(ItemStack itemStack) {
      int valence = 0;
      if (itemStack == null) {
         return 0.0F;
      } else if (itemStack.isEmpty()) {
         return 0.0F;
      } else {
         if (itemStack.getItem() instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem)itemStack.getItem();
            ArmorMaterial material = armor.getMaterial();
            if (material == ArmorMaterials.LEATHER) {
               valence += 100;
            } else if (material == ArmorMaterials.CHAIN) {
               valence += 200;
            } else if (material == ArmorMaterials.IRON) {
               valence += 400;
            } else if (material == ArmorMaterials.GOLD) {
               valence += 300;
            } else if (material == ArmorMaterials.DIAMOND) {
               valence += 500;
            } else if (material == ArmorMaterials.NETHERITE) {
               valence += 600;
            }
         }

         valence += EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack);
         return valence;
      }
   }

   public static float getCrossbowScore(ItemStack stack) {
      int valence = 0;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof CrossbowItem) {
            valence += EnchantmentHelper.getLevel(Enchantments.QUICK_CHARGE, stack);
            valence += EnchantmentHelper.getLevel(Enchantments.MULTISHOT, stack);
            valence += EnchantmentHelper.getLevel(Enchantments.PIERCING, stack);
         }

         return valence;
      }
   }

   public static boolean isGodItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof AxeItem && stack.getItem() == Items.GOLDEN_AXE && EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) > 100) {
         return true;
      } else if (stack.getItem() == Items.SLIME_BALL && EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack) > 1) {
         return true;
      } else {
         return stack.getItem() == Items.TOTEM_OF_UNDYING ? true : stack.getItem() == Items.END_CRYSTAL;
      }
   }

   public static boolean isCommonItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else if (isWindCharge(stack)) {
         return true;
      } else if (isSpear(stack)) {
         return true;
      } else {
         Item item = stack.getItem();
         if (item instanceof BlockItem block) {
            if (block.getBlock() == Blocks.ENCHANTING_TABLE) {
               return false;
            }

            if (block.getBlock() == Blocks.COBWEB) {
               return false;
            }
         } else {
            if (item instanceof BookItem) {
               return false;
            }

            if (item instanceof ExperienceBottleItem) {
               return false;
            }

            if (item instanceof FireworkRocketItem) {
               return false;
            }

            if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS) {
               return false;
            }

            if (item == Items.FLINT_AND_STEEL) {
               return false;
            }
         }

         return true;
      }
   }

   public static int findItem(int i, int i1, Item goldenApple) {
      return 0;
   }
}
