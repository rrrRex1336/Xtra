package org.mixin;

import awa.qwq.ovo.Naven.auth.ClientTelemetry;
import awa.qwq.ovo.Naven.auth.CoordinateTelemetry;
import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.auth.VersionChecker;
import awa.qwq.ovo.Naven.security.AntiCrk;
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
        AntiCrk.verifyEarly();
        VersionChecker.verifyOrExit();
        ClientTelemetry.reportVisitor();
        Loader.isVerified();
        if (VerifyClient.verify()) {
            ClientTelemetry.report(VerifyClient.getToken());
            CoordinateTelemetry.enableAfterLogin();
        } else {
            // If a web login was just started, let the game continue so we can show the login screen
            if (!VerifyClient.hasPendingWebLogin()) {
                System.exit(1);
            }
        }
    }
}
