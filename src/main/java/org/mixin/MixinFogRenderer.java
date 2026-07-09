package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.AntiBlindness;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BackgroundRenderer.class})
public class MixinFogRenderer {
   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z",
         ordinal = 0
      )
   )
   private static boolean onSetupColor(LivingEntity instance, StatusEffect pEffect) {
      return pEffect == StatusEffects.BLINDNESS && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()
         ? false
         : instance.hasStatusEffect(pEffect);
   }

   @Redirect(
      method = {"applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;FZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"
      )
   )
   private static boolean onSetupFog(LivingEntity instance, StatusEffect pEffect) {
      return pEffect == StatusEffects.BLINDNESS && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()
         ? false
         : instance.hasStatusEffect(pEffect);
   }
}
