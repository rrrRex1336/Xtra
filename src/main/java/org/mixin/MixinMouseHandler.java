package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MixinMouseHandler {
   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(Lnet/minecraft/client/util/InputUtil$Key;Z)V"
      )},
      method = {"onMouseButton"}
   )
   private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
      EventMouseClick event = new EventMouseClick(button, action == 0);
      Naven.getInstance().getEventManager().call(event);
   }
}
