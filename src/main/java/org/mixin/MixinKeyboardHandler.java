package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class MixinKeyboardHandler {
   @Inject(
      at = {@At("HEAD")},
      method = {"onKey"}
   )
   private void onKeyPress(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo ci) {
      if (pKey != -1 && Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventKey(pKey, pAction != 0));
      }
   }
}
