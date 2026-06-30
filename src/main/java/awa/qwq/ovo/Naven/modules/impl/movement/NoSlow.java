package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventSlowdown;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "NoSlow",
        description = "NoSlowDown",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
   public final ModeValue modeValue = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Grim", "Heypixel", "Grim Offhand Swap")
           .build()
           .getModeValue();

   private final ModeValue grimForm = ValueBuilder.create(this, "Grim Mode")
           .setDefaultModeIndex(0)
           .setModes("Item Switch", "Tick Slow", "Food Drop", "Blink")
           .setVisibility(() -> this.modeValue.isCurrentMode("Grim"))
           .build()
           .getModeValue();

   private final ModeValue tickSlow = ValueBuilder.create(this, "Tick Pattern")
           .setDefaultModeIndex(0)
           .setModes("1:1 Pattern", "1:3 Pattern", "Legacy Drop")
           .setVisibility(() -> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Tick Slow"))
           .build()
           .getModeValue();

   private final FloatValue legacyHeypixelTicks = ValueBuilder.create(this, "Legacy Drop Ticks")
           .setDefaultFloatValue(9.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(32.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Tick Slow") && tickSlow.isCurrentMode("Legacy Drop"))
           .build()
           .getFloatValue();

   private final ModeValue heypixelTick = ValueBuilder.create(this, "Heypixel Pattern")
           .setDefaultModeIndex(0)
           .setModes("1:3 Pattern", "3:2 Pattern", "Swap Pattern")
           .setVisibility(() -> modeValue.isCurrentMode("Heypixel"))
           .build()
           .getModeValue();

   public FloatValue slowdownTicks = ValueBuilder.create(this, "Delay Ticks")
           .setDefaultFloatValue(12.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(20.0F)
           .setFloatStep(1.0F)
           .setVisibility(()-> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Blink"))
           .build()
           .getFloatValue();

   public BooleanValue food = ValueBuilder.create(this, "Food").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue bow = ValueBuilder.create(this, "Bow").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue crossbow = ValueBuilder.create(this, "Crossbow").setDefaultBooleanValue(true).build().getBooleanValue();
   private int blinkSlowdownCounter = 0;
   private boolean blinkActive = false;
   private boolean usingActive = false;
   private final LinkedBlockingQueue<Packet<?>> movementQueue = new LinkedBlockingQueue<>();
   private int postDelayTicks = 0;
   private int maxUseDuration = 0;
   private int useTimer = 0;
   private Step step = Step.NONE;
   private int grimOffhandUseTicks = 0;
   private final Queue<Packet<ClientGamePacketListener>> delayedOffhandPackets = new ConcurrentLinkedQueue<>();
   private boolean foodDroppedThisUse = false;
   private boolean legacyEating = false;
   private boolean legacyDropSent = false;
   private int legacyReleaseCancelTicks = 0;

   private enum Step {
      NONE, DELAYING
   }

   @Override
   public void onDisable() {
      finishGrimOffhandSwap(true);
      legacyEating = false;
      legacyDropSent = false;
      legacyReleaseCancelTicks = 0;
      releaseMovementPackets();
      super.onDisable();
   }

   @EventTarget
   public void onSlow(EventSlowdown eventSlowdown) {
      if (mc.player == null || (checkFood() && mc.player.getUseItemRemainingTicks() > 30)) return;

      if (!food.getCurrentValue() && checkFood()) return;
      if (!bow.getCurrentValue() && checkItem(Items.BOW)) return;
      if (!crossbow.getCurrentValue() && checkItem(Items.CROSSBOW)) return;

      switch (modeValue.getCurrentMode()) {
         case "Grim":
            switch (grimForm.getCurrentMode()) {
               case "Item Switch":
                  if (mc.player.isUsingItem() && mc.player.tickCount % 33 == 0) {
                     mc.player.connection.send(new ServerboundSetCarriedItemPacket((mc.player.getInventory().selected + 1) % 8));
                     mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
                  }

                  if (mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() <= 26) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;
               case "Tick Slow":
                  switch (tickSlow.getCurrentMode()) {
                     case "1:1 Pattern":
                        if (mc.player.getUseItemRemainingTicks() % 2 == 0) {
                           eventSlowdown.setSlowdown(false);
                           if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                        }
                        break;

                     case "1:3 Pattern":
                        if (mc.player.getUseItemRemainingTicks() % 3 == 0) {
                           eventSlowdown.setSlowdown(false);
                           if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                        }
                        break;

                     case "Legacy Drop":
                        if (mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() <= 30) {
                           eventSlowdown.setSlowdown(false);
                        }
                        if (mc.player.isUsingItem() && !mc.player.isSprinting()) {
                           mc.player.setSprinting(true);
                        }
                        break;
                  }
                  break;
               case "Food Drop":
                  if (mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() <= 30) {
                     ItemStack usingItem = mc.player.getUseItem();

                     if (usingItem != null && usingItem.getItem().isEdible() && !foodDroppedThisUse) {
                        eventSlowdown.setSlowdown(false);
                        if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);

                        mc.gameMode.handleInventoryMouseClick(
                                mc.player.inventoryMenu.containerId,
                                mc.player.getInventory().selected + 36,
                                0,
                                ClickType.THROW,
                                mc.player
                        );
                        eventSlowdown.setSlowdown(false);

                        foodDroppedThisUse = true;
                     }
                  }

                  if (!mc.player.isUsingItem()) {
                     foodDroppedThisUse = false;
                  }
                  break;
               case "Blink":
                  boolean usingNow = mc.player != null && mc.player.getUseItemRemainingTicks() > 0;
                  if (usingNow && mc.player.getUseItem() != null && mc.player.getUseItem().getItem() instanceof BowItem) {
                     return;
                  }

                  if (usingNow && !usingActive) {
                     usingActive = true;
                     blinkSlowdownCounter = (int) slowdownTicks.getCurrentValue();
                     blinkActive = false;
                     postDelayTicks = 0;
                     movementQueue.clear();

                     useTimer = 0;
                     if (mc.player.getUseItem() != null) {
                        maxUseDuration = mc.player.getUseItem().getUseDuration();
                     } else {
                        maxUseDuration = 32;
                     }
                  }

                  if (usingNow) {
                     if (blinkSlowdownCounter > 0) {
                        blinkSlowdownCounter--;
                     } else {
                        blinkActive = true;
                        eventSlowdown.setSlowdown(false);
                        if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                        if (mc.options.keyUse.isDown()) {
                           mc.options.keyUse.setDown(false);
                        }
                     }
                  }
                  break;
            }
            break;
         case "Heypixel":
            switch (heypixelTick.getCurrentMode()) {
               case "1:3 Pattern":
                  if (mc.player.getUseItemRemainingTicks() % 3 == 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;

               case "3:2 Pattern":
                  if (mc.player.getUseItemRemainingTicks() % 3 != 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;

               case "Swap Pattern":
                  if (checkSword()) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;
            }
            break;
         case "Grim Offhand Swap":
            break;
      }
   }

   @EventTarget
   public void onSlowDown(EventSlowdown e) {
      if (!modeValue.isCurrentMode("Grim Offhand Swap")) return;

      if (mc.player == null || mc.getConnection() == null || mc.player.getUseItem() == null) return;

      UseAnim activeUseAnim = mc.player.getUseItem().getUseAnimation();
      if (!isUsable(activeUseAnim) || mc.player.getUseItemRemainingTicks() <= 0) {
         return;
      }

      InteractionHand oppositeHand = mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
              ? InteractionHand.OFF_HAND
              : InteractionHand.MAIN_HAND;

      if (isUsable(mc.player.getItemInHand(oppositeHand).getUseAnimation())) {
         return;
      }

      if (step == Step.NONE) {
         startGrimOffhandSwap();
      }

      if (step == Step.DELAYING) {
         mc.player.setSprinting(true);
         e.setSlowdown(false);
      }
   }

   @EventTarget
   public void onRunTicks(EventRunTicks event) {
      if (!isEnabled()) {
         legacyEating = false;
         legacyDropSent = false;
         legacyReleaseCancelTicks = 0;
         return;
      }

      if (event.getType() == EventType.POST) {
         if (!modeValue.isCurrentMode("Grim") || !grimForm.isCurrentMode("Tick Slow") || !tickSlow.isCurrentMode("Legacy Drop")) {
            legacyEating = false;
            legacyDropSent = false;
            legacyReleaseCancelTicks = 0;
            return;
         }

         if (mc.player == null || mc.options == null) {
            legacyEating = false;
            legacyDropSent = false;
            legacyReleaseCancelTicks = 0;
            return;
         }

         if (legacyReleaseCancelTicks > 0) {
            legacyReleaseCancelTicks--;
         }

         if (mc.player.isUsingItem()) {
            ItemStack itemStack = mc.player.getUseItem();
            if (isLegacyHeypixelFood(itemStack)) {
               legacyEating = true;
               if (mc.player.getTicksUsingItem() >= legacyHeypixelTicks.getCurrentValue() && mc.options.keyUse.isDown()) {
                  if (!legacyDropSent) {
                     int dropSlot = getLegacyHeypixelDropSlot();
                     if (dropSlot != -1) {
                        sendLegacyHeypixelThrowPacket(dropSlot);
                        sendLegacyHeypixelThrowPacket(dropSlot);
                     }
                     legacyDropSent = true;
                  }
                  legacyReleaseCancelTicks = 2;
                  mc.options.keyUse.setDown(false);
               }
            } else {
               legacyEating = false;
               legacyDropSent = false;
            }
         } else if (legacyEating) {
            legacyEating = false;
            legacyDropSent = false;
         }
         return;
      }

      if (event.getType() != EventType.PRE) return;

      if (modeValue.isCurrentMode("Grim Offhand Swap")) {
         updateGrimOffhandSwap();
      }

      if (usingActive) {
         useTimer++;
      }

      if (blinkActive && useTimer >= maxUseDuration) {
         if (postDelayTicks < 1) {
            postDelayTicks++;
            return;
         }
         blinkActive = false;
         releaseMovementPackets();
         usingActive = false;
         return;
      }

      if (blinkActive) {
         while (StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).anyMatch(entity -> entity instanceof PrimedTnt && mc.player.distanceTo(entity) < 10) && !movementQueue.isEmpty()) {
            release1Tick();
         }

         while (!movementQueue.isEmpty()) {
            long movementCount = movementQueue.stream().filter(p -> p instanceof ServerboundMovePlayerPacket).count();
            if (movementCount < 100) {
               break;
            }
            release1Tick();
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      if (event.getType() == EventType.PRE
              && modeValue.isCurrentMode("Heypixel")
              && heypixelTick.isCurrentMode("Swap Pattern")) {
         if (mc.player == null || mc.getConnection() == null || !mc.player.isUsingItem() || !checkSword()) {
            return;
         }

         if (mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0));
         } else {
            int slot = mc.player.getInventory().selected;
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot % 8 + 1));
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot % 7 + 2));
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
         }
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (!isEnabled()) return;

      if (event.getType() == EventType.SEND
              && modeValue.isCurrentMode("Grim")
              && grimForm.isCurrentMode("Tick Slow")
              && tickSlow.isCurrentMode("Legacy Drop")
              && event.getPacket() instanceof ServerboundPlayerActionPacket actionPacket
              && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM
              && legacyReleaseCancelTicks > 0) {
         event.setCancelled(true);
         legacyReleaseCancelTicks = 0;
         return;
      }

      if (modeValue.isCurrentMode("Grim Offhand Swap")) {
         Packet<?> packet = event.getPacket();

         if (event.getType() == EventType.RECEIVE
                 && step == Step.DELAYING
                 && shouldDelayGrimOffhandPacket(packet)) {
            event.setCancelled(true);
            queueDelayedOffhandPacket(packet);
            return;
         }

         if (event.getType() == EventType.SEND && packet instanceof ServerboundPlayerActionPacket) {
            ServerboundPlayerActionPacket actionPacket = (ServerboundPlayerActionPacket) packet;
            if (actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && step == Step.DELAYING) {
               finishGrimOffhandSwap(true);
            }
         }
      }

      // Per Phase Blink
      if (event.getType() == EventType.RECEIVE) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            releaseMovementPackets();
         }
         return;
      }

      if (event.getType() != EventType.SEND) return;
      if (!blinkActive) return;

      Packet<?> packet = event.getPacket();

      if (packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundUseItemPacket) {
         return;
      }

      if (packet instanceof ServerboundChatPacket) {
         return;
      }

      event.setCancelled(true);
      movementQueue.offer(packet);
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      updateSuffix();
   }

   private void updateSuffix() {
      String suffix = modeValue.getCurrentMode();

      if (modeValue.isCurrentMode("Grim")) {
         String form = grimForm.getCurrentMode();
         if (form.equals("Tick Slow")) {
            suffix = "Grim(" + form + "/" + tickSlow.getCurrentMode() + ")";
         } else {
            suffix = "Grim(" + form + ")";
         }
      }

      if (modeValue.isCurrentMode("Heypixel")) {
         suffix = "Heypixel(" + heypixelTick.getCurrentMode() + ")";
      }

      if (modeValue.isCurrentMode("Grim Offhand Swap")) {
         suffix = "Grim Offhand Swap";
      }

      this.setSuffix(suffix);
   }

   private void releaseQueuedPackets() {
      while (!delayedOffhandPackets.isEmpty()) {
         Packet<ClientGamePacketListener> p = delayedOffhandPackets.poll();
         if (p != null && mc.getConnection() != null) {
            p.handle(mc.getConnection());
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void queueDelayedOffhandPacket(Packet<?> packet) {
      delayedOffhandPackets.offer((Packet<ClientGamePacketListener>) packet);
   }

   private void startGrimOffhandSwap() {
      step = Step.DELAYING;
      grimOffhandUseTicks = 0;
      delayedOffhandPackets.clear();

      boolean isInventoryOpenServerSide = mc.player.containerMenu != mc.player.inventoryMenu;
      if (isInventoryOpenServerSide) {
         mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      }

      swap();
   }

   private void updateGrimOffhandSwap() {
      if (step != Step.DELAYING) {
         return;
      }

      if (mc.player == null || mc.options == null || !mc.player.isUsingItem() || !mc.options.keyUse.isDown()) {
         finishGrimOffhandSwap(true);
         return;
      }

      grimOffhandUseTicks++;
      if (grimOffhandUseTicks >= 32) {
         finishGrimOffhandSwap(true);
      }
   }

   private void finishGrimOffhandSwap(boolean swapBack) {
      boolean wasDelaying = step == Step.DELAYING;
      step = Step.NONE;
      grimOffhandUseTicks = 0;

      releaseQueuedPackets();

      if (swapBack && wasDelaying) {
         swap();
      }
   }

   private boolean shouldDelayGrimOffhandPacket(Packet<?> packet) {
      if (packet instanceof ClientboundContainerSetSlotPacket slotPacket) {
         return slotPacket.getContainerId() == ClientboundContainerSetSlotPacket.PLAYER_INVENTORY
                 || mc.player != null && slotPacket.getContainerId() == mc.player.inventoryMenu.containerId;
      }

      if (packet instanceof ClientboundContainerSetContentPacket contentPacket) {
         return mc.player != null && contentPacket.getContainerId() == mc.player.inventoryMenu.containerId;
      }

      return false;
   }

   private void releaseMovementPackets() {
      if (mc.getConnection() == null) {
         movementQueue.clear();
         return;
      }
      try {
         while (!movementQueue.isEmpty()) {
            Packet<?> packet = movementQueue.poll();
            if (packet != null) {
               mc.getConnection().getConnection().send(packet);
            }
         }
      } catch (Exception ignored) {
      }
   }

   private void release1Tick() {
      if (mc.getConnection() == null) {
         movementQueue.clear();
         return;
      }
      try {
         while (!movementQueue.isEmpty()) {
            Packet<?> packet = movementQueue.poll();
            if (packet != null) {
               mc.getConnection().getConnection().send(packet);
            }
            if (packet instanceof ServerboundMovePlayerPacket) {
               break;
            }
         }
      } catch (Exception ignored) {
      }
   }

   private void swap() {
      if (mc.getConnection() != null) {
         mc.getConnection().send(new ServerboundPlayerActionPacket(
                 ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                 BlockPos.ZERO,
                 Direction.DOWN
         ));
      }
   }

   private boolean isUsable(UseAnim useAnim) {
      return useAnim == UseAnim.EAT || useAnim == UseAnim.DRINK;
   }

   private boolean checkFood() {
      ItemStack mainHandItem = mc.player.getMainHandItem();
      ItemStack offhandItem = mc.player.getOffhandItem();
      return mainHandItem.is(Items.GOLDEN_APPLE)
              || offhandItem.is(Items.GOLDEN_APPLE)
              || mainHandItem.is(Items.ENCHANTED_GOLDEN_APPLE)
              || offhandItem.is(Items.ENCHANTED_GOLDEN_APPLE)
              || mainHandItem.is(Items.POTION)
              || offhandItem.is(Items.POTION);
   }

   private boolean checkItem(Item item) {
      ItemStack mainHandItem = mc.player.getMainHandItem();
      ItemStack offhandItem = mc.player.getOffhandItem();
      return mainHandItem.is(item) || offhandItem.is(item);
   }

   private boolean checkSword() {
      ItemStack mainHandItem = mc.player.getMainHandItem();
      ItemStack offhandItem = mc.player.getOffhandItem();
      return mainHandItem.getItem() instanceof SwordItem || offhandItem.getItem() instanceof SwordItem;
   }

   private boolean isLegacyHeypixelFood(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem().isEdible();
   }

   private int getLegacyHeypixelDropSlot() {
      int selected = mc.player.getInventory().selected;
      int[] slots = {
              selected + 1,
              selected - 1,
              selected + 2,
              selected - 2
      };

      for (int slot : slots) {
         if (slot >= 0 && slot < 9 && isStackedDropItem(mc.player.getInventory().getItem(slot))) {
            return slot;
         }
      }

      return -1;
   }

   private boolean isStackedDropItem(ItemStack stack) {
      return !stack.isEmpty() && stack.getMaxStackSize() > 1 && stack.getCount() >= 2;
   }

   private void sendLegacyHeypixelThrowPacket(int inventorySlot) {
      mc.player.connection.send(new ServerboundContainerClickPacket(
              mc.player.inventoryMenu.containerId,
              mc.player.inventoryMenu.getStateId(),
              inventorySlot + 36,
              0,
              ClickType.THROW,
              mc.player.inventoryMenu.getCarried().copy(),
              new Int2ObjectOpenHashMap<>()
      ));
   }
}
