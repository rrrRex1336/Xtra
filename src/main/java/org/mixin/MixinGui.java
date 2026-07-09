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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.*;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {InGameHud.class}, priority = 100)
public abstract class MixinGui {

   @Shadow
   protected Text title;
   @Shadow
   protected int titleRemainTicks;
   @Shadow
   protected int titleFadeInTicks;
   @Shadow
   protected int titleStayTicks;
   @Shadow
   protected int titleFadeOutTicks;
   @Shadow
   protected Text subtitle;
   @Shadow
   @Final
   private static Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR;
   @Shadow
   public abstract TextRenderer getTextRenderer();

   private static final int MODERN_BACKGROUND_COLOR = new Color(0, 0, 0, 120).getRGB();
   private static final float MODERN_FONT_SCALE = 0.66F;

   @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   public void hookScoreboardRender(DrawContext guiGraphics, ScoreboardObjective objective, CallbackInfo ci) {
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
           method = "renderScoreboardSidebar",
           at = @At("RETURN")
   )
   private void onDisplayScoreboardSidebarReturn(DrawContext guiGraphics, ScoreboardObjective objective, CallbackInfo ci) {
      EventRenderScoreboard event = new EventRenderScoreboard(objective.getDisplayName());
      Naven.getInstance().getEventManager().call(event);
   }

