package org.mixin.accessors;

import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EntityS2CPacket.class})
public interface ClientboundMoveEntityPacketAccessor {
   @Accessor("id")
   int getEntityId();

   @Accessor("deltaX")
   short getXa();

   @Accessor("deltaY")
   short getYa();

   @Accessor("deltaZ")
   short getZa();

   @Accessor("yaw")
   byte getYRot();

   @Accessor("pitch")
   byte getXRot();

   @Accessor("onGround")
   boolean getOnGround();

   @Accessor("rotate")
   boolean getHasRot();

   @Accessor("positionChanged")
   boolean getHasPos();
}
