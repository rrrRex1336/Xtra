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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.LivingEntity;
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
   private Minecraft minecraft;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;

   private boolean skijaFrameStarted = false;

   @Inject(method = {"pick"}, at = {@At("TAIL")})
   private void updateSpearPick(float partialTicks, CallbackInfo ci) {
      SpearLogic.updateClientPick(this.minecraft, partialTicks);
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )}
   )
   private void renderLevel(float pPartialTicks, long pFinishTimeNano, PoseStack pMatrixStack, CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventRender(pPartialTicks, pMatrixStack));
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void onRenderWorldTail(CallbackInfo info) {
      Naven.getInstance().getEventManager().call(new EventRenderAfterWorld());
   }

   @Inject(
      method = {"getNightVisionScale"},
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
      if (motionblur.isEnabled() && this.minecraft.player != null && motionblur.shader != null) {
         motionblur.shader.process(tickDelta);
      }
   }

   @Inject(
           method = {"render"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
                   shift = At.Shift.AFTER
           )}
   )
   public void injectRender2DEvent(float p_109094_, long p_109095_, boolean p_109096_, CallbackInfo ci) {
      GuiGraphics e = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
      EventRender2D event = new EventRender2D(e.pose(), e);
      Naven.getInstance().getEventManager().call(event);
   }


   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bobHurt(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
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
