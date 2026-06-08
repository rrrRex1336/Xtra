package awa.qwq.ovo.Naven.modules.impl.Player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.World.Scaffold;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.TickTimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import org.mixin.accessors.AbstractFurnaceMenuAccessor;
import org.mixin.accessors.BrewingStandMenuAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.stream.IntStream;

@ModuleInfo(
        name = "ContainerStealer",
        description = "Automatically steals items from containers",
        category = Category.PLAYER
)
public class ContainerStealer extends Module {

   private static final TickTimeHelper timer = new TickTimeHelper();
   private static final TickTimeHelper timer1 = new TickTimeHelper();
   private static final TickTimeHelper timer2 = new TickTimeHelper();

   public BooleanValue pickTrash = ValueBuilder.create(this, "Pick Trash")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue instant = ValueBuilder.create(this, "Extra Packet")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue swap = ValueBuilder.create(this, "Swap")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private final FloatValue startDelay = ValueBuilder.create(this, "Start Delay")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   private final FloatValue stopDelay = ValueBuilder.create(this, "Close Delay")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   private final FloatValue minDelay = ValueBuilder.create(this, "Min Delay")
           .setVisibility(() -> !instant.getCurrentValue())
           .setDefaultFloatValue(2.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   private final FloatValue maxDelay = ValueBuilder.create(this, "Max Delay")
           .setVisibility(() -> !instant.getCurrentValue())
           .setDefaultFloatValue(6.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   public ModeValue motionMode = ValueBuilder.create(this, "Motion Mode")
           .setModes("Normal", "Silent")
           .setDefaultModeIndex(0)
           .build()
           .getModeValue();

   public ModeValue clickMode = ValueBuilder.create(this, "Click Mode")
           .setModes("Windows Click", "Packet")
           .setDefaultModeIndex(1)
           .setVisibility(() -> !motionMode.getCurrentMode().equals("Silent"))
           .build()
           .getModeValue();

   private final AddonsValue containerSelect = ValueBuilder.create(this, "Container Select")
           .setAddonsModes("Chest", "Double Chest", "Ender Chest", "Brewing Stand", "Furnace")
           .setDefaultSelectedAddons()
           .build()
           .getAddonsValue();

   {
      minDelay.linkAsMin(maxDelay);
      maxDelay.linkAsMax(minDelay);
   }

   private Screen lastTickScreen;

   public static boolean isWorking() {
      return !timer.delay(3);
   }

   @EventTarget
   public void onRunTicks(EventUpdate e) {
      if (instant.getCurrentValue()) {
         setSuffix("Extra Packet");
      } else {
         setSuffix(this.minDelay.getCurrentValue() + " | " + this.maxDelay.getCurrentValue());
      }
   }

   @EventTarget(1)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) return;
      if (mc.player == null) return;
      boolean silent = false;
      Screen currentScreen = mc.screen;
      AbstractContainerMenu menu = mc.player.containerMenu;

      if (menu == null) {
         this.lastTickScreen = currentScreen;
         return;
      }
      if (currentScreen != this.lastTickScreen) {
         timer1.reset();
         timer2.reset();
      }
      String title;
      boolean isSilent = motionMode.getCurrentMode().equals("Silent");

      if (isSilent) {
         title = "";
      } else if (currentScreen instanceof ContainerScreen cs) {
         title = cs.getTitle().getString();
      } else {
         this.lastTickScreen = currentScreen;
         return;
      }

      String chest = Component.translatable("container.chest").getString();
      String largeChest = Component.translatable("container.chestDouble").getString();
      String enderChest = Component.translatable("container.enderchest").getString();
      String brewingStand = Component.translatable("container.brewing").getString();
      String furnace = Component.translatable("container.furnace").getString();

      boolean isChest = title.equals(chest) || title.equals("Chest");
      boolean isDoubleChest = title.equals(largeChest);
      boolean isEnderChest = title.equals(enderChest);
      boolean isBrewingStand = title.equals(brewingStand);
      boolean isFurnace = title.equals(furnace);

      boolean allowed = (containerSelect.isSelected("Chest") && isChest)
              || (containerSelect.isSelected("Double Chest") && isDoubleChest)
              || (containerSelect.isSelected("Ender Chest") && isEnderChest)
              || (containerSelect.isSelected("Brewing Stand") && isBrewingStand)
              || (containerSelect.isSelected("Furnace") && isFurnace);

      if (!allowed) {
         this.lastTickScreen = currentScreen;
         return;
      }
      Container container = null;
      int size = 0;

      if (menu instanceof ChestMenu chestMenu) {
         size = chestMenu.getRowCount() * 9;
      } else if (menu instanceof FurnaceMenu furnaceMenu) {
         container = ((AbstractFurnaceMenuAccessor) furnaceMenu).getContainer();
         size = container.getContainerSize();
      } else if (menu instanceof BrewingStandMenu brewingMenu) {
         container = ((BrewingStandMenuAccessor) brewingMenu).getBrewingStand();
         size = container.getContainerSize();
      }

      if (size == 0) {
         this.lastTickScreen = currentScreen;
         return;
      }

      boolean isEmpty = container != null ? isContainerEmpty(container) : isChestEmpty((ChestMenu) menu);
      if (isEmpty && timer2.delay(this.stopDelay.getCurrentValue())) {
         if (isSilent) {
            mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
         } else {
            mc.player.closeContainer();
         }
         timer2.reset();
         this.lastTickScreen = currentScreen;
         return;
      }

      if (isEmpty) {
         this.lastTickScreen = currentScreen;
         return;
      }
      if (menu instanceof ChestMenu chestMenu) {

         if (instant.getCurrentValue() && timer1.delay(this.startDelay.getCurrentValue())) {
            List<Integer> usefulSlots = new ArrayList<>();
            for (int i = 0; i < ((ChestMenu) menu).getRowCount() * 9; i++) {
               ItemStack stack = menu.getSlot(i).getItem();
               if (!stack.isEmpty() && (pickTrash.getCurrentValue() || (isItemUseful(stack) && isBestItemInChest((ChestMenu) menu, stack)))) {
                  usefulSlots.add(i);
               }
            }
            for (int slotId : usefulSlots) {
               if (silent) sendClickPacket(menu.containerId, slotId);
               else clickSlot(menu, slotId);
            }
            if (usefulSlots.isEmpty() || isChestEmpty((ChestMenu) menu)) timer1.reset();
         } else if (!instant.getCurrentValue()) {
            List<Integer> slots = IntStream.range(0, ((ChestMenu) menu).getRowCount() * 9).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);
            for (int slotId : slots) {
               ItemStack stack = menu.getSlot(slotId).getItem();
               if (!stack.isEmpty() && (pickTrash.getCurrentValue() || (isItemUseful(stack) && isBestItemInChest((ChestMenu) menu, stack)))
                       && timer1.delay(getDelay()) && timer1.delay(this.startDelay.getCurrentValue())) {
                  if (silent) sendClickPacket(menu.containerId, slotId);
                  else clickSlot(menu, slotId);
                  timer1.reset();
                  break;
               }
            }
         }
      } else {
         if (instant.getCurrentValue() && timer1.delay(this.startDelay.getCurrentValue())) {
            for (int i = 0; i < size; i++) {
               ItemStack stack = container.getItem(i);
               if (!stack.isEmpty() && (pickTrash.getCurrentValue() || isItemUseful(stack))) {
                  if (silent) sendClickPacket(menu.containerId, i);
                  else clickSlot(menu, i);
               }
            }
            if (isContainerEmpty(container)) timer1.reset();
         } else if (!instant.getCurrentValue()) {
            List<Integer> slots = IntStream.range(0, size).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);
            for (int slotId : slots) {
               ItemStack stack = container.getItem(slotId);
               if (!stack.isEmpty() && (pickTrash.getCurrentValue() || isItemUseful(stack))
                       && timer1.delay(getDelay()) && timer1.delay(this.startDelay.getCurrentValue())) {
                  if (silent) sendClickPacket(menu.containerId, slotId);
                  else clickSlot(menu, slotId);
                  timer1.reset();
                  break;
               }
            }
         }
      }

      this.lastTickScreen = currentScreen;
   }

   private void sendClickPacket(int containerId, int slotId) {
      int stateId = mc.player.containerMenu != null ? mc.player.containerMenu.getStateId() : 0;
      ItemStack carriedItem = mc.player.containerMenu.getCarried().copy();
      Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
      mc.player.connection.send(new ServerboundContainerClickPacket(containerId, stateId, slotId, 0, ClickType.QUICK_MOVE, carriedItem, changedSlots));
   }

   private void clickSlot(AbstractContainerMenu menu, int slotId) {
      if (clickMode.getCurrentMode().equals("Packet")) {
         sendClickPacket(menu.containerId, slotId);
      } else {
         if (swap.getCurrentValue()) {
            int slot = getFirstEmptySlot();
            if (slot != -1 && slot + 18 < 54) {
               if (slot < 9) {
                  mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, slot, ClickType.SWAP, mc.player);
               } else {
                  mc.gameMode.handleInventoryMouseClick(menu.containerId, slot + 18, 8, ClickType.SWAP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, 8, ClickType.SWAP, mc.player);
               }
            } else {
               mc.player.closeContainer();
            }
         } else {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, 0, ClickType.QUICK_MOVE, mc.player);
         }
      }
   }

   private boolean isContainerEmpty(Container container) {
      for (int i = 0; i < container.getContainerSize(); i++) {
         ItemStack item = container.getItem(i);
         if (!item.isEmpty()) {
            if (pickTrash.getCurrentValue()) return false;
            if (isItemUseful(item)) return false;
         }
      }
      return true;
   }

   public static int getFirstEmptySlot() {
      Inventory inventory = ContainerStealer.mc.player.getInventory();
      for (int i = 0; i < inventory.items.size(); ++i) {
         if (i == 8 || !inventory.getItem(i).isEmpty()) continue;
         return i;
      }
      return -1;
   }

   private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
      if (!InventoryUtils.isGodItem(stack) && !InventoryUtils.isSharpnessAxe(stack)) {
         for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack checkStack = menu.getSlot(i).getItem();
            if (stack.getItem() instanceof ArmorItem && checkStack.getItem() instanceof ArmorItem) {
               ArmorItem item = (ArmorItem) stack.getItem();
               ArmorItem checkItem = (ArmorItem) checkStack.getItem();
               if (item.getEquipmentSlot() == checkItem.getEquipmentSlot() && InventoryUtils.getProtection(checkStack) > InventoryUtils.getProtection(stack)) {
                  return false;
               }
            } else if (stack.getItem() instanceof SwordItem && checkStack.getItem() instanceof SwordItem) {
               if (InventoryUtils.getSwordDamage(checkStack) > InventoryUtils.getSwordDamage(stack)) return false;
            } else if (stack.getItem() instanceof PickaxeItem && checkStack.getItem() instanceof PickaxeItem) {
               if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) return false;
            } else if (stack.getItem() instanceof AxeItem && checkStack.getItem() instanceof AxeItem) {
               if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) return false;
            } else if (stack.getItem() instanceof ShovelItem && checkStack.getItem() instanceof ShovelItem) {
               if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) return false;
            }
         }
         return true;
      } else {
         return true;
      }
   }

   private boolean isChestEmpty(ChestMenu menu) {
      for (int i = 0; i < menu.getRowCount() * 9; i++) {
         ItemStack item = menu.getSlot(i).getItem();
         if (!item.isEmpty()) {
            if (pickTrash.getCurrentValue()) return false;
            if (isItemUseful(item) && this.isBestItemInChest(menu, item)) return false;
         }
      }
      return true;
   }

   public static boolean isItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
         return true;
      } else if (stack.getItem() instanceof ArmorItem) {
         ArmorItem item = (ArmorItem) stack.getItem();
         float protection = InventoryUtils.getProtection(stack);
         float bestArmor = InventoryUtils.getBestArmorScore(item.getEquipmentSlot());
         return !(protection <= bestArmor);
      } else if (stack.getItem() instanceof SwordItem) {
         float damage = InventoryUtils.getSwordDamage(stack);
         float bestDamage = InventoryUtils.getBestSwordDamage();
         return !(damage <= bestDamage);
      } else if (stack.getItem() instanceof PickaxeItem) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestPickaxeScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof AxeItem) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestAxeScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof ShovelItem) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestShovelScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof CrossbowItem) {
         float score = InventoryUtils.getCrossbowScore(stack);
         float bestScore = InventoryUtils.getBestCrossbowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
         float score = InventoryUtils.getPunchBowScore(stack);
         float bestScore = InventoryUtils.getBestPunchBowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
         float score = InventoryUtils.getPowerBowScore(stack);
         float bestScore = InventoryUtils.getBestPowerBowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() == Items.COMPASS) {
         return !InventoryUtils.hasItem(stack.getItem());
      } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) >= InventoryManager.getWaterBucketCount()) {
         return false;
      } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) >= InventoryManager.getLavaBucketCount()) {
         return false;
      } else if (stack.getItem() instanceof BlockItem
              && Scaffold.isValidStack(stack)
              && InventoryUtils.getBlockCountInInventory() + stack.getCount() >= InventoryManager.getMaxBlockSize()) {
         return false;
      } else if (stack.getItem() == Items.ARROW && InventoryUtils.getItemCount(Items.ARROW) + stack.getCount() >= InventoryManager.getMaxArrowSize()) {
         return false;
      } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) >= 1) {
         return false;
      } else if (stack.getItem() != Items.SNOWBALL && stack.getItem() != Items.EGG
              || InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG) + stack.getCount() < InventoryManager.getMaxProjectileSize()
              && InventoryManager.shouldKeepProjectile()) {
         return stack.getItem() instanceof ItemNameBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
      } else {
         return false;
      }
   }

   private int getDelay() {
      return MathUtils.getRandomIntInRange((int) minDelay.getCurrentValue(), (int) maxDelay.getCurrentValue() + 1);
   }
}