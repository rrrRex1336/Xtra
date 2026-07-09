package org.mixin.accessors;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor("jumpingCooldown")
   void setNoJumpDelay(int var1);

   @Accessor("POTION_SWIRLS_COLOR")
   static TrackedData<Integer> getEffectColorId() {
      return null;
   }
}
