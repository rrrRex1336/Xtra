package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.visual.NameProtect;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({PacketByteBuf.class})
public class MixinFriendlyByteBuf {

   /**
    * @author
   * @reason
   */
   @Overwrite
   public Text readText() {
      String json = ((PacketByteBuf)(Object)this).readString();
      String protectedJson = NameProtect.getName(json);
      return Text.Serialization.fromJson(protectedJson);
   }
}
