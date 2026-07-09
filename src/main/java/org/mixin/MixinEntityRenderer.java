package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.NameTags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public class MixinEntityRenderer<T extends Entity> {
   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderNameTag(T pEntity, Text pDisplayName, MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pPackedLight, CallbackInfo ci) {
      if (pEntity instanceof PlayerEntity && Naven.getInstance().getModuleManager().getModule(NameTags.class).isEnabled()) {
         ci.cancel();
      }
   }
}
