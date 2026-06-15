package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.Version;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventDisconnect;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventShutdown;
import awa.qwq.ovo.Naven.modules.impl.visual.Glow;
import awa.qwq.ovo.Naven.utils.animation.AnimationUtils;
import awa.qwq.ovo.Naven.utils.ISkipTicks;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft implements ISkipTicks {

   @Unique
   public int skipTicks;

   @Unique
   private long naven_Modern$lastFrame;

   @Shadow
   @Final
   private User user;

   @Shadow @Final private Window window;

   @Override
   public void setSkipTicks(int ticks) {
      this.skipTicks = ticks;
   }

   @Override
   public int getSkipTicks() {
      return this.skipTicks;
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo info) {
      Naven.modRegister();
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   public void onInit(GameConfig gameConfig, CallbackInfo ci) {
      System.setProperty("java.awt.headless", "false");
   }

   /**
    * @author
    * @reason
    */
   @Overwrite
   public void updateTitle() {
      String gameVersion = SharedConstants.getCurrentVersion().getName();
      this.window.setTitle("Naven Modern " + gameVersion + " " + Version.getVersion());
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void onTick(CallbackInfo ci) {
      if (skipTicks > 0) {
         skipTicks--;
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void shutdown(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
   }

   @Inject(method = "setLevel", at = @At("HEAD"))
   private void onSetLevel(CallbackInfo ci) {
      if (Naven.getInstance().isReady()) {
         Naven.getInstance().getEventManager().call(new EventDisconnect());
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void tickPre(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }
   }

   @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
   private void shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(entity)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "runTick", at = @At("HEAD"))
   private void runTick(CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int) (currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
   }

   @ModifyArg(
           method = "runTick",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"
           ),
           index = 0
   )
   private float fixSkipTicks(float partialTick) {
      if (this.skipTicks > 0) {
         return 0.0F;
      }
      return partialTick;
   }

   @Inject(
           method = "handleKeybinds",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
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