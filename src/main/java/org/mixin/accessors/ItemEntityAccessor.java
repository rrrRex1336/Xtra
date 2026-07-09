package org.mixin.accessors;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    @Accessor("pickupDelay")
    void setPickupDelay(int pickupDelay);

    @Accessor("pickupDelay")
    int getPickupDelay();
}