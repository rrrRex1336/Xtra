package org.mixin;

import awa.qwq.ovo.Naven.auth.VerifyClient;
import linyanli1337.Loader;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class ClientMainMixin {

    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void onMainStart(String[] args, CallbackInfo ci) {
        Loader.isVerified();
        if (!VerifyClient.verify()) {
            System.exit(1);
        }
    }
}