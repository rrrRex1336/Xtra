package org.mixin.accessors;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

   @Accessor("rightClickDelay")
   void setRightClickDelay(int var1);

   @Accessor("missTime")
   void setMissTime(int var1);
}