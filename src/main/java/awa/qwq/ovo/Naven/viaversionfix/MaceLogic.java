package awa.qwq.ovo.Naven.viaversionfix;

import awa.qwq.ovo.Naven.viaversionfix.items.MaceItem;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class MaceLogic {
   private MaceLogic() {
   }

   public static void handlePostHit(Level level, Entity target, LivingEntity attacker, ItemStack stack) {
      if (level == null || target == null || attacker == null || stack == null || stack.isEmpty()) {
         return;
      }

      float fallDistance = attacker.fallDistance;
      if (!canSmashAttack(attacker)) {
         return;
      }

      if (!level.isClientSide) {
         attacker.fallDistance = 0.0F;
         attacker.setDeltaMovement(Vec3.ZERO);

         float smashDamage = getSmashDamage(fallDistance);
         target.hurt(getDamageSource(level, attacker), smashDamage);
         if (target instanceof EnderDragon) {
            target.hurt(level.damageSources().explosion(attacker, attacker), smashDamage);
         }

         if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
               ParticleTypes.EXPLOSION,
               attacker.getX(),
               attacker.getY() + 1.5D,
               attacker.getZ(),
               25,
               4.0D,
               0.0D,
               4.0D,
               1.0D
            );
         }

         playSmashSound(level, attacker, target, fallDistance);
      }
   }

   public static void playClientSmashSound(Level level, Entity target, LivingEntity attacker) {
      if (level == null || target == null || attacker == null || !level.isClientSide || !canSmashAttack(attacker)) {
         return;
      }

      playSmashSound(level, attacker, target, attacker.fallDistance, true);
   }

   private static boolean canSmashAttack(LivingEntity attacker) {
      return attacker.fallDistance > MaceItem.SMASH_MIN_FALL_DISTANCE && !attacker.isFallFlying();
   }

   private static float getSmashDamage(float fallDistance) {
      return 1.5F * fallDistance * 3.0F;
   }

   private static DamageSource getDamageSource(Level level, LivingEntity attacker) {
      if (attacker instanceof Player player) {
         return level.damageSources().playerAttack(player);
      }

      return level.damageSources().mobAttack(attacker);
   }

   private static void playSmashSound(Level level, LivingEntity attacker, Entity target, float fallDistance) {
      playSmashSound(level, attacker, target, fallDistance, false);
   }

   private static void playSmashSound(Level level, LivingEntity attacker, Entity target, float fallDistance, boolean local) {
      SoundEvent sound = getSmashSound(target, fallDistance);
      if (local) {
         level.playLocalSound(attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundSource(), 1.0F, 1.0F, false);
      } else {
         level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundSource(), 1.0F, 1.0F);
      }
   }

   private static SoundEvent getSmashSound(Entity target, float fallDistance) {
      if (!target.onGround()) {
         return ModSounds.VANILLA_MACE_SMASH_AIR;
      }

      if (fallDistance > 5.0F) {
         return ModSounds.VANILLA_MACE_SMASH_GROUND_HEAVY;
      }

      return ModSounds.VANILLA_MACE_SMASH_GROUND;
   }
}
