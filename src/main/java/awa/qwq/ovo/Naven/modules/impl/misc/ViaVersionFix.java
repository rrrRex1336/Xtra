package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.OldHitting;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;

@ModuleInfo(
   name = "ViaVersionFix",
   category = Category.MISC,
   description = "Fix ViaVersion translated legacy and high version behavior."
)
public class ViaVersionFix extends Module {
   private static ViaVersionFix instance;
   private static final String PROTOCOL_TRANSLATOR = "de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator";
   private static final String PROTOCOL_VERSION = "com.viaversion.viaversion.api.protocol.version.ProtocolVersion";
   private static final String VIAFABRICPLUS_HAND_ITEM_PROVIDER = "de.florianmichael.viafabricplus.protocoltranslator.impl.provider.viaversion.ViaFabricPlusHandItemProvider";
   private static boolean serverLegacyBlockingShield;

   public final BooleanValue blocking = ValueBuilder.create(this, "Blocking")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   public final BooleanValue placement = ValueBuilder.create(this, "Placement")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   public final BooleanValue highVersionItem = ValueBuilder.create(this, "High Version Item")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   public final BooleanValue transactionFix = ValueBuilder.create(this, "Transaction-Fix")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public ViaVersionFix() {
      instance = this;
   }

   public static boolean isBlockingFixEnabled() {
      return instance != null && instance.isEnabled() && instance.blocking.getCurrentValue();
   }

   public static boolean shouldApplyLegacyBlockingSlowdown() {
      if (!isBlockingFixEnabled() || mc.player == null || mc.level == null) {
         return false;
      }

      if (serverLegacyBlockingShield || hasVanillaBlockingUseState()) {
         return false;
      }

      if (!isLegacyBlockingContext()) {
         return false;
      }

      return isMainHandSwordBlockingInput();
   }

   private static boolean isLegacyBlockingContext() {
      return isTargetOlderThanOrEqualTo("v1_8") && !serverLegacyBlockingShield;
   }

