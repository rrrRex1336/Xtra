package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.impl.player.NoPush;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class MixinEntity{
   @Unique
   private boolean naven_Modern$movementUtilsMoveHook;

   @Shadow
   protected Vec3d movementMultiplier;

   @Shadow
   public abstract float getPitch(float var1);

   @Shadow
   public abstract float getYaw(float var1);

   @Shadow
   protected abstract Vec3d getRotationVector(float var1, float var2);

   @Shadow
   public abstract Vec3d getVelocity();

   @Shadow
   public abstract void setVelocity(Vec3d pDeltaMovement);

   @Shadow
   public abstract void setVelocity(double pX, double pY, double pZ);

   @Shadow
   public float fallDistance;

   @Inject(
           method = {"move"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
      if (this.naven_Modern$movementUtilsMoveHook) {
         return;
      }

      Entity thisEntity = (Entity)(Object)this;
      if (thisEntity == MinecraftClient.getInstance().player) {
         EventMove event = new EventMove(movement.x, movement.y, movement.z);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
            return;
         }

         if (event.getX() != movement.x || event.getY() != movement.y || event.getZ() != movement.z) {
            ci.cancel();
            this.naven_Modern$movementUtilsMoveHook = true;
            try {
               thisEntity.move(type, new Vec3d(event.getX(), event.getY(), event.getZ()));
            } finally {
               this.naven_Modern$movementUtilsMoveHook = false;
            }
         }
      }
   }


   /**
    * @author
    * @reason
    */
   @Overwrite
   public final Vec3d getRotationVec(float p_20253_) {
      float pitch = this.getPitch(p_20253_);
      float yaw = this.getYaw(p_20253_);
      Entity thisEntity = (Entity)(Object)this;
      if (thisEntity == MinecraftClient.getInstance().player) {
         EventRayTrace lookEvent = new EventRayTrace(thisEntity, yaw, pitch);
         Naven.getInstance().getEventManager().call(lookEvent);
         yaw = lookEvent.yaw;
         pitch = lookEvent.pitch;
      }

      return this.getRotationVector(pitch, yaw);
   }

   @ModifyVariable(method = "updateVelocity", at = @At("HEAD"), argsOnly = true)
   private float modifyFriction(float friction) {
      if ((Object) this == Naven.getInstance().mc.player) {
         EventStrafe2 event = new EventStrafe2(
                 Naven.getInstance().mc.player.getYaw(),
                 friction,
                 Naven.getInstance().mc.player.input.movementForward,
                 Naven.getInstance().mc.player.input.movementSideways,
                 Naven.getInstance().mc.player.isOnGround()
         );
         Naven.getInstance().getEventManager().call(event);
         return event.getFriction();
      }
      return friction;
   }

   @ModifyArg(
           method = {"updateVelocity"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;",
                   ordinal = 0
           ),
           index = 2
   )
   private float modifyYaw(float yaw) {
      EventStrafe strafe = new EventStrafe(yaw);
      Naven.getInstance().getEventManager().call(strafe);
      return strafe.getYaw();
   }

   @Inject(
           method = {"slowMovement"},
           at = {@At("RETURN")}
   )
   private void makeStuckInBlock(BlockState pState, Vec3d pMotionMultiplier, CallbackInfo ci) {
      Entity thisEntity = (Entity)(Object)this;
      if (MinecraftClient.getInstance().player == thisEntity) {
         EventStuckInBlock event = new EventStuckInBlock(pState, pMotionMultiplier);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            this.movementMultiplier = Vec3d.ZERO;
            return;
         }

         this.movementMultiplier = event.getStuckSpeedMultiplier();
      }
   }

   @Inject(
           method = {"pushAwayFrom(Lnet/minecraft/entity/Entity;)V"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void push(Entity entity, CallbackInfo ci) {
      Entity self = (Entity) (Object) this;
      if (!(self instanceof PlayerEntity player)) return;
      NoPush noPush = (NoPush) Naven.getInstance().getModuleManager().getModule(NoPush.class);
      if (noPush == null || !noPush.isEnabled()) return;
      if (shouldCancelPush(entity, noPush)) {
         ci.cancel();
      }
      if (entity instanceof BlinkingPlayer) {
         ci.cancel();
      }
   }
   private boolean shouldCancelPush(Entity pusher, NoPush module) {
      if (pusher instanceof PlayerEntity) {
         return module.players.getCurrentValue();
      } else if (pusher instanceof MobEntity) {
         return module.mobs.getCurrentValue();
      } else if (pusher instanceof ItemEntity) {
         return module.items.getCurrentValue();
      } else if (pusher instanceof AbstractMinecartEntity || pusher instanceof BoatEntity) {
         return module.vehicles.getCurrentValue();
      } else if (pusher instanceof ProjectileEntity) {
         return module.projectiles.getCurrentValue();
      }
      return false;
   }
}
