package org.mixin.accessors;

import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CrossbowItem.class})
public interface CrossbowItemAccessor {
   @Invoker("getSpeed")
   static float getShootingPower(ItemStack itemStack) {
      return 0.0F;
   }
}