   private static boolean isMainHandSwordBlockingInput() {
      if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof SwordItem)) {
         return false;
      }

      if (isUsingNonShieldOffhandItem()) {
         return false;
      }

      if (mc.options.keyUse.isDown()) {
         return true;
      }

      try {
         OldHitting oldHitting = (OldHitting)Naven.getInstance().getModuleManager().getModule(OldHitting.class);
         return oldHitting != null
            && oldHitting.isEnabled()
            && oldHitting.KillauraAutoBlock.getCurrentValue()
            && oldHitting.getAuraTarget() != null;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean isUsingNonShieldOffhandItem() {
      if (mc.player == null || !mc.player.isUsingItem() || mc.player.getUsedItemHand() != InteractionHand.OFF_HAND) {
         return false;
      }

      return mc.player.getOffhandItem().getUseAnimation() != UseAnim.BLOCK;
   }

   private static boolean hasVanillaBlockingUseState() {
      if (mc.player == null || !mc.player.isUsingItem()) {
         return false;
      }

      ItemStack stack = mc.player.getUseItem();
      return !stack.isEmpty() && stack.getUseAnimation() == UseAnim.BLOCK;
   }

   public static boolean shouldUseLegacySwordBlocking(ItemStack stack, Player player, InteractionHand hand) {
      if (!isBlockingFixEnabled() || player == null || mc.level == null || stack == null || hand != InteractionHand.MAIN_HAND) {
         return false;
      }

      Item item = stack.getItem();
      if (!(item instanceof SwordItem)) {
         return false;
      }

      if (isUsingNonShieldOffhandItem()) {
         return false;
      }

      return isTargetOlderThanOrEqualTo("v1_8") || serverLegacyBlockingShield;
   }

   public static boolean shouldUseLegacySwordBlockingStats(ItemStack stack) {
      if (!isBlockingFixEnabled() || mc.player == null || mc.level == null || stack == null || !(stack.getItem() instanceof SwordItem)) {
         return false;
      }

      return (isTargetOlderThanOrEqualTo("v1_8") && !serverLegacyBlockingShield)
         || (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND && ItemStack.isSameItem(mc.player.getUseItem(), stack));
   }

   public static boolean shouldHideServerLegacyBlockingShield(ItemStack stack) {
      return isBlockingFixEnabled()
         && serverLegacyBlockingShield
         && stack != null
         && stack.is(Items.SHIELD)
         && mc.player != null
         && mc.player.getMainHandItem().getItem() instanceof SwordItem
         && isMainHandSwordBlockingInput();
   }

   public static boolean isHighVersionItemFixEnabled() {
      return instance != null && instance.isEnabled() && instance.highVersionItem.getCurrentValue();
   }

   public static boolean isPlacementFixEnabled() {
      return instance != null && instance.isEnabled() && instance.placement.getCurrentValue();
   }

   public static boolean isTransactionFixEnabled() {
      return instance != null && instance.isEnabled() && instance.transactionFix.getCurrentValue();
   }

   public static boolean isTargetOlderThanOrEqualTo(String versionField) {
      try {
         Class<?> translatorClass = Class.forName(PROTOCOL_TRANSLATOR);
         Object targetVersion = translatorClass.getMethod("getTargetVersion").invoke(null);
         Class<?> versionClass = Class.forName(PROTOCOL_VERSION);
         Field field = versionClass.getField(versionField);
         Object version = field.get(null);
         Method method = versionClass.getMethod("olderThanOrEqualTo", versionClass);
         return Boolean.TRUE.equals(method.invoke(targetVersion, version));
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean isTargetNewerThan(String versionField) {
      try {
         Class<?> translatorClass = Class.forName(PROTOCOL_TRANSLATOR);
         Object targetVersion = translatorClass.getMethod("getTargetVersion").invoke(null);
         Class<?> versionClass = Class.forName(PROTOCOL_VERSION);
         Field field = versionClass.getField(versionField);
         Object version = field.get(null);
         Method method = versionClass.getMethod("newerThan", versionClass);
         return Boolean.TRUE.equals(method.invoke(targetVersion, version));
      } catch (Throwable ignored) {
         return false;
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (mc.player == null || mc.level == null) {
         return;
      }

      if (event.getType() == EventType.RECEIVE && this.blocking.getCurrentValue()) {
         this.handleBlockingReceive(event);
      }

      if (event.getType() == EventType.SEND) {
         if (this.blocking.getCurrentValue()) {
            this.handleBlockingSend(event);
         }

         if (!event.isCancelled() && this.placement.getCurrentValue()) {
            this.handlePlacement(event);
         }
      }
   }

   @Override
   public void onDisable() {
      serverLegacyBlockingShield = false;
   }

   private void handleBlockingReceive(EventPacket event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof ClientboundSetEquipmentPacket equipmentPacket && equipmentPacket.getEntity() == mc.player.getId()) {
         for (Pair<EquipmentSlot, ItemStack> pair : equipmentPacket.getSlots()) {
            if (pair.getFirst() == EquipmentSlot.OFFHAND) {
               if (this.isServerLegacyBlockingShield(pair.getSecond())) {
                  serverLegacyBlockingShield = true;
               } else {
                  if (!pair.getSecond().is(Items.SHIELD)) {
                     serverLegacyBlockingShield = false;
                  }
               }
            }
         }
      } else if (packet instanceof ClientboundContainerSetSlotPacket slotPacket
         && slotPacket.getContainerId() == ClientboundContainerSetSlotPacket.PLAYER_INVENTORY
         && slotPacket.getSlot() == 45) {
         if (this.isServerLegacyBlockingShield(slotPacket.getItem())) {
            serverLegacyBlockingShield = true;
         } else if (!slotPacket.getItem().is(Items.SHIELD)) {
            serverLegacyBlockingShield = false;
         }
      }
   }

   private boolean isServerLegacyBlockingShield(ItemStack stack) {
      if (stack == null || !stack.is(Items.SHIELD) || !this.blocking.getCurrentValue()) {
         return false;
      }

      return mc.player != null && mc.player.getMainHandItem().getItem() instanceof SwordItem;
   }

   private void handleBlockingSend(EventPacket event) {
      if (!isTargetOlderThanOrEqualTo("v1_8")) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
         if (useItemOnPacket.getHand() != InteractionHand.MAIN_HAND) {
            event.setCancelled(true);
         } else {
            this.updateViaFabricPlusLastUsedItem(useItemOnPacket.getHand());
         }
      } else if (packet instanceof ServerboundUseItemPacket useItemPacket) {
         if (useItemPacket.getHand() != InteractionHand.MAIN_HAND) {
            event.setCancelled(true);
         } else {
            this.updateViaFabricPlusLastUsedItem(useItemPacket.getHand());
         }
      }
   }

   private void updateViaFabricPlusLastUsedItem(InteractionHand hand) {
      try {
         Class<?> providerClass = Class.forName(VIAFABRICPLUS_HAND_ITEM_PROVIDER);
         Field field = providerClass.getField("lastUsedItem");
         field.set(null, mc.player.getItemInHand(hand).copy());
      } catch (Throwable ignored) {
      }
   }

   private void handlePlacement(EventPacket event) {
      if (!isTargetOlderThanOrEqualTo("v1_18_2")) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket && isTargetOlderThanOrEqualTo("v1_8") && useItemOnPacket.getHand() != InteractionHand.MAIN_HAND) {
         event.setCancelled(true);
      } else if (packet instanceof ServerboundUseItemPacket useItemPacket && isTargetOlderThanOrEqualTo("v1_8") && useItemPacket.getHand() != InteractionHand.MAIN_HAND) {
         event.setCancelled(true);
      } else if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket && useItemOnPacket.getSequence() != 0) {
         event.setPacket(new ServerboundUseItemOnPacket(useItemOnPacket.getHand(), useItemOnPacket.getHitResult(), 0));
      } else if (packet instanceof ServerboundUseItemPacket useItemPacket && useItemPacket.getSequence() != 0) {
         event.setPacket(new ServerboundUseItemPacket(useItemPacket.getHand(), 0));
      }
   }

   public static ViaVersionFix getInstanceOrNull() {
      if (instance != null) {
         return instance;
      }

      try {
         if (Naven.getInstance() != null && Naven.getInstance().getModuleManager() != null) {
            return (ViaVersionFix)Naven.getInstance().getModuleManager().getModule(ViaVersionFix.class);
         }
      } catch (Throwable ignored) {
      }

      return null;
   }
}
