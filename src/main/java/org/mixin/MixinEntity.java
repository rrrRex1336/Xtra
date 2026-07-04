package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.chat.IrcClient;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.impl.player.NoPush;
import net.minecraft.world.entity.projectile.Projectile;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
   protected Vec3 stuckSpeedMultiplier;

   @Shadow
   public abstract float getViewXRot(float var1);

   @Shadow
   public abstract float getViewYRot(float var1);

   @Shadow
   protected abstract Vec3 calculateViewVector(float var1, float var2);

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract void setDeltaMovement(Vec3 pDeltaMovement);

   @Shadow
   public abstract void setDeltaMovement(double pX, double pY, double pZ);

   @Shadow
   public float fallDistance;

   @Inject(
           method = {"move"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onMove(MoverType type, Vec3 movement, CallbackInfo ci) {
      if (this.naven_Modern$movementUtilsMoveHook) {
         return;
      }

      Entity thisEntity = (Entity)(Object)this;
      if (thisEntity == Minecraft.getInstance().player) {
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
               thisEntity.move(type, new Vec3(event.getX(), event.getY(), event.getZ()));
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
   public final Vec3 getViewVector(float p_20253_) {
      float pitch = this.getViewXRot(p_20253_);
      float yaw = this.getViewYRot(p_20253_);
      Entity thisEntity = (Entity)(Object)this;
      if (thisEntity == Minecraft.getInstance().player) {
         EventRayTrace lookEvent = new EventRayTrace(thisEntity, yaw, pitch);
         Naven.getInstance().getEventManager().call(lookEvent);
         yaw = lookEvent.yaw;
         pitch = lookEvent.pitch;
      }

      return this.calculateViewVector(pitch, yaw);
   }

   @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true)
   private float modifyFriction(float friction) {
      if ((Object) this == Naven.getInstance().mc.player) {
         EventStrafe2 event = new EventStrafe2(
                 Naven.getInstance().mc.player.getYRot(),
                 friction,
                 Naven.getInstance().mc.player.input.forwardImpulse,
                 Naven.getInstance().mc.player.input.leftImpulse,
                 Naven.getInstance().mc.player.onGround()
         );
         Naven.getInstance().getEventManager().call(event);
         return event.getFriction();
      }
      return friction;
   }

   @ModifyArg(
           method = {"moveRelative"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;",
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
           method = {"makeStuckInBlock"},
           at = {@At("RETURN")}
   )
   private void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier, CallbackInfo ci) {
      Entity thisEntity = (Entity)(Object)this;
      if (Minecraft.getInstance().player == thisEntity) {
         EventStuckInBlock event = new EventStuckInBlock(pState, pMotionMultiplier);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            this.stuckSpeedMultiplier = Vec3.ZERO;
            return;
         }

         this.stuckSpeedMultiplier = event.getStuckSpeedMultiplier();
      }
   }

   @Inject(
           method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void push(Entity entity, CallbackInfo ci) {
      Entity self = (Entity) (Object) this;
      if (this.isLocalIrcCollision(self, entity)) {
         ci.cancel();
         return;
      }

      if (!(self instanceof Player player)) return;
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
      if (pusher instanceof Player) {
         return module.players.getCurrentValue();
      } else if (pusher instanceof Mob) {
         return module.mobs.getCurrentValue();
      } else if (pusher instanceof ItemEntity) {
         return module.items.getCurrentValue();
      } else if (pusher instanceof AbstractMinecart || pusher instanceof Boat) {
         return module.vehicles.getCurrentValue();
      } else if (pusher instanceof Projectile) {
         return module.projectiles.getCurrentValue();
      }
      return false;
   }

   private boolean isLocalIrcCollision(Entity self, Entity other) {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || self == null || other == null) {
         return false;
      }

      return self == mc.player && IrcClient.isIrcPlayer(other)
         || other == mc.player && IrcClient.isIrcPlayer(self);
   }
}
