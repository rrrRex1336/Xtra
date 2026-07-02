package awa.qwq.ovo.Naven.viaversionfix.items.windcharge;

import awa.qwq.ovo.Naven.viaversionfix.items.ModEntities;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

public class WindChargeProjectile extends AbstractHurtingProjectile implements ItemSupplier {
   private static final float EXPLOSION_RADIUS = 1.2F;
   private static final double BLOCK_HIT_OFFSET = 0.25D;
   private static final ExplosionDamageCalculator WIND_CHARGE_DAMAGE_CALCULATOR = new ExplosionDamageCalculator() {
      @Override
      public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
         return false;
      }

      @Override
      public float getEntityDamageAmount(Explosion explosion, Entity entity) {
         return 0.0F;
      }
   };

   public WindChargeProjectile(EntityType<WindChargeProjectile> type, Level level) {
      super(type, level);
      this.xPower = 0.0D;
      this.yPower = 0.0D;
      this.zPower = 0.0D;
   }

   public WindChargeProjectile(Level level, LivingEntity owner) {
      this(ModEntities.WIND_CHARGE_PROJECTILE, level);
      this.setOwner(owner);
      this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
   }

   @Override
   public boolean canCollideWith(Entity entity) {
      return !(entity instanceof WindChargeProjectile) && super.canCollideWith(entity);
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return !(entity instanceof WindChargeProjectile) && !entity.is(this.getOwner()) && super.canHitEntity(entity);
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level().isClientSide) {
         return;
      }

      Entity owner = this.getOwner();
      LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
      result.getEntity().hurt(this.damageSources().mobProjectile(this, livingOwner), 1.0F);
      this.explode(this.position());
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide) {
         Vec3 offset = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(BLOCK_HIT_OFFSET);
         this.explode(result.getLocation().add(offset));
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide) {
         this.discard();
      }
   }

   private void explode(Vec3 pos) {
      this.level()
         .explode(
            this,
            null,
            WIND_CHARGE_DAMAGE_CALCULATOR,
            pos.x,
            pos.y,
            pos.z,
            EXPLOSION_RADIUS,
            false,
            Level.ExplosionInteraction.NONE,
            ParticleTypes.GUST,
            ParticleTypes.GUST_EMITTER,
            ModSounds.WIND_CHARGE_WIND_BURST
         );
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }

   @Override
   protected float getInertia() {
      return 1.0F;
   }

   @Override
   protected float getLiquidInertia() {
      return this.getInertia();
   }

   @Override
   protected ParticleOptions getTrailParticle() {
      return ParticleTypes.GUST;
   }

   @Override
   protected ClipContext.Block getClipType() {
      return ClipContext.Block.OUTLINE;
   }

   @Override
   public ItemStack getItem() {
      return ModItems.WIND_CHARGE_RENDER_STACK;
   }
}
