package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearMaterial;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
   @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
   private void useMaceModel(ItemStack stack, World level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(stack)) {
         cir.setReturnValue(((ItemRenderer)(Object)this).getModel(ModItems.MACE_RENDER_STACK, level, entity, seed));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerWindCharge(stack)) {
         cir.setReturnValue(((ItemRenderer)(Object)this).getModel(ModItems.WIND_CHARGE_RENDER_STACK, level, entity, seed));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearMaterial material = InventoryUtils.getServerSpearMaterial(stack);
         if (material != null) {
            cir.setReturnValue(((ItemRenderer)(Object)this).getModel(ModItems.getSpearRenderStack(material), level, entity, seed));
         }
      }
   }
}
