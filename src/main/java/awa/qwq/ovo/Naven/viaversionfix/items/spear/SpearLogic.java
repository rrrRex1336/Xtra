package awa.qwq.ovo.Naven.viaversionfix.items.spear;

import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class SpearLogic {
   private static final Map<Long, Long> LAST_KINETIC_CONTACTS = new HashMap<>();
   private static final Map<Integer, Integer> LAST_LUNGE_SOUND_TICK = new HashMap<>();

   private SpearLogic() {
   }

   public static void tickKinetic(World level, LivingEntity attacker, ItemStack stack, SpearMaterial material, int usedTicks) {
      if (level == null || attacker == null || stack == null || stack.isEmpty() || material == null) {
         return;
      }

      if (usedTicks < material.delayTicks()) {
         return;
      }

      if (level.isClient) {
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

         Vec3d look = attacker.getRotationVector();
         double attackerSpeed = Math.max(0.0D, look.dotProduct(attacker.getVelocity().multiply(20.0D)));
         double targetSpeed = look.dotProduct(target.getVelocity().multiply(20.0D));
         double relativeSpeed = Math.max(0.0D, attackerSpeed - targetSpeed);
         double speedFactor = attacker instanceof PlayerEntity ? 1.0D : 0.2D;

         boolean dismount = activeTicks <= material.dismountMaxTicks() && attackerSpeed >= material.dismountMinSpeed() * speedFactor;
         boolean knockback = activeTicks <= material.knockbackMaxTicks() && attackerSpeed >= material.knockbackMinSpeed() * speedFactor;
         boolean damage = activeTicks <= material.damageMaxTicks() && relativeSpeed >= material.damageMinRelativeSpeed() * speedFactor;
         if (!dismount && !knockback && !damage) {
            continue;
         }

         float amount = (float)(1.0D + material.attackDamageBonus() + MathHelper.floor(relativeSpeed * material.damageMultiplier()));
         if (stab(level, attacker, target, amount, damage, knockback, dismount)) {
            rememberHit(level, attacker, target);
            hitAny = true;
         }
      }

      if (hitAny) {
         stack.damage(1, attacker, living -> living.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
         playHitSound(level, attacker, material);
      }
   }

   public static void release(LivingEntity entity) {
      if (entity != null) {
         LAST_LUNGE_SOUND_TICK.remove(entity.getId());
      }
   }

   public static boolean piercingAttack(PlayerEntity player, Entity directTarget) {
      if (player == null || player.getWorld().isClient || directTarget == null) {
         return false;
      }

      ItemStack stack = player.getMainHandStack();
      SpearMaterial material = getMaterial(stack);
      if (material == null || !(stack.getItem() instanceof SpearItem)) {
         return false;
      }

      if (player.getAttackCooldownProgress(0.5F) < 1.0F) {
         return true;
      }

      List<EntityHitResult> hits = getHitEntitiesAlong(player, 1.0F, SpearMaterial.MAX_ATTACK_RANGE, true);
      if (hits.isEmpty() && canHit(player, directTarget)) {
         hits.add(new EntityHitResult(directTarget));
      }

      boolean hitAny = false;
      int knockbackBonus = EnchantmentHelper.getKnockback(player);
      int fireAspect = EnchantmentHelper.getFireAspect(player);
      for (EntityHitResult hit : hits) {
         Entity target = hit.getEntity();
         float damage = (float)player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
         if (target instanceof LivingEntity livingTarget) {
            damage += EnchantmentHelper.getAttackDamage(stack, livingTarget.getGroup());
         }

         if (stab(player.getWorld(), player, target, damage, true, false, false)) {
            applyPiercingEffects(player, target, knockbackBonus, fireAspect);
            hitAny = true;
         }
      }

      player.resetLastAttackedTicks();
      player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
      SpearLogic.playAttackSound(player.getWorld(), player, material);
      if (hitAny) {
         stack.damage(1, player, living -> living.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
         SpearLogic.playHitSound(player.getWorld(), player, material);
      }

      return true;
   }

   public static void updateClientPick(MinecraftClient minecraft, float partialTicks) {
      if (minecraft == null || minecraft.player == null || minecraft.world == null) {
         return;
      }

      ItemStack stack = minecraft.player.getMainHandStack();
      if (!InventoryUtils.isSpear(stack)) {
         return;
      }

      double range = minecraft.player.isCreative() ? SpearMaterial.CREATIVE_ATTACK_RANGE : SpearMaterial.MAX_ATTACK_RANGE;
      EntityHitResult hit = getClosestHitEntity(minecraft.player, partialTicks, range, true);
      if (hit == null) {
         return;
      }

      minecraft.crosshairTarget = hit;
      minecraft.targetedEntity = hit.getEntity();
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

   public static void playAttackSound(World level, LivingEntity entity, SpearMaterial material) {
      if (material == null) {
         return;
      }

      playSound(level, entity, material.isWood() ? ModSounds.SPEAR_WOOD_ATTACK : ModSounds.SPEAR_ATTACK, 1.0F, 1.0F);
   }

   public static void playHitSound(World level, LivingEntity entity, SpearMaterial material) {
      if (material == null) {
         return;
      }

      playSound(level, entity, material.isWood() ? ModSounds.SPEAR_WOOD_HIT : ModSounds.SPEAR_HIT, 1.0F, 1.0F);
   }

   private static List<EntityHitResult> getHitEntitiesAlong(LivingEntity attacker, float partialTicks, double range, boolean respectBlocks) {
      List<EntityHitResult> hits = new ArrayList<>();
      Vec3d start = attacker.getCameraPosVec(partialTicks);
      Vec3d look = attacker.getRotationVec(partialTicks);
      Vec3d end = start.add(look.multiply(range));
      double blockDistance = getBlockDistanceSqr(attacker, start, end, respectBlocks);
      Box searchBox = attacker.getBoundingBox().stretch(look.multiply(range)).expand(SpearMaterial.HITBOX_MARGIN + 1.0D);

      for (Entity entity : attacker.getWorld().getOtherEntities(attacker, searchBox, target -> canHit(attacker, target))) {
         Box box = entity.getBoundingBox().expand(SpearMaterial.HITBOX_MARGIN);
         Optional<Vec3d> clip = box.contains(start) ? Optional.of(start) : box.raycast(start, end);
         if (clip.isEmpty()) {
            continue;
         }

         double distance = start.squaredDistanceTo(clip.get());
         if (distance > blockDistance) {
            continue;
         }

         hits.add(new EntityHitResult(entity, clip.get()));
      }

      hits.sort(Comparator.comparingDouble(hit -> start.squaredDistanceTo(hit.getPos())));
      return hits;
   }

   private static EntityHitResult getClosestHitEntity(LivingEntity attacker, float partialTicks, double range, boolean respectBlocks) {
      List<EntityHitResult> hits = getHitEntitiesAlong(attacker, partialTicks, range, respectBlocks);
      return hits.isEmpty() ? null : hits.get(0);
   }

   private static double getBlockDistanceSqr(LivingEntity attacker, Vec3d start, Vec3d end, boolean respectBlocks) {
      if (!respectBlocks) {
         return start.squaredDistanceTo(end);
      }

      BlockHitResult blockHit = attacker.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, attacker));
      if (blockHit.getType() == HitResult.Type.MISS) {
         return start.squaredDistanceTo(end);
      }

      return start.squaredDistanceTo(blockHit.getPos());
   }

   private static boolean canHit(LivingEntity attacker, Entity target) {
      if (target == null || target == attacker || !target.isAlive() || target.isSpectator()) {
         return false;
      }

      if (!target.isAttackable() && !target.canBeHitByProjectile()) {
         return false;
      }

      if (target.isConnectedThroughVehicle(attacker)) {
         return false;
      }

      if (attacker instanceof PlayerEntity player && target instanceof PlayerEntity targetPlayer && !player.shouldDamagePlayer(targetPlayer)) {
         return false;
      }

      return true;
   }

   private static boolean stab(World level, LivingEntity attacker, Entity target, float amount, boolean damage, boolean knockback, boolean dismount) {
      return stab(level, attacker, target, amount, damage, knockback, dismount, 0.6D);
   }

   private static boolean stab(
      World level,
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
         changed = target.damage(getDamageSource(level, attacker), amount);
      }

      if (knockback && target instanceof LivingEntity livingTarget) {
         Vec3d look = attacker.getRotationVector();
         livingTarget.takeKnockback(knockbackStrength, -look.x, -look.z);
         changed = true;
      }

      if (dismount && target.hasVehicle()) {
         target.stopRiding();
         changed = true;
      }

      return changed;
   }

   private static void applyPiercingEffects(PlayerEntity player, Entity target, int knockbackBonus, int fireAspect) {
      if (knockbackBonus > 0 && target instanceof LivingEntity livingTarget) {
         Vec3d look = player.getRotationVector();
         livingTarget.takeKnockback(0.4D + knockbackBonus * 0.5D, -look.x, -look.z);
      }

      if (fireAspect > 0) {
         target.setOnFireFor(fireAspect * 4);
      }

      if (target instanceof LivingEntity livingTarget) {
         EnchantmentHelper.onUserDamaged(livingTarget, player);
      }

      EnchantmentHelper.onTargetDamaged(player, target);
   }

   private static DamageSource getDamageSource(World level, LivingEntity attacker) {
      if (attacker instanceof PlayerEntity player) {
         return level.getDamageSources().playerAttack(player);
      }

      return level.getDamageSources().mobAttack(attacker);
   }

   private static void pushForward(LivingEntity entity) {
      Vec3d look = entity.getRotationVector();
      Vec3d horizontal = new Vec3d(look.x, 0.0D, look.z);
      if (horizontal.lengthSquared() > 1.0E-6D) {
         horizontal = horizontal.normalize().multiply(SpearMaterial.FORWARD_MOVEMENT);
      }

      Vec3d current = entity.getVelocity();
      double y = Math.max(current.y, look.y * 0.15D);
      entity.setVelocity(horizontal.x, y, horizontal.z);
   }

   private static boolean wasRecentlyHit(World level, LivingEntity attacker, Entity target) {
      long key = contactKey(attacker, target);
      Long lastTick = LAST_KINETIC_CONTACTS.get(key);
      return lastTick != null && level.getTime() - lastTick < SpearMaterial.CONTACT_COOLDOWN_TICKS;
   }

   private static void rememberHit(World level, LivingEntity attacker, Entity target) {
      LAST_KINETIC_CONTACTS.put(contactKey(attacker, target), level.getTime());
      LAST_KINETIC_CONTACTS.entrySet().removeIf(entry -> level.getTime() - entry.getValue() > 100L);
   }

   private static long contactKey(Entity attacker, Entity target) {
      return ((long)attacker.getId() << 32) ^ (target.getId() & 0xffffffffL);
   }

   private static void playLungeSoundOnce(World level, LivingEntity attacker, SpearMaterial material) {
      int lastTick = LAST_LUNGE_SOUND_TICK.getOrDefault(attacker.getId(), -1000);
      if (attacker.age - lastTick <= 10) {
         return;
      }

      LAST_LUNGE_SOUND_TICK.put(attacker.getId(), attacker.age);
      SoundEvent sound = switch (attacker.getRandom().nextInt(3)) {
         case 0 -> ModSounds.SPEAR_LUNGE_1;
         case 1 -> ModSounds.SPEAR_LUNGE_2;
         default -> ModSounds.SPEAR_LUNGE_3;
      };
      playSound(level, attacker, sound, material.isWood() ? 0.7F : 0.8F, 1.0F);
   }

   private static void playSound(World level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      if (level == null || entity == null || sound == null) {
         return;
      }

      if (level.isClient) {
         level.playSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, volume, pitch, false);
      } else {
         level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
      }
   }
}
