package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventRenderAfterWorld;
import awa.qwq.ovo.Naven.modules.impl.visual.FullBright;
import awa.qwq.ovo.Naven.modules.impl.visual.MotionBlur;
import awa.qwq.ovo.Naven.modules.impl.visual.NoHurtCam;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private BufferBuilderStorage buffers;

   private boolean skijaFrameStarted = false;

   @Inject(method = {"updateTargetedEntity"}, at = {@At("TAIL")})
   private void updateSpearPick(float partialTicks, CallbackInfo ci) {
      SpearLogic.updateClientPick(this.client, partialTicks);
      if (this.client.player == null) {
         return;
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )}
   )
   private void renderLevel(float pPartialTicks, long pFinishTimeNano, MatrixStack pMatrixStack, CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventRender(pPartialTicks, pMatrixStack));
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At("TAIL")}
   )
   private void onRenderWorldTail(float pPartialTicks, long pFinishTimeNano, MatrixStack pMatrixStack, CallbackInfo info) {
      Naven.getInstance().getEventManager().call(new EventRenderAfterWorld());
   }

   @Inject(
      method = {"getNightVisionStrength"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime, CallbackInfoReturnable<Float> cir) {
      FullBright module = (FullBright)Naven.getInstance().getModuleManager().getModule(FullBright.class);
      if (module.isEnabled()) {
         cir.setReturnValue(module.brightness.getCurrentValue());
         cir.cancel();
      }
   }

   @Inject(method = {"render"}, at = {@At("TAIL")})
   public void render(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
      // 添加空检查
      if (MotionBlur.instance == null) {
         return;
      }

      MotionBlur motionblur = MotionBlur.instance;
      if (motionblur.isEnabled() && this.client.player != null && motionblur.shader != null) {
         motionblur.shader.render(tickDelta);
      }
   }

   @Inject(
           method = {"render"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;F)V",
                   shift = At.Shift.AFTER
           )}
   )
   public void injectRender2DEvent(float p_109094_, long p_109095_, boolean p_109096_, CallbackInfo ci) {
      DrawContext e = new DrawContext(this.client, this.buffers.getEntityVertexConsumers());
      EventRender2D event = new EventRender2D(e.getMatrices(), e);
      Naven.getInstance().getEventManager().call(event);
   }


   @Inject(
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bobHurt(MatrixStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
      Naven naven = Naven.getInstance();
      ModuleManager moduleManager = naven == null ? null : naven.getModuleManager();
      if (moduleManager == null) {
         return;
      }

      NoHurtCam module = (NoHurtCam)moduleManager.getModule(NoHurtCam.class);
      if (module.isEnabled()) {
         ci.cancel();
      }
   }
}
