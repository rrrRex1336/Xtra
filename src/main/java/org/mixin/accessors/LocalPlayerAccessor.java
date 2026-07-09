package org.mixin.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPlayerEntity.class})
public interface LocalPlayerAccessor {
   @Accessor("lastSprinting")
   boolean isWasSprinting();

   @Accessor("lastYaw")
   float getYRotLast();

   @Accessor("lastPitch")
   float getXRotLast();

   @Accessor("lastPitch")
   void setXRotLast(float var1);

   @Accessor("lastYaw")
   void setYRotLast(float var1);

   @Accessor("ticksSinceLastPositionPacketSent")
   int getPositionReminder();

   @Accessor("ticksSinceLastPositionPacketSent")
   void setPositionReminder(int var1);
}
