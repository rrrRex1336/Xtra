package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.viaversion.viaversion.protocols.protocol1_17to1_16_4.storage.InventoryAcknowledgements")
public class MixinViaInventoryAcknowledgements {
   @Unique
   private final List<Integer> viaVersionFix$ids = Collections.synchronizedList(new ArrayList<>());

   @Inject(method = "addId", at = @At("HEAD"), cancellable = true, remap = false)
   private void addSynchronizedId(int id, CallbackInfo ci) {
      this.viaVersionFix$ids.add(id);
      if (ViaVersionFix.isTransactionFixEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "removeId", at = @At("HEAD"), cancellable = true, remap = false)
   private void removeSynchronizedId(int id, CallbackInfoReturnable<Boolean> cir) {
      boolean removed = this.viaVersionFix$ids.remove((Integer)id);
      if (ViaVersionFix.isTransactionFixEnabled()) {
         cir.setReturnValue(removed);
      }
   }
}
