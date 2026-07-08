package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.modules.impl.visual.IrcChatHUD;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MixinMouseHandler {
   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V"
      )},
      method = {"onPress"}
   )
   private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
      EventMouseClick event = new EventMouseClick(button, action == 0);
      Naven.getInstance().getEventManager().call(event);
   }

   @Inject(method = {"onScroll"}, at = {@At("HEAD")})
   private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      IrcChatHUD.scrollBy(vertical);
   }
}
