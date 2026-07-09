package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.Version;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventDisconnect;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventShutdown;
import awa.qwq.ovo.Naven.modules.impl.visual.Glow;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.animation.AnimationUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraft {

   @Unique
   private long naven_Modern$lastFrame;

   @Shadow
   @Final
   private Session session;

   @Shadow @Final private Window window;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo info) {
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   public void onInit(RunArgs gameConfig, CallbackInfo ci) {
      System.setProperty("java.awt.headless", "false");
   }

   /**
    * @author
    * @reason
    */
   @Overwrite
   public void updateWindowTitle() {
      String gameVersion = SharedConstants.getGameVersion().getName();
      this.window.setTitle("Naven Modern " + gameVersion + " " + Version.getVersion());
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void shutdown(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
   }

   @Inject(method = "setWorld", at = @At("HEAD"))
   private void onSetLevel(ClientWorld world, CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().isReady()) {
         Naven.getInstance().getEventManager().call(new EventDisconnect());
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void tickPre(CallbackInfo ci) {
      Naven.mc = (MinecraftClient)(Object)this;
      Module.refreshMinecraft();
      if (Naven.getInstance() == null) {
         Naven.modRegister();
      }

      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().enablePendingModulesIfReady();
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }
   }

   @Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true)
   private void shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(entity)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void runTick(boolean tick, CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int) (currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
   }

   @Inject(
           method = "handleInputEvents",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
                   ordinal = 0,
                   shift = At.Shift.BEFORE
           ),
           cancellable = true
   )
   private void clickEvent(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         EventClick event = new EventClick();
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
