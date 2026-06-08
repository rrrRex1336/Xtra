package org.mixin.accessors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {

   @Accessor("x")
   double getX();

   @Accessor("y")
   double getY();

   @Accessor("z")
   double getZ();

   @Accessor("yRot")
   @Mutable
   void setYRot(float yRot);

   @Accessor("yRot")
   float getYRot();

   @Accessor("xRot")
   @Mutable
   void setXRot(float xRot);

   @Accessor("xRot")
   float getXRot();

   @Accessor("onGround")
   boolean isOnGround();

   @Accessor("onGround")
   @Mutable
   void setOnGround(boolean onGround);

   @Accessor("hasPos")
   boolean hasPos();

   @Accessor("hasRot")
   boolean hasRot();
}