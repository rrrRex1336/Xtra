package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventGlobalPacket;
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
import net.minecraft.entity.TntEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;

import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.item.*;

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
           .setModes("Grim", "Heypixel")
           .build()
           .getModeValue();

   private final ModeValue grimForm = ValueBuilder.create(this, "Grim Mode")
           .setDefaultModeIndex(0)
           .setModes("Item Switch", "Tick Slow", "Blink")
           .setVisibility(() -> this.modeValue.isCurrentMode("Grim"))
           .build()
           .getModeValue();

   private final ModeValue tickSlow = ValueBuilder.create(this, "Tick Pattern")
           .setDefaultModeIndex(0)
           .setModes("1:1 Pattern", "1:3 Pattern")
           .setVisibility(() -> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Tick Slow"))
           .build()
           .getModeValue();

   private final ModeValue heypixelTick = ValueBuilder.create(this, "Heypixel Pattern")
           .setDefaultModeIndex(0)
           .setModes("1:3 Pattern", "3:2 Pattern", "Old Heypixel")
           .setVisibility(() -> modeValue.isCurrentMode("Heypixel"))
           .build()
           .getModeValue();

   private final FloatValue legacyHeypixelTicks = ValueBuilder.create(this, "Old Heypixel Ticks")
           .setDefaultFloatValue(9.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(32.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> modeValue.isCurrentMode("Heypixel") && heypixelTick.isCurrentMode("Old Heypixel"))
           .build()
           .getFloatValue();

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
   private boolean legacyEating = false;
   private boolean legacyDropSent = false;
   private int legacyReleaseCancelTicks = 0;
   private boolean itemSwitchClientStateDelay = false;
   private final Queue<Packet<ClientPlayPacketListener>> delayedClientStatePackets = new ConcurrentLinkedQueue<>();
   private final Queue<Packet<?>> delayedItemSwitchActionPackets = new ConcurrentLinkedQueue<>();

   @Override
   public void onDisable() {
      legacyEating = false;
      legacyDropSent = false;
      legacyReleaseCancelTicks = 0;
      itemSwitchClientStateDelay = false;
      releaseDelayedClientStatePackets();
      releaseDelayedItemSwitchActionPackets();
      releaseMovementPackets();
      super.onDisable();
   }

   @EventTarget
   public void onSlow(EventSlowdown eventSlowdown) {
      if (mc.player == null || (checkFood() && mc.player.getItemUseTimeLeft() > 30)) return;

      if (!food.getCurrentValue() && checkFood()) return;
      if (!bow.getCurrentValue() && checkItem(Items.BOW)) return;
      if (!crossbow.getCurrentValue() && checkItem(Items.CROSSBOW)) return;

      switch (modeValue.getCurrentMode()) {
         case "Grim":
            switch (grimForm.getCurrentMode()) {
               case "Item Switch":
                  break;
               case "Tick Slow":
                  switch (tickSlow.getCurrentMode()) {
                     case "1:1 Pattern":
                        if (mc.player.getItemUseTimeLeft() % 2 == 0) {
                           eventSlowdown.setSlowdown(false);
                           if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                        }
                        break;

                     case "1:3 Pattern":
                        if (mc.player.getItemUseTimeLeft() % 3 == 0) {
                           eventSlowdown.setSlowdown(false);
                           if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                        }
                        break;
                  }
                  break;
               case "Blink":
                  boolean usingNow = mc.player != null && mc.player.getItemUseTimeLeft() > 0;
                  if (usingNow && mc.player.getActiveItem() != null && mc.player.getActiveItem().getItem() instanceof BowItem) {
                     return;
                  }

                  if (usingNow && !usingActive) {
                     usingActive = true;
                     blinkSlowdownCounter = (int) slowdownTicks.getCurrentValue();
                     blinkActive = false;
                     postDelayTicks = 0;
                     movementQueue.clear();

                     useTimer = 0;
                     if (mc.player.getActiveItem() != null) {
                        maxUseDuration = mc.player.getActiveItem().getMaxUseTime();
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
                        if (mc.options.useKey.isPressed()) {
                           mc.options.useKey.setPressed(false);
                        }
                     }
                  }
                  break;
            }
            break;
         case "Heypixel":
            switch (heypixelTick.getCurrentMode()) {
               case "1:3 Pattern":
                  if (mc.player.getItemUseTimeLeft() % 3 == 0 && (!checkFood() || mc.player.getItemUseTimeLeft() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;

               case "3:2 Pattern":
                  if (mc.player.getItemUseTimeLeft() % 3 != 0 && (!checkFood() || mc.player.getItemUseTimeLeft() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;
               case "Old Heypixel":
                  if (mc.player.isUsingItem() && mc.player.getItemUseTimeLeft() <= 30) {
                     eventSlowdown.setSlowdown(false);
                  }
                  if (mc.player.isUsingItem() && !mc.player.isSprinting()) {
                     mc.player.setSprinting(true);
                  }
                  break;
            }
            break;
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
         if (!modeValue.isCurrentMode("Heypixel") || !heypixelTick.isCurrentMode("Old Heypixel")) {
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
            ItemStack itemStack = mc.player.getActiveItem();
            if (isLegacyHeypixelFood(itemStack)) {
               legacyEating = true;
               if (mc.player.getItemUseTime() >= legacyHeypixelTicks.getCurrentValue() && mc.options.useKey.isPressed()) {
                  if (!legacyDropSent) {
                     int dropSlot = getLegacyHeypixelDropSlot();
                     if (dropSlot != -1) {
                        sendLegacyHeypixelThrowPacket(dropSlot);
                        sendLegacyHeypixelThrowPacket(dropSlot);
                     }
                     legacyDropSent = true;
                  }
                  legacyReleaseCancelTicks = 2;
                  mc.options.useKey.setPressed(false);
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

      updateItemSwitchClientStateDelay();

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
         while (StreamSupport.stream(mc.world.getEntities().spliterator(), false).anyMatch(entity -> entity instanceof TntEntity && mc.player.distanceTo(entity) < 10) && !movementQueue.isEmpty()) {
            release1Tick();
         }

         while (!movementQueue.isEmpty()) {
            long movementCount = movementQueue.stream().filter(p -> p instanceof PlayerMoveC2SPacket).count();
            if (movementCount < 100) {
               break;
            }
            release1Tick();
         }
      }
   }

   @EventTarget
   public void onGlobalPacket(EventGlobalPacket event) {
      if (!isEnabled()) {
         return;
      }

      if (event.getType() == EventType.SEND) {
         if (!itemSwitchClientStateDelay) {
            return;
         }

         if (isOffhandSwapAction(event.getPacket())) {
            event.setCancelled(true);
            delayedItemSwitchActionPackets.offer(event.getPacket());
         }
         return;
      }

      if (event.getType() != EventType.RECEIVE) {
         return;
      }

      if (!itemSwitchClientStateDelay && shouldStartItemSwitchClientStateDelay()) {
         startItemSwitchClientStateDelay();
      }

      if (!itemSwitchClientStateDelay || !shouldDelayClientStatePacket(event.getPacket())) {
         return;
      }

      event.setCancelled(true);
      queueDelayedClientStatePacket(event.getPacket());
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (!isEnabled()) return;

      if (event.getType() == EventType.SEND
              && modeValue.isCurrentMode("Heypixel")
              && heypixelTick.isCurrentMode("Old Heypixel")
              && event.getPacket() instanceof PlayerActionC2SPacket actionPacket
              && actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
              && legacyReleaseCancelTicks > 0) {
         event.setCancelled(true);
         legacyReleaseCancelTicks = 0;
         return;
      }

      // Per Phase Blink
      if (event.getType() == EventType.RECEIVE) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            releaseMovementPackets();
         }
         return;
      }

      if (event.getType() != EventType.SEND) return;
      if (!blinkActive) return;

      Packet<?> packet = event.getPacket();

      if (packet instanceof PlayerInteractBlockC2SPacket || packet instanceof PlayerInteractItemC2SPacket) {
         return;
      }

      if (packet instanceof ChatMessageC2SPacket) {
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

      this.setSuffix(suffix);
   }

   private void updateItemSwitchClientStateDelay() {
      if (shouldStartItemSwitchClientStateDelay()) {
         startItemSwitchClientStateDelay();
         return;
      }

      if (itemSwitchClientStateDelay && !shouldKeepItemSwitchClientStateDelay()) {
         itemSwitchClientStateDelay = false;
         releaseDelayedClientStatePackets();
         releaseDelayedItemSwitchActionPackets();
      }
   }

   private void startItemSwitchClientStateDelay() {
      itemSwitchClientStateDelay = true;
   }

   private boolean shouldStartItemSwitchClientStateDelay() {
      if (!modeValue.isCurrentMode("Grim") || !grimForm.isCurrentMode("Item Switch")) {
         return false;
      }
      if (!food.getCurrentValue() || mc.player == null || mc.options == null || !mc.options.useKey.isPressed() || !mc.player.isUsingItem()) {
         return false;
      }
      return isFoodUseItem(mc.player.getActiveItem());
   }

   private boolean shouldKeepItemSwitchClientStateDelay() {
      return modeValue.isCurrentMode("Grim")
              && grimForm.isCurrentMode("Item Switch")
              && mc.player != null
              && mc.options != null
              && mc.options.useKey.isPressed();
   }

   private boolean shouldDelayClientStatePacket(Packet<?> packet) {
      if (packet == null || mc.player == null) {
         return false;
      }

      int playerId = mc.player.getId();
      if (packet instanceof PlaySoundFromEntityS2CPacket soundPacket) {
         return soundPacket.getEntityId() == playerId && isFoodCompletionSound(soundPacket.getSound().value());
      }
      if (packet instanceof PlaySoundS2CPacket soundPacket) {
         return isFoodCompletionSound(soundPacket.getSound().value())
                 && mc.player.squaredDistanceTo(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()) <= 9.0D;
      }
      if (packet instanceof EntityVelocityUpdateS2CPacket motionPacket) {
         return motionPacket.getId() == playerId;
      }
      if (packet instanceof EntityDamageS2CPacket damagePacket) {
         return damagePacket.entityId() == playerId;
      }
      if (packet instanceof DamageTiltS2CPacket hurtPacket) {
         return hurtPacket.id() == playerId;
      }
      if (packet instanceof EntityStatusS2CPacket entityEventPacket) {
         return mc.world != null && entityEventPacket.getEntity(mc.world) == mc.player;
      }
      if (packet instanceof EntityTrackerUpdateS2CPacket dataPacket) {
         return dataPacket.id() == playerId;
      }
      if (packet instanceof EntityAttributesS2CPacket attributesPacket) {
         return attributesPacket.getEntityId() == playerId;
      }
      if (packet instanceof EntityStatusEffectS2CPacket effectPacket) {
         return effectPacket.getEntityId() == playerId;
      }
      if (packet instanceof RemoveEntityStatusEffectS2CPacket effectPacket) {
         return mc.world != null && effectPacket.getEntity(mc.world) == mc.player;
      }
      if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentPacket) {
         return equipmentPacket.getId() == playerId;
      }
      if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slotPacket) {
         int containerId = slotPacket.getSyncId();
         return containerId == ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID
                 || containerId == ScreenHandlerSlotUpdateS2CPacket.UPDATE_CURSOR_SYNC_ID
                 || containerId == mc.player.playerScreenHandler.syncId;
      }
      if (packet instanceof InventoryS2CPacket contentPacket) {
         return contentPacket.getSyncId() == mc.player.playerScreenHandler.syncId;
      }
      return packet instanceof HealthUpdateS2CPacket
              || packet instanceof PlayerPositionLookS2CPacket
              || packet instanceof ExperienceBarUpdateS2CPacket
              || packet instanceof UpdateSelectedSlotS2CPacket;
   }

   @SuppressWarnings("unchecked")
   private void queueDelayedClientStatePacket(Packet<?> packet) {
      delayedClientStatePackets.offer((Packet<ClientPlayPacketListener>) packet);
   }

   private void releaseDelayedClientStatePackets() {
      if (mc.getNetworkHandler() == null) {
         delayedClientStatePackets.clear();
         return;
      }

      try {
         while (!delayedClientStatePackets.isEmpty()) {
            Packet<ClientPlayPacketListener> packet = delayedClientStatePackets.poll();
            if (packet != null) {
               packet.apply(mc.getNetworkHandler());
            }
         }
      } catch (Exception ignored) {
      }
   }

   private boolean isOffhandSwapAction(Packet<?> packet) {
      return packet instanceof PlayerActionC2SPacket actionPacket
              && actionPacket.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND;
   }

   private boolean isFoodCompletionSound(SoundEvent sound) {
      return sound == SoundEvents.ENTITY_PLAYER_BURP;
   }

   private void releaseDelayedItemSwitchActionPackets() {
      if (mc.getNetworkHandler() == null) {
         delayedItemSwitchActionPackets.clear();
         return;
      }

      try {
         while (!delayedItemSwitchActionPackets.isEmpty()) {
            Packet<?> packet = delayedItemSwitchActionPackets.poll();
            if (packet != null) {
               mc.getNetworkHandler().sendPacket(packet);
            }
         }
      } catch (Exception ignored) {
      }
   }

   private void releaseMovementPackets() {
      if (mc.getNetworkHandler() == null) {
         movementQueue.clear();
         return;
      }
      try {
         while (!movementQueue.isEmpty()) {
            Packet<?> packet = movementQueue.poll();
            if (packet != null) {
               mc.getNetworkHandler().getConnection().send(packet);
            }
         }
      } catch (Exception ignored) {
      }
   }

   private void release1Tick() {
      if (mc.getNetworkHandler() == null) {
         movementQueue.clear();
         return;
      }
      try {
         while (!movementQueue.isEmpty()) {
            Packet<?> packet = movementQueue.poll();
            if (packet != null) {
               mc.getNetworkHandler().getConnection().send(packet);
            }
            if (packet instanceof PlayerMoveC2SPacket) {
               break;
            }
         }
      } catch (Exception ignored) {
      }
   }

   private boolean checkFood() {
      ItemStack mainHandItem = mc.player.getMainHandStack();
      ItemStack offhandItem = mc.player.getOffHandStack();
      return mainHandItem.isOf(Items.GOLDEN_APPLE)
              || offhandItem.isOf(Items.GOLDEN_APPLE)
              || mainHandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE)
              || offhandItem.isOf(Items.ENCHANTED_GOLDEN_APPLE)
              || mainHandItem.isOf(Items.POTION)
              || offhandItem.isOf(Items.POTION);
   }

   private boolean checkItem(Item item) {
      ItemStack mainHandItem = mc.player.getMainHandStack();
      ItemStack offhandItem = mc.player.getOffHandStack();
      return mainHandItem.isOf(item) || offhandItem.isOf(item);
   }

   private boolean isFoodUseItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      UseAction animation = stack.getUseAction();
      return stack.getItem().isFood() || animation == UseAction.EAT || animation == UseAction.DRINK;
   }

   private boolean isLegacyHeypixelFood(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem().isFood();
   }

   private int getLegacyHeypixelDropSlot() {
      int selected = mc.player.getInventory().selectedSlot;
      int[] slots = {
              selected + 1,
              selected - 1,
              selected + 2,
              selected - 2
      };

      for (int slot : slots) {
         if (slot >= 0 && slot < 9 && isStackedDropItem(mc.player.getInventory().getStack(slot))) {
            return slot;
         }
      }

      return -1;
   }

   private boolean isStackedDropItem(ItemStack stack) {
      return !stack.isEmpty() && stack.getMaxCount() > 1 && stack.getCount() >= 2;
   }

   private void sendLegacyHeypixelThrowPacket(int inventorySlot) {
      mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
              mc.player.playerScreenHandler.syncId,
              mc.player.playerScreenHandler.getRevision(),
              inventorySlot + 36,
              0,
              SlotActionType.THROW,
              mc.player.playerScreenHandler.getCursorStack().copy(),
              new Int2ObjectOpenHashMap<>()
      ));
   }
}
