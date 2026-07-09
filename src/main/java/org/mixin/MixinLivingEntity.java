package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventFallFlying;
import awa.qwq.ovo.Naven.events.impl.EventJump;
import awa.qwq.ovo.Naven.events.impl.EventRotationAnimation;
import awa.qwq.ovo.Naven.modules.impl.visual.AntiNausea;
import awa.qwq.ovo.Naven.modules.impl.visual.FullBright;
import awa.qwq.ovo.Naven.modules.impl.visual.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity extends Entity {
   public MixinLivingEntity(EntityType<?> pEntityType, World pLevel) {
      super(pEntityType, pLevel);
   }

   @Redirect(
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getYaw()F",
         opcode = 182,
         ordinal = 0
      ),
      method = {"jump"}
   )
   private float modifyJumpYaw(LivingEntity entity) {
      EventJump event = new EventJump(entity.getYaw());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"
      )
   )
   private float hookModifyFallFlyingPitch(LivingEntity instance) {
      EventFallFlying event = new EventFallFlying(instance.getPitch());
      Naven.getInstance().getEventManager().call(event);
      return event.getPitch();
   }

   @Inject(
      method = {"hasStatusEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hasEffect(StatusEffect pEffect, CallbackInfoReturnable<Boolean> cir) {
      LivingEntity thisEntity = (LivingEntity)(Object)this;
      if (thisEntity == MinecraftClient.getInstance().player) {
         FullBright fullBright = (FullBright)Naven.getInstance().getModuleManager().getModule(FullBright.class);
         if (pEffect == StatusEffects.NIGHT_VISION && fullBright.isEnabled()) {
            cir.setReturnValue(true);
            cir.cancel();
         }

         AntiNausea antiNausea = (AntiNausea)Naven.getInstance().getModuleManager().getModule(AntiNausea.class);
         if (pEffect == StatusEffects.NAUSEA && antiNausea.isEnabled()) {
            cir.setReturnValue(false);
            cir.cancel();
         }
      }
   }

   @Redirect(
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"
           ),
           method = {"turnHead"}
   )
   private float modifyHeadYaw(LivingEntity entity) {
      if (entity == MinecraftClient.getInstance().player) {
         Rotation rotationModule = (Rotation) Naven.getInstance().getModuleManager().getModule(Rotation.class);
         if (rotationModule != null && rotationModule.isEnabled() && rotationModule.headYaw.getCurrentValue()) {
            EventRotationAnimation event = new EventRotationAnimation(entity.getYaw(), 0.0F, 0.0F, 0.0F);
            Naven.getInstance().getEventManager().call(event);
            return event.getYaw();
         }
      }
      return entity.getYaw();
   }
}
