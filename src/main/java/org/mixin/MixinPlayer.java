package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventAttackSlowdown;
import awa.qwq.ovo.Naven.events.impl.EventAttackYaw;
import awa.qwq.ovo.Naven.events.impl.EventStayingOnGroundSurface;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerEntity.class})
public abstract class MixinPlayer extends LivingEntity {
   private static final double ATTACK_SLOWDOWN_XZ = 0.6D;

   protected MixinPlayer(EntityType<? extends LivingEntity> pEntityType, World pLevel) {
      super(pEntityType, pLevel);
   }

   @Inject(method = {"attack"}, at = {@At("HEAD")}, cancellable = true)
   private void attackWithSpear(Entity target, CallbackInfo ci) {
      if (SpearLogic.piercingAttack((PlayerEntity)(Object)this, target)) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
      )
   )
   private float hookFixRotation(PlayerEntity instance) {
      EventAttackYaw event = new EventAttackYaw(instance.getYaw());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
           method = {"attack"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
           )
   )
   private void hookSetDeltaMovement(PlayerEntity instance, Vec3d vec3) {
      EventAttackSlowdown event = new EventAttackSlowdown(EventAttackSlowdown.Type.Delta_Movement, ATTACK_SLOWDOWN_XZ);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         double motionXZ = event.getMotionXZ();
         if (motionXZ != ATTACK_SLOWDOWN_XZ) {
            double scale = motionXZ / ATTACK_SLOWDOWN_XZ;
            instance.setVelocity(new Vec3d(vec3.x * scale, vec3.y, vec3.z * scale));
         } else {
            instance.setVelocity(vec3);
         }
      }
   }

   @Redirect(
           method = {"attack"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"
           )
   )
   private void hookSetSprinting(PlayerEntity instance, boolean sprinting) {
      EventAttackSlowdown event = new EventAttackSlowdown(EventAttackSlowdown.Type.Sprinting);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.setSprinting(sprinting);
      }
   }

   @Inject(
      method = {"clipAtLedge"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void isStayingOnGroundSurface(CallbackInfoReturnable<Boolean> info) {
      EventStayingOnGroundSurface event = new EventStayingOnGroundSurface((Boolean)info.getReturnValue());
      Naven.getInstance().getEventManager().call(event);
      info.setReturnValue(event.isStay());
   }
}
