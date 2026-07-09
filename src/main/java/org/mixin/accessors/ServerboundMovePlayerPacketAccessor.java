package org.mixin.accessors;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface ServerboundMovePlayerPacketAccessor {

   @Accessor("x")
   double getX();

   @Accessor("y")
   double getY();

   @Accessor("z")
   double getZ();

   @Accessor("yaw")
   @Mutable
   void setYRot(float yRot);

   @Accessor("yaw")
   float getYRot();

   @Accessor("pitch")
   @Mutable
   void setXRot(float xRot);

   @Accessor("pitch")
   float getXRot();

   @Accessor("onGround")
   boolean isOnGround();

   @Accessor("onGround")
   @Mutable
   void setOnGround(boolean onGround);

   @Accessor("changePosition")
   boolean hasPos();

   @Accessor("changeLook")
   boolean hasRot();
}
