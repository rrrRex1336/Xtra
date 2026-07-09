package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.ui.ClickGUI;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.utils.TickTimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.apache.commons.lang3.tuple.Pair;

@ModuleInfo(
   name = "InventoryManager",
   description = "Automatically manage your inventory",
   category = Category.PLAYER
)
public class InventoryManager extends Module {
   private static final TickTimeHelper timer = new TickTimeHelper();
   private final FloatValue minDelay = ValueBuilder.create(this, "Min Delay")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   private final FloatValue maxDelay = ValueBuilder.create(this, "Max Delay")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   ModeValue offhandItems = ValueBuilder.create(this, "Offhand Items")
      .setModes("None", "Golden Apple", "Projectile", "Fishing Rod", "Block")
      .build()
      .getModeValue();
   BooleanValue autoArmor = ValueBuilder.create(this, "Auto Armor").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue inventoryOnly = ValueBuilder.create(this, "Inventory Only").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue noMove = ValueBuilder.create(this, "No Move")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   BooleanValue silentNoSprint = ValueBuilder.create(this, "Silent No Sprint")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> !this.inventoryOnly.getCurrentValue() && !this.noMove.getCurrentValue())
           .build()
           .getBooleanValue();
   BooleanValue switchSword = ValueBuilder.create(this, "Switch Sword").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue swordSlot = ValueBuilder.create(this, "Sword Slot")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchSword.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBlock = ValueBuilder.create(this, "Switch Block")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Block"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue blockSlot = ValueBuilder.create(this, "Block Slot")
      .setDefaultFloatValue(2.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue() && !this.offhandItems.isCurrentMode("Block"))
      .build()
      .getFloatValue();
   FloatValue maxBlockSize = ValueBuilder.create(this, "Max Block Size")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchPickaxe = ValueBuilder.create(this, "Switch Pickaxe").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue pickaxeSlot = ValueBuilder.create(this, "Pickaxe Slot")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchPickaxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchAxe = ValueBuilder.create(this, "Switch Axe").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue axeSlot = ValueBuilder.create(this, "Axe Slot")
      .setDefaultFloatValue(4.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchAxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchMace = ValueBuilder.create(this, "Switch Mace").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue maceSlot = ValueBuilder.create(this, "Mace Slot")
      .setDefaultFloatValue(4.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchMace.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBow = ValueBuilder.create(this, "Switch Bow or Crossbow").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue bowSlot = ValueBuilder.create(this, "Bow Slot")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   ModeValue preferBow = ValueBuilder.create(this, "Bow Priority")
      .setModes("Crossbow", "Power Bow", "Punch Bow")
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getModeValue();
   FloatValue maxArrowSize = ValueBuilder.create(this, "Max Arrow Size")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchWaterBucket = ValueBuilder.create(this, "Switch Water Bucket").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketSlot = ValueBuilder.create(this, "Water Bucket Slot")
      .setDefaultFloatValue(6.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchWaterBucket.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchEnderPearl = ValueBuilder.create(this, "Switch Ender Pearl").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue enderPearlSlot = ValueBuilder.create(this, "Ender Pearl Slot")
      .setDefaultFloatValue(7.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchEnderPearl.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchFireball = ValueBuilder.create(this, "Switch Fireball").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fireballSlot = ValueBuilder.create(this, "Fireball Slot")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchFireball.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchGoldenApple = ValueBuilder.create(this, "Switch Golden Apple")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Golden Apple"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue goldenAppleSlot = ValueBuilder.create(this, "Golden Apple Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple"))
      .build()
      .getFloatValue();
   BooleanValue throwItems = ValueBuilder.create(this, "Throw Items").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketCount = ValueBuilder.create(this, "Keep Water Buckets")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue lavaBucketCount = ValueBuilder.create(this, "Keep Lava Buckets")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue keepProjectile = ValueBuilder.create(this, "Keep Eggs & Snowballs").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue switchProjectile = ValueBuilder.create(this, "Switch Eggs & Snowballs")
      .setDefaultBooleanValue(false)
      .setVisibility(() -> this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
      .build()
      .getBooleanValue();
   FloatValue projectileSlot = ValueBuilder.create(this, "Eggs & Snowballs Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchProjectile.getCurrentValue() && this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
      .build()
      .getFloatValue();
   FloatValue maxProjectileSize = ValueBuilder.create(this, "Max Eggs & Snowballs Size")
      .setDefaultFloatValue(64.0F)
      .setFloatStep(16.0F)
      .setMinFloatValue(16.0F)
      .setMaxFloatValue(256.0F)
      .setVisibility(() -> this.keepProjectile.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchRod = ValueBuilder.create(this, "Switch Rod")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Fishing Rod"))
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   FloatValue rodSlot = ValueBuilder.create(this, "Rod Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod"))
      .build()
      .getFloatValue();
   int noMoveTicks = 0;
   private boolean clickOffHand = false;
   private boolean inventoryOpen = false;
   private boolean silentNoSprintWasSprinting = false;
   private boolean silentNoSprintWasKeySprintDown = false;
   private float currentDelay = -1.0F;

   {
      minDelay.linkAsMin(maxDelay);
      maxDelay.linkAsMax(minDelay);
   }

   public static int getMaxBlockSize() {
      return (int)((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).maxBlockSize.getCurrentValue();
   }

   public static boolean shouldKeepProjectile() {
      return ((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).keepProjectile.getCurrentValue();
   }

   public static int getMaxProjectileSize() {
      return (int)((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).maxProjectileSize.getCurrentValue();
   }

   public static int getMaxArrowSize() {
      return (int)((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).maxArrowSize.getCurrentValue();
   }

   public static int getWaterBucketCount() {
      return (int)((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).waterBucketCount.getCurrentValue();
   }

   public static int getLavaBucketCount() {
      return (int)((InventoryManager)Naven.getInstance().getModuleManager().getModule(InventoryManager.class)).lavaBucketCount.getCurrentValue();
   }

   @Override
   public void onDisable() {
      this.releaseSilentNoSprintIfIdle();
      this.silentNoSprintWasSprinting = false;
      this.silentNoSprintWasKeySprintDown = false;
   }

   public boolean isItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (InventoryUtils.isGodItem(stack)) {
         return true;
      } else if (InventoryUtils.isMace(stack)) {
         return true;
      } else if (InventoryUtils.isWindCharge(stack)) {
         return true;
      } else if (InventoryUtils.isSpear(stack)) {
         return true;
      } else if (stack.toHoverableText().getString().contains("点击使用")) {
         return true;
      } else if (stack.getItem() instanceof ArmorItem) {
         ArmorItem item = (ArmorItem)stack.getItem();
         float protection = InventoryUtils.getProtection(stack);
         if (InventoryUtils.getCurrentArmorScore(item.getSlotType()) >= protection) {
            return false;
         } else {
            float bestArmor = InventoryUtils.getBestArmorScore(item.getSlotType());
            return !(protection < bestArmor);
         }
      } else if (stack.getItem() instanceof SwordItem) {
         return InventoryUtils.getBestSword() == stack;
      } else if (stack.getItem() instanceof PickaxeItem) {
         return InventoryUtils.getBestPickaxe() == stack;
      } else if (stack.getItem() instanceof AxeItem && !InventoryUtils.isSharpnessAxe(stack)) {
         return InventoryUtils.getBestAxe() == stack;
      } else if (stack.getItem() instanceof ShovelItem) {
         return InventoryUtils.getBestShovel() == stack;
      } else if (stack.getItem() instanceof CrossbowItem) {
         return InventoryUtils.getBestCrossbow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
         return InventoryUtils.getBestPunchBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
         return InventoryUtils.getBestPowerBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.getItemCount(Items.BOW) > 1) {
         return false;
      } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) > getWaterBucketCount()) {
         return false;
      } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) > getLavaBucketCount()) {
         return false;
      } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) > 1) {
         return false;
      } else if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) && !InventoryUtils.isWindCharge(stack) && !shouldKeepProjectile()) {
         return false;
      } else {
         return stack.getItem() instanceof AliasedBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.SEND) {
         if (e.getPacket() instanceof CloseHandledScreenC2SPacket) {
            this.inventoryOpen = false;
         }

         if (this.inventoryOpen && !this.inventoryOnly.getCurrentValue()) {
            if (e.getPacket() instanceof PlayerMoveC2SPacket) {
               if (MoveUtils.isMoving()) {
                  mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
               }
            } else if (e.getPacket() instanceof PlayerInteractBlockC2SPacket
               || e.getPacket() instanceof PlayerInteractItemC2SPacket
               || e.getPacket() instanceof PlayerInteractEntityC2SPacket
               || e.getPacket() instanceof PlayerActionC2SPacket) {
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            }
         }
      }
   }

   private boolean checkConfig() {
      List<Pair<BooleanValue, FloatValue>> pairs = new ArrayList<>();
      if (!this.keepProjectile.getCurrentValue()) {
         this.switchProjectile.setCurrentValue(false);
      }

      pairs.add(Pair.of(this.switchSword, this.swordSlot));
      pairs.add(Pair.of(this.switchPickaxe, this.pickaxeSlot));
      pairs.add(Pair.of(this.switchAxe, this.axeSlot));
      pairs.add(Pair.of(this.switchMace, this.maceSlot));
      pairs.add(Pair.of(this.switchBow, this.bowSlot));
      pairs.add(Pair.of(this.switchWaterBucket, this.waterBucketSlot));
      pairs.add(Pair.of(this.switchEnderPearl, this.enderPearlSlot));
      pairs.add(Pair.of(this.switchFireball, this.fireballSlot));
      if (!this.offhandItems.isCurrentMode("Golden Apple")) {
         pairs.add(Pair.of(this.switchGoldenApple, this.goldenAppleSlot));
      }

      if (!this.offhandItems.isCurrentMode("Projectile")) {
         pairs.add(Pair.of(this.switchProjectile, this.projectileSlot));
      }

      if (!this.offhandItems.isCurrentMode("Fishing Rod")) {
         pairs.add(Pair.of(this.switchRod, this.rodSlot));
      }

      if (!this.offhandItems.isCurrentMode("Block")) {
         pairs.add(Pair.of(this.switchBlock, this.blockSlot));
      }

      Set<Integer> usedSlot = new HashSet<>();

      for (Pair<BooleanValue, FloatValue> pair : pairs) {
         if (((BooleanValue)pair.getKey()).getCurrentValue()) {
            int targetSlot = (int)(((FloatValue)pair.getValue()).getCurrentValue() - 1.0F);
            if (usedSlot.contains(targetSlot)) {
               return false;
            }

            usedSlot.add(targetSlot);
         }
      }

      return true;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (!(mc.currentScreen instanceof ClickGUI) && !this.checkConfig()) {
            Notification notification = new Notification(
               NotificationLevel.ERROR, "Duplicate slot config in Inventory Manager! Please check your config!", 8000L
            );
            Naven.getInstance().getNotificationManager().addNotification(notification);
            this.toggle();
            return;
         }

         if (InventoryUtils.shouldDisableFeatures()) {
            this.releaseSilentNoSprintIfIdle();
            return;
         }

         if (MoveUtils.isMoving()) {
            this.noMoveTicks = 0;
         } else {
            this.noMoveTicks++;
         }

         if (ContainerStealer.isWorking()
                 || Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                 || (this.inventoryOnly.getCurrentValue() ? !(mc.currentScreen instanceof InventoryScreen) : (this.noMove.getCurrentValue() ? this.noMoveTicks <= 1 : false))) {
            this.clickOffHand = false;
            this.releaseSilentNoSprintIfIdle();
            return;
         }

         if (mc.currentScreen instanceof HandledScreen container && container.getScreenHandler().syncId != mc.player.playerScreenHandler.syncId) {
            this.releaseSilentNoSprintIfIdle();
            return;
         }

         if (this.shouldHoldSilentNoSprint()) {
            this.stopSilentSprint();
         } else {
            this.releaseSilentNoSprintIfIdle();
         }

         if (this.autoArmor.getCurrentValue()) {
            for (int i = 0; i < mc.player.getInventory().armor.size(); i++) {
               ItemStack stack = (ItemStack)mc.player.getInventory().armor.get(i);
               if (stack.getItem() instanceof ArmorItem) {
                  ArmorItem item = (ArmorItem)stack.getItem();
                  if (!stack.isEmpty()
                     && this.canClickInventory()
                     && InventoryUtils.getBestArmorScore(item.getSlotType()) > InventoryUtils.getProtection(stack)) {
                     this.prepareSilentNoSprintClick();
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 4 + (4 - i), 1, SlotActionType.THROW, mc.player);
                     this.inventoryOpen = true;
                     this.resetClickTimer();
                  }
               }
            }

            for (int ix = 0; ix < mc.player.getInventory().main.size(); ix++) {
               ItemStack stack = (ItemStack)mc.player.getInventory().main.get(ix);
               if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                  ArmorItem item = (ArmorItem)stack.getItem();
                  float currentItemScore = InventoryUtils.getProtection(stack);
                  boolean isBestItem = InventoryUtils.getBestArmorScore(item.getSlotType()) == currentItemScore;
                  boolean isBetterItem = InventoryUtils.getCurrentArmorScore(item.getSlotType()) < currentItemScore;
                  if (isBestItem && isBetterItem && this.canClickInventory()) {
                     this.prepareSilentNoSprintClick();
                     if (ix < 9) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, ix + 36, 0, SlotActionType.QUICK_MOVE, mc.player);
                     } else {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, ix, 0, SlotActionType.QUICK_MOVE, mc.player);
                     }

                     this.inventoryOpen = true;
                     this.resetClickTimer();
                  }
               }
            }
         }

         if (this.clickOffHand && this.canClickInventory()) {
            this.prepareSilentNoSprintClick();
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            this.inventoryOpen = true;
            this.clickOffHand = false;
            this.resetClickTimer();
         }

         if (this.offhandItems.isCurrentMode("Golden Apple")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offHand.get(0);
            int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
            if (slot != -1 && this.canClickInventory()) {
               if (offHand.getItem() == Items.GOLDEN_APPLE) {
                  ItemStack goldenAppleStack = (ItemStack)mc.player.getInventory().main.get(slot);
                  if (offHand.getCount() + goldenAppleStack.getCount() <= 64) {
                     this.prepareSilentNoSprintClick();
                     if (slot < 9) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 0, SlotActionType.PICKUP, mc.player);
                     } else {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
                     }

                     this.inventoryOpen = true;
                     this.clickOffHand = true;
                     this.resetClickTimer();
                  }
               } else {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("Projectile")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offHand.get(0);
            ItemStack bestProjectile = InventoryUtils.getBestProjectile();
            if (bestProjectile != null) {
               int slot = InventoryUtils.getItemStackSlot(bestProjectile);
               boolean shouldSwap = false;
               if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
                  shouldSwap = true;
               } else if (offHand.getCount() < bestProjectile.getCount()) {
                  shouldSwap = true;
               }

               if (shouldSwap && slot != -1 && this.canClickInventory()) {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("Fishing Rod")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offHand.get(0);
            int slotx = InventoryUtils.getItemSlot(Items.FISHING_ROD);
            if (slotx != -1 && this.canClickInventory() && offHand.getItem() != Items.FISHING_ROD) {
               this.swapOffHand(slotx);
            }
         } else if (this.offhandItems.isCurrentMode("Block")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offHand.get(0);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null) {
               int slotx = InventoryUtils.getItemStackSlot(bestBlock);
               boolean shouldSwapx = false;
               if (Scaffold.isValidStack(offHand)) {
                  if (offHand.getCount() < bestBlock.getCount()) {
                     shouldSwapx = true;
                  }
               } else {
                  shouldSwapx = true;
               }

               if (shouldSwapx && slotx != -1 && this.canClickInventory()) {
                  this.swapOffHand(slotx);
               }
            }
         }

         if (this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple")) {
            this.swapItem((int)(this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE);
         }

         if (this.switchBlock.getCurrentValue()) {
            int blockSlot = (int)(this.blockSlot.getCurrentValue() - 1.0F);
            ItemStack currentBlock = (ItemStack)mc.player.getInventory().main.get(blockSlot);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null
               && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
               && !this.offhandItems.isCurrentMode("Block")) {
               this.swapItem(blockSlot, bestBlock);
            }

            if ((float)InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue()) {
               ItemStack worstBlock = InventoryUtils.getWorstBlock();
               this.throwItem(worstBlock);
            }
         }

         if (this.switchSword.getCurrentValue()) {
            int slotxx = (int)(this.swordSlot.getCurrentValue() - 1.0F);
            ItemStack currentSword = (ItemStack)mc.player.getInventory().main.get(slotxx);
            ItemStack bestSword = InventoryUtils.getBestSword();
            ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
            if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
               bestSword = bestShapeAxe;
            }

            if (bestSword != null) {
               float currentDamage = currentSword.getItem() instanceof SwordItem
                  ? InventoryUtils.getSwordDamage(currentSword)
                  : InventoryUtils.getAxeDamage(currentSword);
               float bestWeaponDamage = bestSword.getItem() instanceof SwordItem
                  ? InventoryUtils.getSwordDamage(bestSword)
                  : InventoryUtils.getAxeDamage(bestSword);
               if (bestWeaponDamage > currentDamage) {
                  this.swapItem(slotxx, bestSword);
               }
            }
         }

         if (this.switchPickaxe.getCurrentValue()) {
            int slotxxx = (int)(this.pickaxeSlot.getCurrentValue() - 1.0F);
            ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
            ItemStack currentPickaxe = (ItemStack)mc.player.getInventory().main.get(slotxxx);
            if (bestPickaxe != null
               && bestPickaxe.getItem() instanceof PickaxeItem
               && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe) || !(currentPickaxe.getItem() instanceof PickaxeItem))
               )
             {
               this.swapItem(slotxxx, bestPickaxe);
            }
         }

         if (this.switchAxe.getCurrentValue()) {
            int slotxxx = (int)(this.axeSlot.getCurrentValue() - 1.0F);
            ItemStack bestAxe = InventoryUtils.getBestAxe();
            ItemStack currentAxe = (ItemStack)mc.player.getInventory().main.get(slotxxx);
            if (bestAxe != null
               && bestAxe.getItem() instanceof AxeItem
               && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !(currentAxe.getItem() instanceof AxeItem))) {
               this.swapItem(slotxxx, bestAxe);
            }
         }

         if (this.switchMace.getCurrentValue()) {
            int slotxxx = (int)(this.maceSlot.getCurrentValue() - 1.0F);
            ItemStack mace = InventoryUtils.getMace();
            if (mace != null && this.shouldSwapItem(slotxxx, mace)) {
               this.swapItem(slotxxx, mace);
            }
         }

         if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod")) {
            int slotxxx = (int)(this.rodSlot.getCurrentValue() - 1.0F);
            ItemStack bestRod = InventoryUtils.getFishingRod();
            ItemStack currentRod = (ItemStack)mc.player.getInventory().main.get(slotxxx);
            if (!(currentRod.getItem() instanceof FishingRodItem)) {
               this.swapItem(slotxxx, bestRod);
            }
         }

         if (this.switchBow.getCurrentValue()) {
            int slotxxx = (int)(this.bowSlot.getCurrentValue() - 1.0F);
            ItemStack currentBow = (ItemStack)mc.player.getInventory().main.get(slotxxx);
            ItemStack bestBow;
            float bestBowScore;
            float currentBowScore;
            if (this.preferBow.isCurrentMode("Crossbow")) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            } else if (this.preferBow.isCurrentMode("Power Bow")) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            } else {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow != null && bestBowScore > currentBowScore) {
               this.swapItem(slotxxx, bestBow);
            }

            if ((float)InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue()) {
               ItemStack worstArrow = InventoryUtils.getWorstArrow();
               this.throwItem(worstArrow);
            }
         }

         if (this.switchEnderPearl.getCurrentValue()) {
            this.swapItem((int)(this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL);
         }

         if (this.switchWaterBucket.getCurrentValue()) {
            this.swapItem((int)(this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET);
         }

         if (this.switchFireball.getCurrentValue()) {
            this.swapItem((int)(this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE);
         }

         if (this.keepProjectile.getCurrentValue()) {
            if ((float)(InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()) {
               ItemStack worstProjectile = InventoryUtils.getWorstProjectile();
               this.throwItem(worstProjectile);
            }

            if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile")) {
               int projectileSlot = (int)(this.projectileSlot.getCurrentValue() - 1.0F);
               if (InventoryUtils.getItemCount(Items.EGG) > 0) {
                  this.swapItem(projectileSlot, Items.EGG);
               } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0) {
                  this.swapItem(projectileSlot, Items.SNOWBALL);
               }
            }
         }

         if (this.throwItems.getCurrentValue()) {
            List<Integer> slots = IntStream.range(0, mc.player.getInventory().main.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            for (Integer slotxxxx : slots) {
               ItemStack stack = (ItemStack)mc.player.getInventory().main.get(slotxxxx);
               if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                  this.throwItem(stack);
               }
            }
         }

         if (!this.shouldHoldSilentNoSprint()) {
            this.releaseSilentNoSprintIfIdle();
         }
      }
   }

   private boolean isSilentNoSprintEnabled() {
      return this.isEnabled()
              && this.silentNoSprint.getCurrentValue()
              && !this.inventoryOnly.getCurrentValue()
              && !this.noMove.getCurrentValue()
              && mc.player != null
              && mc.interactionManager != null;
   }

   private boolean canManageForSilentNoSprint() {
      if (!this.isSilentNoSprintEnabled() || InventoryUtils.shouldDisableFeatures()) {
         return false;
      }

      try {
         if (ContainerStealer.isWorking() || Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()) {
            return false;
         }
      } catch (Exception ignored) {
      }

      return !(mc.currentScreen instanceof HandledScreen container && container.getScreenHandler().syncId != mc.player.playerScreenHandler.syncId);
   }

   private boolean shouldHoldSilentNoSprint() {
      if (!this.canManageForSilentNoSprint()) {
         return false;
      }

      boolean pending = this.hasPendingInventoryAction();
      if (!pending) {
         return false;
      }

      float delay = this.getCurrentDelay();
      if (delay >= 4.0F) {
         return timer.delay(delay);
      }

      return true;
   }

   private boolean canClickInventory() {
      return timer.delay(this.getCurrentDelay());
   }

   private void resetClickTimer() {
      timer.reset();
      this.currentDelay = this.nextDelay();
   }

   private float getCurrentDelay() {
      float min = Math.min(this.minDelay.getCurrentValue(), this.maxDelay.getCurrentValue());
      float max = Math.max(this.minDelay.getCurrentValue(), this.maxDelay.getCurrentValue());
      if (this.currentDelay < min || this.currentDelay > max) {
         this.currentDelay = this.nextDelay();
      }
      return this.currentDelay;
   }

   private float nextDelay() {
      int min = Math.round(Math.min(this.minDelay.getCurrentValue(), this.maxDelay.getCurrentValue()));
      int max = Math.round(Math.max(this.minDelay.getCurrentValue(), this.maxDelay.getCurrentValue()));
      return MathUtils.getRandomIntInRange(min, max + 1);
   }

   public static boolean shouldStopSprintForSprintModule() {
      try {
         InventoryManager inventoryManager = (InventoryManager) Naven.getInstance().getModuleManager().getModule(InventoryManager.class);
         return inventoryManager != null && inventoryManager.shouldHoldSilentNoSprint();
      } catch (Exception ignored) {
         return false;
      }
   }

   private void prepareSilentNoSprintClick() {
      if (this.isSilentNoSprintEnabled()) {
         this.stopSilentSprint();
      }
   }

   private void stopSilentSprint() {
      if (mc.player == null) {
         return;
      }

      if (!this.silentNoSprintWasSprinting && !this.silentNoSprintWasKeySprintDown) {
         this.silentNoSprintWasSprinting = mc.player.isSprinting();
         this.silentNoSprintWasKeySprintDown = mc.options.sprintKey.isPressed();
      }

      mc.options.sprintKey.setPressed(false);
      mc.options.getSprintToggled().setValue(false);
      if (mc.player.isSprinting()) {
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
         }
         mc.player.setSprinting(false);
      }
   }

   private void releaseSilentNoSprintIfIdle() {
      if (mc.player == null || (!this.silentNoSprintWasSprinting && !this.silentNoSprintWasKeySprintDown)) {
         this.silentNoSprintWasSprinting = false;
         this.silentNoSprintWasKeySprintDown = false;
         return;
      }

      if (this.silentNoSprintWasKeySprintDown) {
         mc.options.sprintKey.setPressed(true);
      }
      if (this.silentNoSprintWasSprinting && this.canResumeSilentSprint()) {
         mc.player.setSprinting(true);
      }

      this.silentNoSprintWasSprinting = false;
      this.silentNoSprintWasKeySprintDown = false;
   }

   private boolean canResumeSilentSprint() {
      return mc.player != null
              && MoveUtils.isMoving()
              && mc.player.getHealth() > 0.0F
              && !mc.player.isTouchingWater()
              && !mc.player.isInLava()
              && !mc.player.isSneaking()
              && !mc.player.hasVehicle();
   }

   private boolean hasPendingInventoryAction() {
      try {
         if (this.autoArmor.getCurrentValue()) {
            for (int i = 0; i < mc.player.getInventory().armor.size(); i++) {
               ItemStack stack = mc.player.getInventory().armor.get(i);
               if (stack.getItem() instanceof ArmorItem item
                       && !stack.isEmpty()
                       && InventoryUtils.getBestArmorScore(item.getSlotType()) > InventoryUtils.getProtection(stack)) {
                  return true;
               }
            }

            for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
               ItemStack stack = mc.player.getInventory().main.get(i);
               if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem item) {
                  float currentItemScore = InventoryUtils.getProtection(stack);
                  if (InventoryUtils.getBestArmorScore(item.getSlotType()) == currentItemScore
                          && InventoryUtils.getCurrentArmorScore(item.getSlotType()) < currentItemScore) {
                     return true;
                  }
               }
            }
         }

         if (this.clickOffHand) {
            return true;
         }

         if (this.hasPendingOffhandAction() || this.hasPendingHotbarAction()) {
            return true;
         }

         if (this.throwItems.getCurrentValue()) {
            for (ItemStack stack : mc.player.getInventory().main) {
               if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                  return true;
               }
            }
         }
      } catch (Exception ignored) {
         return false;
      }

      return false;
   }

   private boolean hasPendingOffhandAction() {
      ItemStack offHand = mc.player.getInventory().offHand.get(0);
      if (this.offhandItems.isCurrentMode("Golden Apple")) {
         int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
         if (slot == -1) {
            return false;
         }
         if (offHand.getItem() != Items.GOLDEN_APPLE) {
            return true;
         }
         ItemStack goldenAppleStack = mc.player.getInventory().main.get(slot);
         return offHand.getCount() + goldenAppleStack.getCount() <= 64;
      }

      if (this.offhandItems.isCurrentMode("Projectile")) {
         ItemStack bestProjectile = InventoryUtils.getBestProjectile();
         if (bestProjectile == null) {
            return false;
         }
         int slot = InventoryUtils.getItemStackSlot(bestProjectile);
         return slot != -1 && (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL || offHand.getCount() < bestProjectile.getCount());
      }

      if (this.offhandItems.isCurrentMode("Fishing Rod")) {
         int slot = InventoryUtils.getItemSlot(Items.FISHING_ROD);
         return slot != -1 && offHand.getItem() != Items.FISHING_ROD;
      }

      if (this.offhandItems.isCurrentMode("Block")) {
         ItemStack bestBlock = InventoryUtils.getBestBlock();
         if (bestBlock == null) {
            return false;
         }
         int slot = InventoryUtils.getItemStackSlot(bestBlock);
         return slot != -1 && (!Scaffold.isValidStack(offHand) || offHand.getCount() < bestBlock.getCount());
      }

      return false;
   }

   private boolean hasPendingHotbarAction() {
      if (this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple")
              && this.shouldSwapItem((int)(this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE)) {
         return true;
      }

      if (this.switchBlock.getCurrentValue()) {
         int blockSlot = (int)(this.blockSlot.getCurrentValue() - 1.0F);
         ItemStack currentBlock = mc.player.getInventory().main.get(blockSlot);
         ItemStack bestBlock = InventoryUtils.getBestBlock();
         if (bestBlock != null
                 && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
                 && !this.offhandItems.isCurrentMode("Block")
                 && this.shouldSwapItem(blockSlot, bestBlock)) {
            return true;
         }
         if ((float)InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue() && InventoryUtils.getWorstBlock() != null) {
            return true;
         }
      }

      if (this.switchSword.getCurrentValue()) {
         int slot = (int)(this.swordSlot.getCurrentValue() - 1.0F);
         ItemStack currentSword = mc.player.getInventory().main.get(slot);
         ItemStack bestSword = InventoryUtils.getBestSword();
         ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
         if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
            bestSword = bestShapeAxe;
         }
         if (bestSword != null) {
            float currentDamage = currentSword.getItem() instanceof SwordItem ? InventoryUtils.getSwordDamage(currentSword) : InventoryUtils.getAxeDamage(currentSword);
            float bestWeaponDamage = bestSword.getItem() instanceof SwordItem ? InventoryUtils.getSwordDamage(bestSword) : InventoryUtils.getAxeDamage(bestSword);
            if (bestWeaponDamage > currentDamage && this.shouldSwapItem(slot, bestSword)) {
               return true;
            }
         }
      }

      if (this.switchPickaxe.getCurrentValue()) {
         int slot = (int)(this.pickaxeSlot.getCurrentValue() - 1.0F);
         ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
         ItemStack currentPickaxe = mc.player.getInventory().main.get(slot);
         if (bestPickaxe != null && bestPickaxe.getItem() instanceof PickaxeItem
                 && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe) || !(currentPickaxe.getItem() instanceof PickaxeItem))
                 && this.shouldSwapItem(slot, bestPickaxe)) {
            return true;
         }
      }

      if (this.switchAxe.getCurrentValue()) {
         int slot = (int)(this.axeSlot.getCurrentValue() - 1.0F);
         ItemStack bestAxe = InventoryUtils.getBestAxe();
         ItemStack currentAxe = mc.player.getInventory().main.get(slot);
         if (bestAxe != null && bestAxe.getItem() instanceof AxeItem
                 && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !(currentAxe.getItem() instanceof AxeItem))
                 && this.shouldSwapItem(slot, bestAxe)) {
            return true;
         }
      }

      if (this.switchMace.getCurrentValue()) {
         int slot = (int)(this.maceSlot.getCurrentValue() - 1.0F);
         ItemStack mace = InventoryUtils.getMace();
         if (mace != null && this.shouldSwapItem(slot, mace)) {
            return true;
         }
      }

      if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod")) {
         int slot = (int)(this.rodSlot.getCurrentValue() - 1.0F);
         ItemStack bestRod = InventoryUtils.getFishingRod();
         ItemStack currentRod = mc.player.getInventory().main.get(slot);
         if (bestRod != null && !(currentRod.getItem() instanceof FishingRodItem) && this.shouldSwapItem(slot, bestRod)) {
            return true;
         }
      }

      if (this.hasPendingBowAction()) {
         return true;
      }

      if (this.switchEnderPearl.getCurrentValue() && this.shouldSwapItem((int)(this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL)) {
         return true;
      }
      if (this.switchWaterBucket.getCurrentValue() && this.shouldSwapItem((int)(this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET)) {
         return true;
      }
      if (this.switchFireball.getCurrentValue() && this.shouldSwapItem((int)(this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE)) {
         return true;
      }

      if (this.keepProjectile.getCurrentValue()) {
         if ((float)(InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()
                 && InventoryUtils.getWorstProjectile() != null) {
            return true;
         }
         if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile")) {
            int projectileSlot = (int)(this.projectileSlot.getCurrentValue() - 1.0F);
            if (InventoryUtils.getItemCount(Items.EGG) > 0 && this.shouldSwapItem(projectileSlot, Items.EGG)) {
               return true;
            }
            return InventoryUtils.getItemCount(Items.SNOWBALL) > 0 && this.shouldSwapItem(projectileSlot, Items.SNOWBALL);
         }
      }

      return false;
   }

   private boolean hasPendingBowAction() {
      if (!this.switchBow.getCurrentValue()) {
         return false;
      }

      int slot = (int)(this.bowSlot.getCurrentValue() - 1.0F);
      ItemStack currentBow = mc.player.getInventory().main.get(slot);
      ItemStack bestBow = null;
      float bestBowScore = 0.0F;
      float currentBowScore = 0.0F;

      if (this.preferBow.isCurrentMode("Crossbow")) {
         bestBow = InventoryUtils.getBestCrossbow();
         bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
         currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
      } else if (this.preferBow.isCurrentMode("Power Bow")) {
         bestBow = InventoryUtils.getBestPowerBow();
         bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
         currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
      } else {
         bestBow = InventoryUtils.getBestPunchBow();
         bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
         currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
      }

      if (bestBow == null) {
         bestBow = InventoryUtils.getBestCrossbow();
         bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
         currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
      }
      if (bestBow == null) {
         bestBow = InventoryUtils.getBestPowerBow();
         bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
         currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
      }
      if (bestBow == null) {
         bestBow = InventoryUtils.getBestPunchBow();
         bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
         currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
      }

      return bestBow != null && bestBowScore > currentBowScore && this.shouldSwapItem(slot, bestBow)
              || (float)InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue() && InventoryUtils.getWorstArrow() != null;
   }

   private boolean shouldSwapItem(int targetSlot, ItemStack bestItem) {
      if (bestItem == null || targetSlot < 0 || targetSlot >= 9) {
         return false;
      }

      ItemStack currentSlot = mc.player.getInventory().main.get(targetSlot);
      return InventoryUtils.isItemValid(currentSlot) && bestItem != currentSlot && InventoryUtils.getItemStackSlot(bestItem) != -1;
   }

   private boolean shouldSwapItem(int targetSlot, Item item) {
      if (targetSlot < 0 || targetSlot >= 9) {
         return false;
      }

      ItemStack currentSlot = mc.player.getInventory().main.get(targetSlot);
      if (!InventoryUtils.isItemValid(currentSlot)) {
         return false;
      }

      int bestItemSlot = InventoryUtils.getItemSlot(item);
      if (bestItemSlot == -1) {
         return false;
      }

      ItemStack bestItemStack = mc.player.getInventory().main.get(bestItemSlot);
      return currentSlot.getItem() != item || currentSlot.getItem() == item && currentSlot.getCount() < bestItemStack.getCount();
   }

   private void swapOffHand(int slot) {
      this.prepareSilentNoSprintClick();
      if (slot < 9) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot + 36, 40, SlotActionType.SWAP, mc.player);
      } else {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
      }

      this.inventoryOpen = true;
      this.resetClickTimer();
   }

   private void throwItem(ItemStack item) {
      if (InventoryUtils.isItemValid(item) && this.canClickInventory()) {
         int itemSlot = InventoryUtils.getItemStackSlot(item);
         if (itemSlot != -1) {
            this.prepareSilentNoSprintClick();
            if (itemSlot < 9) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, itemSlot + 36, 1, SlotActionType.THROW, mc.player);
            } else {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, itemSlot, 1, SlotActionType.THROW, mc.player);
            }

            this.inventoryOpen = true;
            this.resetClickTimer();
         }
      }
   }

   private void swapItem(int targetSlot, ItemStack bestItem) {
      ItemStack currentSlot = (ItemStack)mc.player.getInventory().main.get(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && bestItem != currentSlot && this.canClickInventory()) {
         int bestItemSlot = InventoryUtils.getItemStackSlot(bestItem);
         if (bestItemSlot != -1) {
            this.prepareSilentNoSprintClick();
            if (bestItemSlot < 9) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestItemSlot + 36, targetSlot, SlotActionType.SWAP, mc.player);
            } else {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestItemSlot, targetSlot, SlotActionType.SWAP, mc.player);
            }

            this.inventoryOpen = true;
            this.resetClickTimer();
         }
      }
   }

   private void swapItem(int targetSlot, Item item) {
      ItemStack currentSlot = (ItemStack)mc.player.getInventory().main.get(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && this.canClickInventory()) {
         int bestItemSlot = InventoryUtils.getItemSlot(item);
         if (bestItemSlot != -1) {
            ItemStack bestItemStack = (ItemStack)mc.player.getInventory().main.get(bestItemSlot);
            if (currentSlot.getItem() != item || currentSlot.getItem() == item && currentSlot.getCount() < bestItemStack.getCount()) {
               this.prepareSilentNoSprintClick();
               if (bestItemSlot < 9) {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestItemSlot + 36, targetSlot, SlotActionType.SWAP, mc.player);
               } else {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestItemSlot, targetSlot, SlotActionType.SWAP, mc.player);
               }

               this.inventoryOpen = true;
               this.resetClickTimer();
            }
         }
      }
   }
}