   @Redirect(
           method = "renderScoreboardSidebar",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getDisplayName()Lnet/minecraft/text/Text;"
           )
   )
   public Text hookScoreboardTitle(ScoreboardObjective instance) {
      return this.getScoreboardTitle(instance);
   }

   @Redirect(
           method = "renderScoreboardSidebar",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getNumberFormatOr(Lnet/minecraft/scoreboard/number/NumberFormat;)Lnet/minecraft/scoreboard/number/NumberFormat;"
           )
   )
   public NumberFormat hookScoreboardNumberFormat(ScoreboardObjective instance, NumberFormat fallback) {
      NumberFormat numberFormat = instance.getNumberFormatOr(fallback);
      awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module = this.getScoreboardModule();
      return this.shouldHideScoreboardScore(module) ? BlankNumberFormat.INSTANCE : numberFormat;
   }

   @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
   public void hookTitle(Text pTitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.title = event.getTitle();
         this.titleRemainTicks = this.titleFadeInTicks + this.titleStayTicks + this.titleFadeOutTicks;
         ci.cancel();
      }
   }

   @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
   public void hookSubtitle(Text pSubtitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.subtitle = event.getTitle();
         ci.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
   public void hookRenderEffects(DrawContext guiGraphics, CallbackInfo ci) {
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

   private Text getScoreboardTitle(ScoreboardObjective objective) {
      EventRenderScoreboard event = new EventRenderScoreboard(objective.getDisplayName());
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
   }

   private String getScoreboardPlayerName(Scoreboard scoreboard, ScoreboardEntry entry) {
      AbstractTeam team = scoreboard.getScoreHolderTeam(entry.owner());
      return Team.decorateName(team, entry.name()).getString();
   }

   private void renderScoreboard(DrawContext guiGraphics, ScoreboardObjective objective, awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module, boolean modern) {
      Scoreboard scoreboard = objective.getScoreboard();
      NumberFormat numberFormat = module.hideScore.getCurrentValue()
              ? BlankNumberFormat.INSTANCE
              : objective.getNumberFormatOr(StyledNumberFormat.RED);
      List<VanillaScoreboardLine> lines = scoreboard.getScoreboardEntries(objective).stream()
              .filter(entry -> !entry.hidden())
              .sorted(SCOREBOARD_ENTRY_COMPARATOR)
              .limit(15)
              .map(entry -> this.createVanillaScoreboardLine(scoreboard, numberFormat, module.hideScore.getCurrentValue(), entry))
              .toList();

      if (lines.isEmpty()) {
         module.clearModernRenderer();
         return;
      }

      TextRenderer font = this.getTextRenderer();
      Text title = this.getScoreboardTitle(objective);
      int titleWidth = font.getWidth(title);
      int maxWidth = titleWidth;
      int separatorWidth = font.getWidth(":");

      for (VanillaScoreboardLine line : lines) {
         int lineWidth = font.getWidth(line.name());
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

      float baseBoxLeft = guiGraphics.getScaledWindowWidth() - maxWidth - 5.0F;
      float scoreboardHeight = 10.0F + lines.size() * 9.0F;
      module.updateDrag(baseBoxLeft, 0.0F, maxWidth + 4.0F, scoreboardHeight);
      int boxLeft = Math.round(module.getRenderX(baseBoxLeft));
      int left = boxLeft + 2;
      int right = boxLeft + maxWidth + 4;
      int titleTop = Math.round(module.getRenderY(0.0F));
      int rowTop = titleTop + 10;
      int bottom = rowTop + lines.size() * 9;
      int titleY = rowTop - 9;
      MinecraftClient minecraft = MinecraftClient.getInstance();
      int backgroundColor = minecraft.options.getTextBackgroundColor(0.3F);
      int titleBackgroundColor = minecraft.options.getTextBackgroundColor(0.4F);

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
               RenderUtils.drawRoundedRect(overlayGraphics.getMatrices(), rectX, rectY, rectWidth, rectHeight, 3.0F, MODERN_BACKGROUND_COLOR);
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
         guiGraphics.drawText(font, title, left + maxWidth / 2 - titleWidth / 2, titleY, -1, false);
      }

      for (int i = 0; i < lines.size(); i++) {
         VanillaScoreboardLine line = lines.get(i);
         int y = rowTop + i * 9;
         guiGraphics.drawText(font, line.name(), left, y, -1, false);
         if (line.scoreWidth() > 0) {
            guiGraphics.drawText(font, line.score(), right - line.scoreWidth(), y, -1, false);
         }
      }
   }

   private float renderModernComponent(DrawContext guiGraphics, Text component, float x, float y, int fallbackColor, boolean shadow) {
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

   private float getModernComponentWidth(Text component) {
      float[] width = new float[]{0.0F};
      boolean[] measured = new boolean[]{false};
      component.visit((style, text) -> {
         width[0] += this.getModernStringWidth(text);
         measured[0] = true;
         return Optional.empty();
      }, Style.EMPTY);

      return measured[0] ? width[0] : this.getModernStringWidth(component.getString());
   }

   private float renderModernString(DrawContext guiGraphics, String text, float x, float y, int color, boolean shadow) {
      if (text.isEmpty()) {
         return 0.0F;
      }

      this.prepareScoreboardRenderState();
      CustomTextRenderer renderer = Fonts.misansScoreboard;
      renderer.setAlpha(1.0F);
      renderer.render(guiGraphics.getMatrices(), text, x, y, new Color(color, true), shadow, MODERN_FONT_SCALE);
      return renderer.getWidth(text, MODERN_FONT_SCALE);
   }

   private float getModernStringWidth(String text) {
      return Fonts.misansScoreboard.getWidth(text, MODERN_FONT_SCALE);
   }

   private int getStyleColor(Style style, int fallbackColor) {
      if (style != null && style.getColor() != null) {
         return withOpaqueAlpha(style.getColor().getRgb());
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

   private VanillaScoreboardLine createVanillaScoreboardLine(Scoreboard scoreboard, NumberFormat numberFormat, boolean hideScore, ScoreboardEntry entry) {
      AbstractTeam team = scoreboard.getScoreHolderTeam(entry.owner());
      Text name = Team.decorateName(team, entry.name());
      Text score = hideScore ? Text.empty() : entry.formatted(numberFormat);
      return new VanillaScoreboardLine(name, score, this.getTextRenderer().getWidth(score));
   }

   private static record VanillaScoreboardLine(Text name, Text score, int scoreWidth) {
   }
}
