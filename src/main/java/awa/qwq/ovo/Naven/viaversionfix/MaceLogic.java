package awa.qwq.ovo.Naven.viaversionfix;

import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import awa.qwq.ovo.Naven.viaversionfix.items.mace.MaceItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MaceLogic {
   private MaceLogic() {
   }

   public static void handlePostHit(World level, Entity target, LivingEntity attacker, ItemStack stack) {
      if (level == null || target == null || attacker == null || stack == null || stack.isEmpty()) {
         return;
      }

      float fallDistance = attacker.fallDistance;
      if (!canSmashAttack(attacker)) {
         return;
      }

      if (!level.isClient) {
         attacker.fallDistance = 0.0F;
         attacker.setVelocity(Vec3d.ZERO);

         float smashDamage = getSmashDamage(fallDistance);
         target.damage(getDamageSource(level, attacker), smashDamage);
         if (target instanceof EnderDragonEntity) {
            target.damage(level.getDamageSources().explosion(attacker, attacker), smashDamage);
         }

         if (level instanceof ServerWorld serverLevel) {
            serverLevel.spawnParticles(
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

   public static void playClientSmashSound(World level, Entity target, LivingEntity attacker) {
      if (level == null || target == null || attacker == null || !level.isClient || !canSmashAttack(attacker)) {
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

   private static DamageSource getDamageSource(World level, LivingEntity attacker) {
      if (attacker instanceof PlayerEntity player) {
         return level.getDamageSources().playerAttack(player);
      }

      return level.getDamageSources().mobAttack(attacker);
   }

   private static void playSmashSound(World level, LivingEntity attacker, Entity target, float fallDistance) {
      playSmashSound(level, attacker, target, fallDistance, false);
   }

   private static void playSmashSound(World level, LivingEntity attacker, Entity target, float fallDistance, boolean local) {
      SoundEvent sound = getSmashSound(target, fallDistance);
      if (local) {
         level.playSound(attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundCategory(), 1.0F, 1.0F, false);
      } else {
         level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundCategory(), 1.0F, 1.0F);
      }
   }

   private static SoundEvent getSmashSound(Entity target, float fallDistance) {
      if (!target.isOnGround()) {
         return ModSounds.VANILLA_MACE_SMASH_AIR;
      }

      if (fallDistance > 5.0F) {
         return ModSounds.VANILLA_MACE_SMASH_GROUND_HEAVY;
      }

      return ModSounds.VANILLA_MACE_SMASH_GROUND;
   }
}
