package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderScoreboard;
import awa.qwq.ovo.Naven.events.impl.EventSetTitle;
import awa.qwq.ovo.Naven.modules.impl.visual.NoRender;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {Gui.class}, priority = 100)
public abstract class MixinGui {

   @Shadow
   protected Component title;
   @Shadow
   protected int titleTime;
   @Shadow
   protected int titleFadeInTime;
   @Shadow
   protected int titleStayTime;
   @Shadow
   protected int titleFadeOutTime;
   @Shadow
   protected Component subtitle;
   @Shadow
   @Final
   private static Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER;
   @Shadow
   public abstract Font getFont();

   private static final int MODERN_BACKGROUND_COLOR = new Color(0, 0, 0, 120).getRGB();
   private static final float MODERN_FONT_SCALE = 0.66F;

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   public void hookScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
      awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module = this.getScoreboardModule();
      if (module == null || !module.isEnabled()) {
         return;
      }

      try {
         this.renderScoreboard(guiGraphics, objective, module, module.modern.getCurrentValue());
         ci.cancel();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @Inject(
           method = "displayScoreboardSidebar",
           at = @At("RETURN")
   )
   private void onDisplayScoreboardSidebarReturn(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
      EventRenderScoreboard event = new EventRenderScoreboard(objective.getDisplayName());
      Naven.getInstance().getEventManager().call(event);
   }

   @Redirect(
           method = "displayScoreboardSidebar",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;"
           )
   )
   public Component hookScoreboardTitle(Objective instance) {
      return this.getScoreboardTitle(instance);
   }

