package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.TickTimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
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
           .setAddonsModes("Chest", "Double Chest", "Ender Chest", "Brewing Stand", "Furnace", "Barrel", "Shulker Box", "Dispenser", "Hopper")
           .setDefaultSelectedAddons()
           .build()
           .getAddonsValue();

   {
      minDelay.linkAsMin(maxDelay);
      maxDelay.linkAsMax(minDelay);
   }

   private Screen lastTickScreen;
   private int lastContainerId = -1;

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

      if (menu == null || menu == mc.player.inventoryMenu) {
         this.lastTickScreen = currentScreen;
         this.lastContainerId = -1;
         return;
      }
      if (currentScreen != this.lastTickScreen || menu.containerId != this.lastContainerId) {
         timer1.reset();
         timer2.reset();
      }
      String title;
      boolean isSilent = motionMode.getCurrentMode().equals("Silent");

      if (isSilent) {
         title = "";
      } else if (currentScreen instanceof AbstractContainerScreen<?> screen) {
         title = screen.getTitle().getString();
      } else {
         remember(currentScreen, menu);
         return;
      }

      ContainerInfo containerInfo = getContainerInfo(menu, title, isSilent);
      if (!containerInfo.allowed() || containerInfo.size() == 0) {
         remember(currentScreen, menu);
         return;
      }

      boolean isEmpty = isContainerEmpty(menu, containerInfo);
      if (isEmpty && timer2.delay(this.stopDelay.getCurrentValue())) {
         if (isSilent) {
            mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
            mc.player.clientSideCloseContainer();
         } else {
            mc.player.closeContainer();
         }
         timer.reset();
         timer2.reset();
         remember(currentScreen, menu);
         return;
      }

      if (isEmpty) {
         remember(currentScreen, menu);
         return;
      }

      if (instant.getCurrentValue() && timer1.delay(this.startDelay.getCurrentValue())) {
         List<Integer> usefulSlots = new ArrayList<>();
         for (int i = 0; i < containerInfo.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty() && shouldSteal(menu, containerInfo, stack)) {
               usefulSlots.add(i);
            }
         }
         for (int slotId : usefulSlots) {
            if (silent) sendClickPacket(menu.containerId, slotId);
            else clickSlot(menu, slotId);
         }
         if (!usefulSlots.isEmpty()) {
            timer.reset();
         }
         if (usefulSlots.isEmpty() || isContainerEmpty(menu, containerInfo)) timer1.reset();
      } else if (!instant.getCurrentValue()) {
         List<Integer> slots = IntStream.range(0, containerInfo.size()).boxed().collect(Collectors.toList());
         Collections.shuffle(slots);
         for (int slotId : slots) {
            ItemStack stack = menu.getSlot(slotId).getItem();
            if (!stack.isEmpty() && shouldSteal(menu, containerInfo, stack)
                    && timer1.delay(getDelay()) && timer1.delay(this.startDelay.getCurrentValue())) {
               if (silent) sendClickPacket(menu.containerId, slotId);
               else clickSlot(menu, slotId);
               timer.reset();
               timer1.reset();
               break;
            }
         }
      }

      remember(currentScreen, menu);
   }

   private void sendClickPacket(int containerId, int slotId) {
      int stateId = mc.player.containerMenu != null ? mc.player.containerMenu.getStateId() : 0;
      ItemStack carriedItem = mc.player.containerMenu != null ? mc.player.containerMenu.getCarried().copy() : ItemStack.EMPTY;
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

   private boolean shouldSteal(AbstractContainerMenu menu, ContainerInfo info, ItemStack stack) {
      if (pickTrash.getCurrentValue()) {
         return true;
      }
      if (!isItemUseful(stack)) {
         return false;
      }
      return !info.chestLike() || !(menu instanceof ChestMenu chestMenu) || isBestItemInChest(chestMenu, stack);
   }

   private boolean isContainerEmpty(AbstractContainerMenu menu, ContainerInfo info) {
      for (int i = 0; i < info.size(); i++) {
         ItemStack item = menu.getSlot(i).getItem();
         if (!item.isEmpty()) {
            if (pickTrash.getCurrentValue()) return false;
            if (isItemUseful(item) && (!info.chestLike() || !(menu instanceof ChestMenu chestMenu) || isBestItemInChest(chestMenu, item))) return false;
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

   private ContainerInfo getContainerInfo(AbstractContainerMenu menu, String title, boolean silent) {
      if (menu instanceof ChestMenu chestMenu) {
         int rows = chestMenu.getRowCount();
         boolean doubleChest = rows >= 6;
         boolean titleKnown = title != null && !title.isEmpty();
         boolean titleChest = titleKnown && (title.equals(Component.translatable("container.chest").getString()) || title.equals("Chest"));
         boolean titleDoubleChest = titleKnown && title.equals(Component.translatable("container.chestDouble").getString());
         boolean titleEnderChest = titleKnown && title.equals(Component.translatable("container.enderchest").getString());
         boolean titleBarrel = titleKnown && title.equals(Component.translatable("container.barrel").getString());
         boolean titleShulker = titleKnown && title.equals(Component.translatable("container.shulkerBox").getString());

         boolean allowed;
         if (silent || !titleKnown) {
            allowed = doubleChest
                    ? containerSelect.isSelected("Double Chest")
                    : containerSelect.isSelected("Chest")
                    || containerSelect.isSelected("Ender Chest")
                    || containerSelect.isSelected("Barrel")
                    || containerSelect.isSelected("Shulker Box");
         } else {
            allowed = (containerSelect.isSelected("Chest") && titleChest)
                    || (containerSelect.isSelected("Double Chest") && titleDoubleChest)
                    || (containerSelect.isSelected("Ender Chest") && titleEnderChest)
                    || (containerSelect.isSelected("Barrel") && titleBarrel)
                    || (containerSelect.isSelected("Shulker Box") && titleShulker);
         }
         return new ContainerInfo(allowed, rows * 9, true);
      }

      if (menu instanceof BrewingStandMenu) {
         String brewingStand = Component.translatable("container.brewing").getString();
         boolean allowed = containerSelect.isSelected("Brewing Stand") && (silent || title == null || title.isEmpty() || title.equals(brewingStand));
         return new ContainerInfo(allowed, 5, false);
      }

      if (menu instanceof AbstractFurnaceMenu) {
         String furnace = Component.translatable("container.furnace").getString();
         String blastFurnace = Component.translatable("container.blast_furnace").getString();
         String smoker = Component.translatable("container.smoker").getString();
         boolean allowed = containerSelect.isSelected("Furnace")
                 && (silent || title == null || title.isEmpty() || title.equals(furnace) || title.equals(blastFurnace) || title.equals(smoker));
         return new ContainerInfo(allowed, 3, false);
      }

      int containerSlots = Math.max(0, menu.slots.size() - Inventory.INVENTORY_SIZE);
      boolean titleBarrel = title != null && title.equals(Component.translatable("container.barrel").getString());
      boolean titleShulker = title != null && title.equals(Component.translatable("container.shulkerBox").getString());
      boolean titleDispenser = title != null && (title.equals(Component.translatable("container.dispenser").getString()) || title.equals(Component.translatable("container.dropper").getString()));
      boolean titleHopper = title != null && title.equals(Component.translatable("container.hopper").getString());
      boolean allowed = (containerSelect.isSelected("Barrel") && (silent || titleBarrel) && containerSlots == 27)
              || (containerSelect.isSelected("Shulker Box") && (silent || titleShulker) && containerSlots == 27)
              || (containerSelect.isSelected("Dispenser") && (silent || titleDispenser) && containerSlots == 9)
              || (containerSelect.isSelected("Hopper") && (silent || titleHopper) && containerSlots == 5);
      boolean chestLike = (containerSelect.isSelected("Barrel") && (silent || titleBarrel) && containerSlots == 27)
              || (containerSelect.isSelected("Shulker Box") && (silent || titleShulker) && containerSlots == 27);
      return new ContainerInfo(allowed, containerSlots, chestLike);
   }

   private void remember(Screen screen, AbstractContainerMenu menu) {
      this.lastTickScreen = screen;
      this.lastContainerId = menu == null ? -1 : menu.containerId;
   }

   private record ContainerInfo(boolean allowed, int size, boolean chestLike) {
   }
}
