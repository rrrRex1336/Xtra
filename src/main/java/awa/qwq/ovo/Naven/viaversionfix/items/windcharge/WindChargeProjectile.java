package awa.qwq.ovo.Naven.viaversionfix.items.windcharge;

import awa.qwq.ovo.Naven.viaversionfix.items.ModEntities;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

public class WindChargeProjectile extends ExplosiveProjectileEntity implements FlyingItemEntity {
   private static final float EXPLOSION_RADIUS = 1.2F;
   private static final double BLOCK_HIT_OFFSET = 0.25D;
   private static final ExplosionBehavior WIND_CHARGE_DAMAGE_CALCULATOR = new ExplosionBehavior() {
      @Override
      public boolean canDestroyBlock(Explosion explosion, BlockView level, BlockPos pos, BlockState state, float power) {
         return false;
      }

      @Override
      public float calculateDamage(Explosion explosion, Entity entity) {
         return 0.0F;
      }
   };

   public WindChargeProjectile(EntityType<WindChargeProjectile> type, World level) {
      super(type, level);
      this.powerX = 0.0D;
      this.powerY = 0.0D;
      this.powerZ = 0.0D;
   }

   public WindChargeProjectile(World level, LivingEntity owner) {
      this(ModEntities.WIND_CHARGE_PROJECTILE, level);
      this.setOwner(owner);
      this.setPosition(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
   }

   @Override
   public boolean collidesWith(Entity entity) {
      return !(entity instanceof WindChargeProjectile) && super.collidesWith(entity);
   }

   @Override
   protected boolean canHit(Entity entity) {
      return !(entity instanceof WindChargeProjectile) && !entity.isPartOf(this.getOwner()) && super.canHit(entity);
   }

   @Override
   protected void onEntityHit(EntityHitResult result) {
      super.onEntityHit(result);
      if (this.getWorld().isClient) {
         return;
      }

      Entity owner = this.getOwner();
      LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
      result.getEntity().damage(this.getDamageSources().mobProjectile(this, livingOwner), 1.0F);
      this.explode(this.getPos());
   }

   @Override
   protected void onBlockHit(BlockHitResult result) {
      super.onBlockHit(result);
      if (!this.getWorld().isClient) {
         Vec3d offset = Vec3d.of(result.getSide().getVector()).multiply(BLOCK_HIT_OFFSET);
         this.explode(result.getPos().add(offset));
      }
   }

   @Override
   protected void onCollision(HitResult result) {
      super.onCollision(result);
      if (!this.getWorld().isClient) {
         this.discard();
      }
   }

   private void explode(Vec3d pos) {
      this.getWorld()
         .createExplosion(
            this,
            null,
            WIND_CHARGE_DAMAGE_CALCULATOR,
            pos.x,
            pos.y,
            pos.z,
            EXPLOSION_RADIUS,
            false,
            World.ExplosionSourceType.NONE,
            ParticleTypes.GUST,
            ParticleTypes.GUST_EMITTER,
            ModSounds.WIND_CHARGE_WIND_BURST
         );
   }

   @Override
   protected boolean isBurning() {
      return false;
   }

   @Override
   protected float getDrag() {
      return 1.0F;
   }

   @Override
   protected float getDragInWater() {
      return this.getDrag();
   }

   @Override
   protected ParticleEffect getParticleType() {
      return ParticleTypes.GUST;
   }

   @Override
   protected RaycastContext.ShapeType getRaycastShapeType() {
      return RaycastContext.ShapeType.OUTLINE;
   }

   @Override
   public ItemStack getStack() {
      return ModItems.WIND_CHARGE_RENDER_STACK;
   }
}
