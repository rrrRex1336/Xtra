package org.mixin.accessors;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Framebuffer.class})
public interface RenderTargetAccessor {
   @Accessor("depthAttachment")
   void setDepthBufferId(int var1);
}
