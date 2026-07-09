package org.mixin.accessors;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GenericContainerScreenHandler.class)
public interface ChestMenuAccessor {
    @Accessor("inventory")
    Inventory getContainer();
}
