package awa.qwq.ovo.Naven.viaversionfix.items.spear;

import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpearLogic {
   private static final Map<Long, Long> LAST_KINETIC_CONTACTS = new HashMap<>();
   private static final Map<Integer, Integer> LAST_LUNGE_SOUND_TICK = new HashMap<>();

   private SpearLogic() {
   }

   public static void tickKinetic(Level level, LivingEntity attacker, ItemStack stack, SpearMaterial material, int usedTicks) {
      if (level == null || attacker == null || stack == null || stack.isEmpty() || material == null) {
         return;
      }

      if (usedTicks < material.delayTicks()) {
         return;
      }

      if (level.isClientSide) {
         return;
      }

      playLungeSoundOnce(level, attacker, material);
      pushForward(attacker);

      int activeTicks = usedTicks - material.delayTicks();
      List<EntityHitResult> hits = getHitEntitiesAlong(attacker, 1.0F, SpearMaterial.MAX_ATTACK_RANGE, true);
      boolean hitAny = false;
      for (EntityHitResult hit : hits) {
         Entity target = hit.getEntity();
         if (wasRecentlyHit(level, attacker, target)) {
            continue;
         }

         Vec3 look = attacker.getLookAngle();
         double attackerSpeed = Math.max(0.0D, look.dot(attacker.getDeltaMovement().scale(20.0D)));
         double targetSpeed = look.dot(target.getDeltaMovement().scale(20.0D));
         double relativeSpeed = Math.max(0.0D, attackerSpeed - targetSpeed);
         double speedFactor = attacker instanceof Player ? 1.0D : 0.2D;

         boolean dismount = activeTicks <= material.dismountMaxTicks() && attackerSpeed >= material.dismountMinSpeed() * speedFactor;
         boolean knockback = activeTicks <= material.knockbackMaxTicks() && attackerSpeed >= material.knockbackMinSpeed() * speedFactor;
         boolean damage = activeTicks <= material.damageMaxTicks() && relativeSpeed >= material.damageMinRelativeSpeed() * speedFactor;
         if (!dismount && !knockback && !damage) {
            continue;
         }

         float amount = (float)(1.0D + material.attackDamageBonus() + Mth.floor(relativeSpeed * material.damageMultiplier()));
         if (stab(level, attacker, target, amount, damage, knockback, dismount)) {
            rememberHit(level, attacker, target);
            hitAny = true;
         }
      }

      if (hitAny) {
         stack.hurtAndBreak(1, attacker, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
         playHitSound(level, attacker, material);
      }
   }

   public static void release(LivingEntity entity) {
      if (entity != null) {
         LAST_LUNGE_SOUND_TICK.remove(entity.getId());
      }
   }

   public static boolean piercingAttack(Player player, Entity directTarget) {
      if (player == null || player.level().isClientSide || directTarget == null) {
         return false;
      }

      ItemStack stack = player.getMainHandItem();
      SpearMaterial material = getMaterial(stack);
      if (material == null || !(stack.getItem() instanceof SpearItem)) {
         return false;
      }

      if (player.getAttackStrengthScale(0.5F) < 1.0F) {
         return true;
      }

      List<EntityHitResult> hits = getHitEntitiesAlong(player, 1.0F, SpearMaterial.MAX_ATTACK_RANGE, true);
      if (hits.isEmpty() && canHit(player, directTarget)) {
         hits.add(new EntityHitResult(directTarget));
      }

      boolean hitAny = false;
      int knockbackBonus = EnchantmentHelper.getKnockbackBonus(player);
      int fireAspect = EnchantmentHelper.getFireAspect(player);
      for (EntityHitResult hit : hits) {
         Entity target = hit.getEntity();
         float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
         if (target instanceof LivingEntity livingTarget) {
            damage += EnchantmentHelper.getDamageBonus(stack, livingTarget.getMobType());
         }

         if (stab(player.level(), player, target, damage, true, false, false)) {
            applyPiercingEffects(player, target, knockbackBonus, fireAspect);
            hitAny = true;
         }
      }

      player.resetAttackStrengthTicker();
      player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
      SpearLogic.playAttackSound(player.level(), player, material);
      if (hitAny) {
         stack.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
         SpearLogic.playHitSound(player.level(), player, material);
      }

      return true;
   }

   public static void updateClientPick(Minecraft minecraft, float partialTicks) {
      if (minecraft == null || minecraft.player == null || minecraft.level == null) {
         return;
      }

      ItemStack stack = minecraft.player.getMainHandItem();
      if (!InventoryUtils.isSpear(stack)) {
         return;
      }

      double range = minecraft.player.isCreative() ? SpearMaterial.CREATIVE_ATTACK_RANGE : SpearMaterial.MAX_ATTACK_RANGE;
      EntityHitResult hit = getClosestHitEntity(minecraft.player, partialTicks, range, true);
      if (hit == null) {
         return;
      }

      minecraft.hitResult = hit;
      minecraft.crosshairPickEntity = hit.getEntity();
   }

   public static SpearMaterial getMaterial(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      if (stack.getItem() instanceof SpearItem spearItem) {
         return spearItem.getSpearMaterial();
      }

      return InventoryUtils.getServerSpearMaterial(stack);
   }

   public static void playAttackSound(Level level, LivingEntity entity, SpearMaterial material) {
      if (material == null) {
         return;
      }

      playSound(level, entity, material.isWood() ? ModSounds.SPEAR_WOOD_ATTACK : ModSounds.SPEAR_ATTACK, 1.0F, 1.0F);
   }

   public static void playHitSound(Level level, LivingEntity entity, SpearMaterial material) {
      if (material == null) {
         return;
      }

      playSound(level, entity, material.isWood() ? ModSounds.SPEAR_WOOD_HIT : ModSounds.SPEAR_HIT, 1.0F, 1.0F);
   }

   private static List<EntityHitResult> getHitEntitiesAlong(LivingEntity attacker, float partialTicks, double range, boolean respectBlocks) {
      List<EntityHitResult> hits = new ArrayList<>();
      Vec3 start = attacker.getEyePosition(partialTicks);
      Vec3 look = attacker.getViewVector(partialTicks);
      Vec3 end = start.add(look.scale(range));
      double blockDistance = getBlockDistanceSqr(attacker, start, end, respectBlocks);
      AABB searchBox = attacker.getBoundingBox().expandTowards(look.scale(range)).inflate(SpearMaterial.HITBOX_MARGIN + 1.0D);

      for (Entity entity : attacker.level().getEntities(attacker, searchBox, target -> canHit(attacker, target))) {
         AABB box = entity.getBoundingBox().inflate(SpearMaterial.HITBOX_MARGIN);
         Optional<Vec3> clip = box.contains(start) ? Optional.of(start) : box.clip(start, end);
         if (clip.isEmpty()) {
            continue;
         }

         double distance = start.distanceToSqr(clip.get());
         if (distance > blockDistance) {
            continue;
         }

         hits.add(new EntityHitResult(entity, clip.get()));
      }

      hits.sort(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())));
      return hits;
   }

   private static EntityHitResult getClosestHitEntity(LivingEntity attacker, float partialTicks, double range, boolean respectBlocks) {
      List<EntityHitResult> hits = getHitEntitiesAlong(attacker, partialTicks, range, respectBlocks);
      return hits.isEmpty() ? null : hits.get(0);
   }

   private static double getBlockDistanceSqr(LivingEntity attacker, Vec3 start, Vec3 end, boolean respectBlocks) {
      if (!respectBlocks) {
         return start.distanceToSqr(end);
      }

      BlockHitResult blockHit = attacker.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
      if (blockHit.getType() == HitResult.Type.MISS) {
         return start.distanceToSqr(end);
      }

      return start.distanceToSqr(blockHit.getLocation());
   }

   private static boolean canHit(LivingEntity attacker, Entity target) {
      if (target == null || target == attacker || !target.isAlive() || target.isSpectator()) {
         return false;
      }

      if (!target.isAttackable() && !target.canBeHitByProjectile()) {
         return false;
      }

      if (target.isPassengerOfSameVehicle(attacker)) {
         return false;
      }

      if (attacker instanceof Player player && target instanceof Player targetPlayer && !player.canHarmPlayer(targetPlayer)) {
         return false;
      }

      return true;
   }

   private static boolean stab(Level level, LivingEntity attacker, Entity target, float amount, boolean damage, boolean knockback, boolean dismount) {
      return stab(level, attacker, target, amount, damage, knockback, dismount, 0.6D);
   }

   private static boolean stab(
      Level level,
      LivingEntity attacker,
      Entity target,
      float amount,
      boolean damage,
      boolean knockback,
      boolean dismount,
      double knockbackStrength
   ) {
      boolean changed = false;
      if (damage) {
         changed = target.hurt(getDamageSource(level, attacker), amount);
      }

      if (knockback && target instanceof LivingEntity livingTarget) {
         Vec3 look = attacker.getLookAngle();
         livingTarget.knockback(knockbackStrength, -look.x, -look.z);
         changed = true;
      }

      if (dismount && target.isPassenger()) {
         target.stopRiding();
         changed = true;
      }

      return changed;
   }

   private static void applyPiercingEffects(Player player, Entity target, int knockbackBonus, int fireAspect) {
      if (knockbackBonus > 0 && target instanceof LivingEntity livingTarget) {
         Vec3 look = player.getLookAngle();
         livingTarget.knockback(0.4D + knockbackBonus * 0.5D, -look.x, -look.z);
      }

      if (fireAspect > 0) {
         target.setSecondsOnFire(fireAspect * 4);
      }

      if (target instanceof LivingEntity livingTarget) {
         EnchantmentHelper.doPostHurtEffects(livingTarget, player);
      }

      EnchantmentHelper.doPostDamageEffects(player, target);
   }

   private static DamageSource getDamageSource(Level level, LivingEntity attacker) {
      if (attacker instanceof Player player) {
         return level.damageSources().playerAttack(player);
      }

      return level.damageSources().mobAttack(attacker);
   }

   private static void pushForward(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
      if (horizontal.lengthSqr() > 1.0E-6D) {
         horizontal = horizontal.normalize().scale(SpearMaterial.FORWARD_MOVEMENT);
      }

      Vec3 current = entity.getDeltaMovement();
      double y = Math.max(current.y, look.y * 0.15D);
      entity.setDeltaMovement(horizontal.x, y, horizontal.z);
   }

   private static boolean wasRecentlyHit(Level level, LivingEntity attacker, Entity target) {
      long key = contactKey(attacker, target);
      Long lastTick = LAST_KINETIC_CONTACTS.get(key);
      return lastTick != null && level.getGameTime() - lastTick < SpearMaterial.CONTACT_COOLDOWN_TICKS;
   }

   private static void rememberHit(Level level, LivingEntity attacker, Entity target) {
      LAST_KINETIC_CONTACTS.put(contactKey(attacker, target), level.getGameTime());
      LAST_KINETIC_CONTACTS.entrySet().removeIf(entry -> level.getGameTime() - entry.getValue() > 100L);
   }

   private static long contactKey(Entity attacker, Entity target) {
      return ((long)attacker.getId() << 32) ^ (target.getId() & 0xffffffffL);
   }

   private static void playLungeSoundOnce(Level level, LivingEntity attacker, SpearMaterial material) {
      int lastTick = LAST_LUNGE_SOUND_TICK.getOrDefault(attacker.getId(), -1000);
      if (attacker.tickCount - lastTick <= 10) {
         return;
      }

      LAST_LUNGE_SOUND_TICK.put(attacker.getId(), attacker.tickCount);
      SoundEvent sound = switch (attacker.getRandom().nextInt(3)) {
         case 0 -> ModSounds.SPEAR_LUNGE_1;
         case 1 -> ModSounds.SPEAR_LUNGE_2;
         default -> ModSounds.SPEAR_LUNGE_3;
      };
      playSound(level, attacker, sound, material.isWood() ? 0.7F : 0.8F, 1.0F);
   }

   private static void playSound(Level level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      if (level == null || entity == null || sound == null) {
         return;
      }

      if (level.isClientSide) {
         level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
      } else {
         level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
      }
   }
}
