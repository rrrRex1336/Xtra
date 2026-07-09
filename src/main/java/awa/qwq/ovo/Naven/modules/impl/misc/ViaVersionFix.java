package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.OldHitting;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;

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
   private static final int WIND_CHARGE_SPAWN_GRACE_TICKS = 10;
   private static final int WIND_CHARGE_TRACK_TICKS = 80;
   private static final double WIND_CHARGE_SPAWN_DISTANCE_SQR = 36.0D;
   private static final double WIND_CHARGE_BURST_DISTANCE_SQR = 25.0D;
   private static boolean serverLegacyBlockingShield;
   private final Map<Integer, TrackedViaWindCharge> viaWindCharges = new HashMap<>();
   private int lastServerWindChargeUseTick = -1000;
   private int lastWindChargeThrowSoundTick = -1000;
   private int lastWindChargeBurstSoundTick = -1000;
   private Vec3d lastWindChargeBurstPos = Vec3d.ZERO;

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
      if (!isBlockingFixEnabled() || mc.player == null || mc.world == null) {
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
      if (mc.player == null || !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
         return false;
      }

      if (isUsingNonShieldOffhandItem()) {
         return false;
      }

      if (mc.options.useKey.isPressed()) {
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
      if (mc.player == null || !mc.player.isUsingItem() || mc.player.getActiveHand() != Hand.OFF_HAND) {
         return false;
      }

      return mc.player.getOffHandStack().getUseAction() != UseAction.BLOCK;
   }

   private static boolean hasVanillaBlockingUseState() {
      if (mc.player == null || !mc.player.isUsingItem()) {
         return false;
      }

      ItemStack stack = mc.player.getActiveItem();
      return !stack.isEmpty() && stack.getUseAction() == UseAction.BLOCK;
   }

   public static boolean shouldUseLegacySwordBlocking(ItemStack stack, PlayerEntity player, Hand hand) {
      if (!isBlockingFixEnabled() || player == null || mc.world == null || stack == null || hand != Hand.MAIN_HAND) {
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
      if (!isBlockingFixEnabled() || mc.player == null || mc.world == null || stack == null || !(stack.getItem() instanceof SwordItem)) {
         return false;
      }

      return (isTargetOlderThanOrEqualTo("v1_8") && !serverLegacyBlockingShield)
         || (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND && ItemStack.areItemsEqual(mc.player.getActiveItem(), stack));
   }

   public static boolean shouldHideServerLegacyBlockingShield(ItemStack stack) {
      return isBlockingFixEnabled()
         && serverLegacyBlockingShield
         && stack != null
         && stack.isOf(Items.SHIELD)
         && mc.player != null
         && mc.player.getMainHandStack().getItem() instanceof SwordItem
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
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (this.highVersionItem.getCurrentValue()) {
         this.handleWindChargePackets(event);
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
      this.viaWindCharges.clear();
      this.lastServerWindChargeUseTick = -1000;
   }

   private void handleWindChargePackets(EventPacket event) {
      Packet<?> packet = event.getPacket();
      if (event.getType() == EventType.SEND) {
         if (packet instanceof PlayerInteractItemC2SPacket useItemPacket) {
            this.rememberServerWindChargeUse(useItemPacket.getHand());
         } else if (packet instanceof PlayerInteractBlockC2SPacket useItemOnPacket) {
            this.rememberServerWindChargeUse(useItemOnPacket.getHand());
         }

         return;
      }

      if (event.getType() != EventType.RECEIVE) {
         return;
      }

      this.pruneViaWindCharges();

      if (packet instanceof EntitySpawnS2CPacket addEntityPacket) {
         this.handleWindChargeEntitySpawn(addEntityPacket);
      } else if (packet instanceof ExplosionS2CPacket explodePacket) {
         this.handleWindChargeExplosion(explodePacket);
      } else if (packet instanceof EntitiesDestroyS2CPacket removeEntitiesPacket) {
         this.handleWindChargeEntityRemove(removeEntitiesPacket);
      }
   }

   private void rememberServerWindChargeUse(Hand hand) {
      ItemStack stack = mc.player.getStackInHand(hand);
      if (InventoryUtils.isServerWindCharge(stack)) {
         this.lastServerWindChargeUseTick = mc.player.age;
      }
   }

   private void handleWindChargeEntitySpawn(EntitySpawnS2CPacket packet) {
      if (packet.getEntityType() != EntityType.SHULKER_BULLET || !this.isRecentServerWindChargeUse()) {
         return;
      }

      Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
      if (pos.squaredDistanceTo(mc.player.getEyePos()) > WIND_CHARGE_SPAWN_DISTANCE_SQR) {
         return;
      }

      this.viaWindCharges.put(packet.getId(), new TrackedViaWindCharge(pos, mc.player.age));
      this.playWindChargeThrow(pos);
   }

   private boolean isRecentServerWindChargeUse() {
      return mc.player.age - this.lastServerWindChargeUseTick <= WIND_CHARGE_SPAWN_GRACE_TICKS;
   }

   private void handleWindChargeExplosion(ExplosionS2CPacket packet) {
      Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
      Integer id = this.findTrackedWindChargeNear(pos);
      if (id == null) {
         return;
      }

      this.viaWindCharges.remove(id);
      this.playWindChargeBurst(pos);
   }

   private Integer findTrackedWindChargeNear(Vec3d pos) {
      Integer closestId = null;
      double closestDistance = WIND_CHARGE_BURST_DISTANCE_SQR;

      for (Map.Entry<Integer, TrackedViaWindCharge> entry : this.viaWindCharges.entrySet()) {
         Vec3d trackedPos = this.getTrackedWindChargePosition(entry.getKey(), entry.getValue());
         double distance = trackedPos.squaredDistanceTo(pos);
         if (distance <= closestDistance) {
            closestDistance = distance;
            closestId = entry.getKey();
         }
      }

      return closestId;
   }

   private void handleWindChargeEntityRemove(EntitiesDestroyS2CPacket packet) {
      for (int entityId : packet.getEntityIds()) {
         TrackedViaWindCharge tracked = this.viaWindCharges.remove(entityId);
         if (tracked != null) {
            this.playWindChargeBurst(this.getTrackedWindChargePosition(entityId, tracked));
         }
      }
   }

   private Vec3d getTrackedWindChargePosition(int entityId, TrackedViaWindCharge tracked) {
      Entity entity = mc.world.getEntityById(entityId);
      return entity != null ? entity.getPos() : tracked.lastKnownPos;
   }

   private void pruneViaWindCharges() {
      Iterator<Map.Entry<Integer, TrackedViaWindCharge>> iterator = this.viaWindCharges.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<Integer, TrackedViaWindCharge> entry = iterator.next();
         if (mc.player.age - entry.getValue().spawnTick > WIND_CHARGE_TRACK_TICKS) {
            iterator.remove();
         }
      }
   }

   private void playWindChargeThrow(Vec3d pos) {
      if (mc.player.age - this.lastWindChargeThrowSoundTick <= 1) {
         return;
      }

      this.lastWindChargeThrowSoundTick = mc.player.age;
      mc.world.playSound(pos.x, pos.y, pos.z, ModSounds.WIND_CHARGE_THROW, SoundCategory.NEUTRAL, 0.5F, 1.0F, false);
   }

   private void playWindChargeBurst(Vec3d pos) {
      if (mc.player.age - this.lastWindChargeBurstSoundTick <= 1 && this.lastWindChargeBurstPos.squaredDistanceTo(pos) <= 4.0D) {
         return;
      }

      this.lastWindChargeBurstSoundTick = mc.player.age;
      this.lastWindChargeBurstPos = pos;
      mc.world.addParticle(ParticleTypes.GUST_EMITTER, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
      mc.world.playSound(pos.x, pos.y, pos.z, ModSounds.WIND_CHARGE_WIND_BURST, SoundCategory.NEUTRAL, 1.0F, 1.25F, false);
   }

   private void handleBlockingReceive(EventPacket event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentPacket && equipmentPacket.getId() == mc.player.getId()) {
         for (Pair<EquipmentSlot, ItemStack> pair : equipmentPacket.getEquipmentList()) {
            if (pair.getFirst() == EquipmentSlot.OFFHAND) {
               if (this.isServerLegacyBlockingShield(pair.getSecond())) {
                  serverLegacyBlockingShield = true;
               } else {
                  if (!pair.getSecond().isOf(Items.SHIELD)) {
                     serverLegacyBlockingShield = false;
                  }
               }
            }
         }
      } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slotPacket
         && slotPacket.getSyncId() == ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID
         && slotPacket.getSlot() == 45) {
         if (this.isServerLegacyBlockingShield(slotPacket.getStack())) {
            serverLegacyBlockingShield = true;
         } else if (!slotPacket.getStack().isOf(Items.SHIELD)) {
            serverLegacyBlockingShield = false;
         }
      }
   }

   private boolean isServerLegacyBlockingShield(ItemStack stack) {
      if (stack == null || !stack.isOf(Items.SHIELD) || !this.blocking.getCurrentValue()) {
         return false;
      }

      return mc.player != null && mc.player.getMainHandStack().getItem() instanceof SwordItem;
   }

   private void handleBlockingSend(EventPacket event) {
      if (!isTargetOlderThanOrEqualTo("v1_8")) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (packet instanceof PlayerInteractBlockC2SPacket useItemOnPacket) {
         if (useItemOnPacket.getHand() != Hand.MAIN_HAND) {
            event.setCancelled(true);
         } else {
            this.updateViaFabricPlusLastUsedItem(useItemOnPacket.getHand());
         }
      } else if (packet instanceof PlayerInteractItemC2SPacket useItemPacket) {
         if (useItemPacket.getHand() != Hand.MAIN_HAND) {
            event.setCancelled(true);
         } else {
            this.updateViaFabricPlusLastUsedItem(useItemPacket.getHand());
         }
      }
   }

   private void updateViaFabricPlusLastUsedItem(Hand hand) {
      try {
         Class<?> providerClass = Class.forName(VIAFABRICPLUS_HAND_ITEM_PROVIDER);
         Field field = providerClass.getField("lastUsedItem");
         field.set(null, mc.player.getStackInHand(hand).copy());
      } catch (Throwable ignored) {
      }
   }

   private void handlePlacement(EventPacket event) {
      if (!isTargetOlderThanOrEqualTo("v1_18_2")) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (packet instanceof PlayerInteractBlockC2SPacket useItemOnPacket && isTargetOlderThanOrEqualTo("v1_8") && useItemOnPacket.getHand() != Hand.MAIN_HAND) {
         event.setCancelled(true);
      } else if (packet instanceof PlayerInteractItemC2SPacket useItemPacket && isTargetOlderThanOrEqualTo("v1_8") && useItemPacket.getHand() != Hand.MAIN_HAND) {
         event.setCancelled(true);
      } else if (packet instanceof PlayerInteractBlockC2SPacket useItemOnPacket && useItemOnPacket.getSequence() != 0) {
         event.setPacket(new PlayerInteractBlockC2SPacket(useItemOnPacket.getHand(), useItemOnPacket.getBlockHitResult(), 0));
      } else if (packet instanceof PlayerInteractItemC2SPacket useItemPacket && useItemPacket.getSequence() != 0) {
         event.setPacket(new PlayerInteractItemC2SPacket(useItemPacket.getHand(), 0));
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

   private static final class TrackedViaWindCharge {
      private final Vec3d lastKnownPos;
      private final int spawnTick;

      private TrackedViaWindCharge(Vec3d lastKnownPos, int spawnTick) {
         this.lastKnownPos = lastKnownPos;
         this.spawnTick = spawnTick;
      }
   }
}
