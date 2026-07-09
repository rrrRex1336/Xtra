package org.mixin;

import org.mixin.accessors.ShapeIndexBufferAccessor;
import awa.qwq.ovo.Naven.utils.renderer.GL;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder.DrawParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({VertexBuffer.class})
public abstract class MixinVertexBuffer {
   @Shadow
   private int indexBufferId;

   @Inject(
      method = {"uploadIndexBuffer"},
      at = {@At("RETURN")}
   )
   private void onConfigureIndexBuffer(DrawParameters arg, ByteBuffer byteBuffer, CallbackInfoReturnable<ShapeIndexBuffer> info) {
      if (info.getReturnValue() == null) {
         GL.CURRENT_IBO = this.indexBufferId;
      } else {
         GL.CURRENT_IBO = ((ShapeIndexBufferAccessor)(Object)info.getReturnValue()).getId();
      }
   }
}
