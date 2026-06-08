package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.Visual.NameProtect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({FriendlyByteBuf.class})
public class MixinFriendlyByteBuf {

   /**
    * @author
    * @reason
    */
   @Overwrite
   public Component readComponent() {
      String json = ((FriendlyByteBuf)(Object)this).readUtf();
      String protectedJson = NameProtect.getName(json);
      return Component.Serializer.fromJson(protectedJson);
   }
}