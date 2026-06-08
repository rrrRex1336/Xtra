package org.mixin.accessors;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundPlayerPositionPacket.class})
public interface ClientboundPlayerPositionPacketAccessor {
    @Accessor("xRot")
    float getXRot();

    @Accessor("yRot")
    float getYRot();
}
