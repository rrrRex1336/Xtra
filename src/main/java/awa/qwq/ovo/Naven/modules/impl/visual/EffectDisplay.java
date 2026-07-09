package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.StringHelper;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "EffectDisplay",
   description = "Displays potion effects on the HUD",
   category = Category.VISUAL
)
public class EffectDisplay extends Module {
   private static final String PREVIEW_EFFECT_NAME = "EffectDisplay\u7684\u6548\u679c";

   private List<Runnable> list;
   private final Map<StatusEffect, EffectDisplay.MobEffectInfo> infos = new ConcurrentHashMap<>();
   private final Color headerColor = new Color(150, 45, 45, 255);
   private final Color bodyColor = new Color(0, 0, 0, 50);
   private final List<Vector4f> blurMatrices = new ArrayList<>();
   private final FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
   private final FloatValue yOffset = DragManager.createHiddenPositionValue(this, "Drag Y", 0.0F);
   private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);
   private final EffectDisplay.MobEffectInfo previewInfo = new EffectDisplay.MobEffectInfo();

   @EventTarget(4)
   public void renderIcons(EventRender2D e) {
      boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
      this.prepareHudRenderState();
      try {
         if (this.list != null) {
            this.list.forEach(Runnable::run);
         }
      } finally {
         this.restoreHudRenderState(depthWasEnabled, depthMaskWasEnabled, blendWasEnabled);
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (e.getType() != EventType.BLUR) {
         return;
      }

      for (Vector4f matrix : this.blurMatrices) {
         RenderUtils.drawRoundedRect(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), 5.0F, 1073741824);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      double harmonyAlpha = Fonts.harmony.mesh.alpha;
      boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
      this.prepareHudRenderState();

      try {
         for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            EffectDisplay.MobEffectInfo info;
            if (this.infos.containsKey(effect.getEffectType())) {
               info = this.infos.get(effect.getEffectType());
            } else {
               info = new EffectDisplay.MobEffectInfo();
               this.infos.put(effect.getEffectType(), info);
            }

            info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
            info.duration = effect.getDuration();
            info.amplifier = effect.getAmplifier();
            info.shouldDisappear = false;
         }

         boolean hasRealEffects = !mc.player.getStatusEffects().isEmpty();
         boolean preview = !hasRealEffects && DragManager.isHudEditorActive();
         if (preview) {
            this.infos.clear();
         }

         List<Entry<StatusEffect, EffectDisplay.MobEffectInfo>> displayEntries = new ArrayList<>(this.infos.entrySet());
         if (preview) {
            this.preparePreviewInfo();
            displayEntries.add(new AbstractMap.SimpleEntry<>(StatusEffects.SPEED, this.previewInfo));
         }

         int startY = mc.getWindow().getScaledHeight() / 2 - displayEntries.size() * 16;
         this.list = Lists.newArrayListWithExpectedSize(displayEntries.size());
         this.blurMatrices.clear();
         Fonts.harmony.setAlpha(1.0F);

         CustomTextRenderer harmony = Fonts.harmony;
         float maxWidth = 0.0F;
         for (Entry<StatusEffect, EffectDisplay.MobEffectInfo> entry : displayEntries) {
            EffectDisplay.MobEffectInfo effectInfo = entry.getValue();
            String text = effectInfo == this.previewInfo ? PREVIEW_EFFECT_NAME : this.getDisplayName(entry.getKey(), effectInfo);
            effectInfo.width = 25.0F + harmony.getWidth(text, 0.3) + 20.0F;
            maxWidth = Math.max(maxWidth, effectInfo.width);
         }

         float stackHeight = displayEntries.isEmpty() ? 0.0F : displayEntries.size() * 34.0F - 4.0F;
         this.dragManager.update(10.0F, (float) startY, maxWidth, stackHeight);
         float stackX = this.dragManager.getX(10.0F);
         float rowY = this.dragManager.getY((float) startY);

         for (Entry<StatusEffect, EffectDisplay.MobEffectInfo> entry : displayEntries) {
            e.getStack().push();
            try {
               EffectDisplay.MobEffectInfo effectInfo = entry.getValue();
               boolean previewEntry = effectInfo == this.previewInfo;
               String text = previewEntry ? PREVIEW_EFFECT_NAME : this.getDisplayName(entry.getKey(), effectInfo);
               if (effectInfo.yTimer.value == -1.0F) {
                  effectInfo.yTimer.value = rowY;
               }

               harmony.setAlpha(1.0F);
               float x = effectInfo.xTimer.value;
               float y = effectInfo.yTimer.value;
               effectInfo.shouldDisappear = !previewEntry && !mc.player.hasStatusEffect(entry.getKey());
               if (effectInfo.shouldDisappear) {
                  effectInfo.xTimer.target = -effectInfo.width - 20.0F;
                  if (x <= -effectInfo.width - 20.0F) {
                     this.infos.remove(entry.getKey());
                  }
               } else {
                  effectInfo.durationTimer.target = (float)effectInfo.duration / (float)effectInfo.maxDuration * effectInfo.width;
                  if (effectInfo.durationTimer.value <= 0.0F) {
                     effectInfo.durationTimer.value = effectInfo.durationTimer.target;
                  }

                  effectInfo.xTimer.target = stackX;
                  effectInfo.yTimer.target = rowY;
                  effectInfo.yTimer.update(true);
               }

               effectInfo.durationTimer.update(true);
               effectInfo.xTimer.update(true);
               StencilUtils.write(false);
               this.blurMatrices.add(new Vector4f(x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F));
               RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
               StencilUtils.erase(true);
               RenderUtils.fillBound(e.getStack(), x, y, effectInfo.width, 30.0F, this.bodyColor.getRGB());
               RenderUtils.fillBound(e.getStack(), x, y, effectInfo.durationTimer.value, 30.0F, this.bodyColor.getRGB());
               RenderUtils.drawRoundedRect(e.getStack(), x + effectInfo.width - 10.0F, y + 7.0F, 5.0F, 18.0F, 2.0F, this.headerColor.getRGB());
               harmony.setAlpha(1.0F);
               harmony.render(e.getStack(), text, (double)(x + 27.0F), (double)(y + 7.0F), this.headerColor, true, 0.3);
               float tickRate = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getTickManager().getTickRate() : 20.0F;
               String duration = previewEntry ? "00:30" : StringHelper.formatTicks(effectInfo.duration, tickRate);
               harmony.render(e.getStack(), duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);
               StatusEffectSpriteManager mobeffecttexturemanager = mc.getStatusEffectSpriteManager();
               Sprite textureatlassprite = mobeffecttexturemanager.getSprite(entry.getKey());
               this.list.add(() -> {
                  RenderSystem.setShaderTexture(0, textureatlassprite.getAtlasId());
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  e.getGuiGraphics().drawSprite((int)(x + 6.0F), (int)(y + 8.0F), 1, 18, 18, textureatlassprite);
               });
               rowY += 34.0F;
            } finally {
               StencilUtils.dispose();
               e.getStack().pop();
            }
         }
      } finally {
         Fonts.harmony.setAlpha((float)harmonyAlpha);
         this.restoreHudRenderState(depthWasEnabled, depthMaskWasEnabled, blendWasEnabled);
      }
   }

   public String getDisplayName(StatusEffect effect, EffectDisplay.MobEffectInfo info) {
      String effectName = effect.getName().getString();
      String amplifierName;
      if (info.amplifier == 0) {
         amplifierName = "";
      } else if (info.amplifier == 1) {
         amplifierName = " " + I18n.translate("enchantment.level.2", new Object[0]);
      } else if (info.amplifier == 2) {
         amplifierName = " " + I18n.translate("enchantment.level.3", new Object[0]);
      } else if (info.amplifier == 3) {
         amplifierName = " " + I18n.translate("enchantment.level.4", new Object[0]);
      } else {
         amplifierName = " " + info.amplifier;
      }

      return effectName + amplifierName;
   }

   private void preparePreviewInfo() {
      this.previewInfo.maxDuration = 600;
      this.previewInfo.duration = 600;
      this.previewInfo.amplifier = 0;
      this.previewInfo.shouldDisappear = false;
      if (this.previewInfo.durationTimer.value <= 0.0F) {
         this.previewInfo.durationTimer.value = 0.0F;
      }
   }

   private void prepareHudRenderState() {
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(false);
   }

   private void restoreHudRenderState(boolean depthWasEnabled, boolean depthMaskWasEnabled, boolean blendWasEnabled) {
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDepthMask(depthMaskWasEnabled);
      if (depthWasEnabled) {
         GL11.glEnable(GL11.GL_DEPTH_TEST);
      } else {
         GL11.glDisable(GL11.GL_DEPTH_TEST);
      }

      if (blendWasEnabled) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
      } else {
         RenderSystem.disableBlend();
      }
   }

   public static class MobEffectInfo {
      public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
      public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public int maxDuration = -1;
      public int duration = 0;
      public int amplifier = 0;
      public boolean shouldDisappear = false;
      public float width;
   }
}
