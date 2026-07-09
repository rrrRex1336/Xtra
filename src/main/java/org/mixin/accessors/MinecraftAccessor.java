package org.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftAccessor {

   @Accessor("itemUseCooldown")
   void setRightClickDelay(int var1);

   @Accessor("attackCooldown")
   void setMissTime(int var1);
}