   @Redirect(
           method = "displayScoreboardSidebar",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"
           )
   )
   public NumberFormat hookScoreboardNumberFormat(Objective instance, NumberFormat fallback) {
      NumberFormat numberFormat = instance.numberFormatOrDefault(fallback);
      awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module = this.getScoreboardModule();
      return this.shouldHideScoreboardScore(module) ? BlankFormat.INSTANCE : numberFormat;
   }

   @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
   public void hookTitle(Component pTitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.title = event.getTitle();
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
         ci.cancel();
      }
   }

   @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
   public void hookSubtitle(Component pSubtitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.subtitle = event.getTitle();
         ci.cancel();
      }
   }

   @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
   public void hookRenderEffects(GuiGraphics guiGraphics, CallbackInfo ci) {
      NoRender noRender = (NoRender) Naven.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender.isEnabled() && noRender.disableEffects.getCurrentValue()) {
         ci.cancel();
      }
   }

   private awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard getScoreboardModule() {
      try {
         return (awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard) Naven.getInstance().getModuleManager().getModule(awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard.class);
      } catch (Exception ignored) {
         return null;
      }
   }

   private boolean shouldHideScoreboardScore(awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module) {
      return module != null && module.isEnabled() && module.hideScore.getCurrentValue();
   }

   private Component getScoreboardTitle(Objective objective) {
      EventRenderScoreboard event = new EventRenderScoreboard(objective.getDisplayName());
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
   }

   private String getScoreboardPlayerName(Scoreboard scoreboard, PlayerScoreEntry entry) {
      Team team = scoreboard.getPlayersTeam(entry.owner());
      return PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString();
   }

   private void renderScoreboard(GuiGraphics guiGraphics, Objective objective, awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module, boolean modern) {
      Scoreboard scoreboard = objective.getScoreboard();
      NumberFormat numberFormat = module.hideScore.getCurrentValue()
              ? BlankFormat.INSTANCE
              : objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
      List<VanillaScoreboardLine> lines = scoreboard.listPlayerScores(objective).stream()
              .filter(entry -> !entry.isHidden())
              .sorted(SCORE_DISPLAY_ORDER)
              .limit(15)
              .map(entry -> this.createVanillaScoreboardLine(scoreboard, numberFormat, module.hideScore.getCurrentValue(), entry))
              .toList();

      if (lines.isEmpty()) {
         module.clearModernRenderer();
         return;
      }

      Font font = this.getFont();
      Component title = this.getScoreboardTitle(objective);
      int titleWidth = font.width(title);
      int maxWidth = titleWidth;
      int separatorWidth = font.width(":");

      for (VanillaScoreboardLine line : lines) {
         int lineWidth = font.width(line.name());
         if (line.scoreWidth() > 0) {
            lineWidth += separatorWidth + line.scoreWidth();
         }

         maxWidth = Math.max(maxWidth, lineWidth);
      }

      if (modern) {
         maxWidth = Math.round(this.getModernComponentWidth(title));
         float modernSeparatorWidth = this.getModernStringWidth(":");
         for (VanillaScoreboardLine line : lines) {
            float lineWidth = this.getModernComponentWidth(line.name());
            if (line.scoreWidth() > 0) {
               lineWidth += modernSeparatorWidth + this.getModernComponentWidth(line.score());
            }

            maxWidth = Math.max(maxWidth, (int)Math.ceil(lineWidth));
         }
      }

      float baseBoxLeft = guiGraphics.guiWidth() - maxWidth - 5.0F;
      float scoreboardHeight = 10.0F + lines.size() * 9.0F;
      module.updateDrag(baseBoxLeft, 0.0F, maxWidth + 4.0F, scoreboardHeight);
      int boxLeft = Math.round(module.getRenderX(baseBoxLeft));
      int left = boxLeft + 2;
      int right = boxLeft + maxWidth + 4;
      int titleTop = Math.round(module.getRenderY(0.0F));
      int rowTop = titleTop + 10;
      int bottom = rowTop + lines.size() * 9;
      int titleY = rowTop - 9;
      Minecraft minecraft = Minecraft.getInstance();
      int backgroundColor = minecraft.options.getBackgroundColor(0.3F);
      int titleBackgroundColor = minecraft.options.getBackgroundColor(0.4F);

      if (modern) {
         float rectX = left - 2.0F;
         float rectY = titleTop;
         float rectWidth = right - rectX;
         float rectHeight = bottom - titleTop;
         final int renderMaxWidth = maxWidth;
         module.setShaderRect(rectX, rectY, rectWidth, rectHeight);
         module.setModernRenderer(overlayGraphics -> {
            boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            double misansAlpha = Fonts.misansScoreboard.mesh.alpha;
            this.prepareScoreboardRenderState();
            try {
               RenderUtils.drawRoundedRect(overlayGraphics.pose(), rectX, rectY, rectWidth, rectHeight, 3.0F, MODERN_BACKGROUND_COLOR);
               this.prepareScoreboardRenderState();
               Fonts.misansScoreboard.setAlpha(1.0F);
               this.renderModernComponent(overlayGraphics, title, left + (renderMaxWidth - this.getModernComponentWidth(title)) / 2.0F, titleY, -1, false);

               for (int i = 0; i < lines.size(); i++) {
                  VanillaScoreboardLine line = lines.get(i);
                  int y = rowTop + i * 9;
                  this.renderModernComponent(overlayGraphics, line.name(), left, y, -1, false);
                  if (line.scoreWidth() > 0) {
                     this.renderModernComponent(overlayGraphics, line.score(), right - this.getModernComponentWidth(line.score()), y, 0xFFFF5555, false);
                  }
               }
            } finally {
               Fonts.misansScoreboard.setAlpha((float)misansAlpha);
               this.restoreScoreboardRenderState(depthWasEnabled, depthMaskWasEnabled, blendWasEnabled);
            }
         });

         return;
      } else {
         module.clearModernRenderer();
         guiGraphics.fill(left - 2, titleTop, right, rowTop - 1, titleBackgroundColor);
         guiGraphics.fill(left - 2, rowTop - 1, right, bottom, backgroundColor);
         guiGraphics.drawString(font, title, left + maxWidth / 2 - titleWidth / 2, titleY, -1, false);
      }

      for (int i = 0; i < lines.size(); i++) {
         VanillaScoreboardLine line = lines.get(i);
         int y = rowTop + i * 9;
         guiGraphics.drawString(font, line.name(), left, y, -1, false);
         if (line.scoreWidth() > 0) {
            guiGraphics.drawString(font, line.score(), right - line.scoreWidth(), y, -1, false);
         }
      }
   }

   private float renderModernComponent(GuiGraphics guiGraphics, Component component, float x, float y, int fallbackColor, boolean shadow) {
      float[] currentX = new float[]{x};
      boolean[] rendered = new boolean[]{false};
      component.visit((style, text) -> {
         currentX[0] += this.renderModernString(guiGraphics, text, currentX[0], y, this.getStyleColor(style, fallbackColor), shadow);
         rendered[0] = true;
         return Optional.empty();
      }, Style.EMPTY);

      if (!rendered[0]) {
         currentX[0] += this.renderModernString(guiGraphics, component.getString(), currentX[0], y, withOpaqueAlpha(fallbackColor), shadow);
      }

      return currentX[0] - x;
   }

   private float getModernComponentWidth(Component component) {
      float[] width = new float[]{0.0F};
      boolean[] measured = new boolean[]{false};
      component.visit((style, text) -> {
         width[0] += this.getModernStringWidth(text);
         measured[0] = true;
         return Optional.empty();
      }, Style.EMPTY);

      return measured[0] ? width[0] : this.getModernStringWidth(component.getString());
   }

   private float renderModernString(GuiGraphics guiGraphics, String text, float x, float y, int color, boolean shadow) {
      if (text.isEmpty()) {
         return 0.0F;
      }

      this.prepareScoreboardRenderState();
      CustomTextRenderer renderer = Fonts.misansScoreboard;
      renderer.setAlpha(1.0F);
      renderer.render(guiGraphics.pose(), text, x, y, new Color(color, true), shadow, MODERN_FONT_SCALE);
      return renderer.getWidth(text, MODERN_FONT_SCALE);
   }

   private float getModernStringWidth(String text) {
      return Fonts.misansScoreboard.getWidth(text, MODERN_FONT_SCALE);
   }

   private int getStyleColor(Style style, int fallbackColor) {
      if (style != null && style.getColor() != null) {
         return withOpaqueAlpha(style.getColor().getValue());
      }

      return withOpaqueAlpha(fallbackColor);
   }

   private static int withOpaqueAlpha(int color) {
      return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
   }

   private void prepareScoreboardRenderState() {
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(false);
   }

   private void restoreScoreboardRenderState(boolean depthWasEnabled, boolean depthMaskWasEnabled, boolean blendWasEnabled) {
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

   private VanillaScoreboardLine createVanillaScoreboardLine(Scoreboard scoreboard, NumberFormat numberFormat, boolean hideScore, PlayerScoreEntry entry) {
      Team team = scoreboard.getPlayersTeam(entry.owner());
      Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
      Component score = hideScore ? Component.empty() : entry.formatValue(numberFormat);
      return new VanillaScoreboardLine(name, score, this.getFont().width(score));
   }

   private static record VanillaScoreboardLine(Component name, Component score, int scoreWidth) {
   }
}
