package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
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
   public final ModeValue modeValue = ValueBuilder.create(this, "Mode Select")
           .setDefaultModeIndex(0)
           .setModes("Grim", "Heypixel Fast", "Latest Grim")
           .build()
           .getModeValue();

   private final ModeValue grimForm = ValueBuilder.create(this, "Grim Slow Form")
           .setDefaultModeIndex(0)
           .setModes("Switch Item", "Tick Slowdown", "Food Drop Cancel", "Per Phase Blink")
           .setVisibility(() -> this.modeValue.isCurrentMode("Grim"))
           .build()
           .getModeValue();

   private final ModeValue tickSlow = ValueBuilder.create(this, "Tick Slow Select")
           .setDefaultModeIndex(0)
           .setModes("1:1 Interlace", "1:3 Pattern")
           .setVisibility(() -> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Tick Slowdown"))
           .build()
           .getModeValue();

   private final ModeValue heypixelTick = ValueBuilder.create(this, "Heypixel Tick Slow")
           .setDefaultModeIndex(0)
           .setModes("1:3 Pattern", "3:2 TickSlow")
           .setVisibility(() -> modeValue.isCurrentMode("Heypixel Fast"))
           .build()
           .getModeValue();

   public FloatValue slowdownTicks = ValueBuilder.create(this, "Slowdown Ticks")
           .setDefaultFloatValue(12.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(20.0F)
           .setFloatStep(1.0F)
           .setVisibility(()-> modeValue.isCurrentMode("Grim") && grimForm.isCurrentMode("Per Phase Blink"))
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
   private int noUsingItemTicks = 0;
   private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
   private boolean foodDroppedThisUse = false;

   private enum Step {
      NONE, CANCEL_C0F, SWAP_HANDS, EATING
   }

   @Override
   public void onDisable() {
      step = Step.NONE;
      noUsingItemTicks = 0;
      releaseQueuedPackets();
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
               case "Switch Item":
                  if (mc.player.isUsingItem() && mc.player.tickCount % 33 == 0) {
                     mc.player.connection.send(new ServerboundSetCarriedItemPacket((mc.player.getInventory().selected + 1) % 8));
                     mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
                  }

                  if (mc.player.isUsingItem() && mc.player.getUseItemRemainingTicks() <= 26) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;
               case "Tick Slowdown":
                  switch (tickSlow.getCurrentMode()) {
                     case "1:1 Interlace":
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
                  }
                  break;
               case "Food Drop Cancel":
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
               case "Per Phase Blink":
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
         case "Heypixel Fast":
            switch (heypixelTick.getCurrentMode()) {
               case "1:3 Pattern":
                  if (mc.player.getUseItemRemainingTicks() % 3 == 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;

               case "3:2 TickSlow":
                  if (mc.player.getUseItemRemainingTicks() % 3 != 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
                     eventSlowdown.setSlowdown(false);
                     if (mc.player.isUsingItem() && !mc.player.isSprinting()) mc.player.setSprinting(true);
                  }
                  break;
            }
            break;
         case "Latest Grim":
            break;
      }
   }

   @EventTarget
   public void onSlowDown(EventSlowdown e) {
      if (!modeValue.isCurrentMode("Latest Grim")) return;

      if (mc.player == null || mc.player.getUseItem() == null) return;

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

      if (step != Step.EATING) {
         mc.options.keyUse.setDown(false);
      }

      if (step == Step.NONE) {
         step = Step.CANCEL_C0F;

         boolean isInventoryOpenServerSide = mc.player.containerMenu != mc.player.inventoryMenu;
         if (isInventoryOpenServerSide) {
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
         }
      } else if (step == Step.EATING) {
         mc.player.setSprinting(true);
         e.setSlowdown(false);
      }
   }

   @EventTarget
   public void onRunTicks(EventRunTicks event) {
      if (!isEnabled() || event.getType() != EventType.PRE) return;

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
   public void onPacket(EventPacket event) {
      if (!isEnabled()) return;

      // Latest Grim
      if (modeValue.isCurrentMode("Latest Grim")) {
         Packet<?> packet = event.getPacket();

         if (packet instanceof ServerboundPongPacket && step != Step.NONE) {
            event.setCancelled(true);
            packets.add(packet);

            if (step == Step.CANCEL_C0F) {
               step = Step.SWAP_HANDS;
               mc.getConnection().send(new ServerboundPlayerActionPacket(
                       ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                       BlockPos.ZERO,
                       Direction.DOWN
               ));
            }
         }

         if (packet instanceof ClientboundContainerSetSlotPacket && step == Step.SWAP_HANDS) {
            mc.options.keyUse.setDown(true);
            step = Step.EATING;
         }

         if (packet instanceof ServerboundPlayerActionPacket) {
            ServerboundPlayerActionPacket actionPacket = (ServerboundPlayerActionPacket) packet;
            if (actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && step == Step.EATING) {
               releaseQueuedPackets();
               swap();
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
      // Latest Grim
      if (modeValue.isCurrentMode("Latest Grim")) {
         if (step != Step.EATING) {
            noUsingItemTicks = 0;
         } else {
            if (mc.player.isUsingItem()) {
               noUsingItemTicks = 0;
            } else {
               noUsingItemTicks++;
               if (noUsingItemTicks >= 5) {
                  releaseQueuedPackets();
                  swap();
               }
            }
         }
      }

      updateSuffix();
   }

   private void updateSuffix() {
      String suffix = modeValue.getCurrentMode();

      if (modeValue.isCurrentMode("Grim")) {
         String form = grimForm.getCurrentMode();
         if (form.equals("Tick Slowdown")) {
            suffix = "Grim(" + form + "/" + tickSlow.getCurrentMode() + ")";
         } else {
            suffix = "Grim(" + form + ")";
         }
      }

      if (modeValue.isCurrentMode("Heypixel Fast")) {
         suffix = "Heypixel Fast(Tick Slowdown/" + heypixelTick.getCurrentMode() + ")";
      }

      if (modeValue.isCurrentMode("Latest Grim")) {
         suffix = "Latest Grim(Offhand Swap)";
      }

      this.setSuffix(suffix);
   }

   private void releaseQueuedPackets() {
      step = Step.NONE;

      while (!packets.isEmpty()) {
         Packet<?> p = packets.poll();
         if (p != null && mc.getConnection() != null) {
            mc.getConnection().send(p);
         }
      }
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
}