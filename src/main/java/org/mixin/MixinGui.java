package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderScoreboard;
import awa.qwq.ovo.Naven.events.impl.EventSetTitle;
import awa.qwq.ovo.Naven.modules.impl.visual.NoRender;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;
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

   private static final int MODERN_HEADER_COLOR = new Color(150, 45, 45, 255).getRGB();
   private static final int MODERN_BODY_COLOR = new Color(0, 0, 0, 120).getRGB();
   private static final int MODERN_BACKGROUND_COLOR = new Color(0, 0, 0, 180).getRGB();

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   public void hookScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
      awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module = this.getScoreboardModule();
      if (module == null || !module.isEnabled() || !module.modern.getCurrentValue()) {
         if (module != null && module.isEnabled()) {
            try {
               this.renderVanillaScoreboard(guiGraphics, objective, module);
               ci.cancel();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         return;
      }

      boolean stencilActive = false;
      try {
         Scoreboard scoreboard = objective.getScoreboard();
         List<PlayerScoreEntry> list = scoreboard.listPlayerScores(objective).stream()
                 .filter(entry -> !entry.isHidden())
                 .sorted(SCORE_DISPLAY_ORDER)
                 .limit(15)
                 .toList();

         if (list.isEmpty()) {
            ci.cancel();
            return;
         }

         CustomTextRenderer font = Fonts.opensans;
         float fontSize = 0.4F;
         boolean hideScore = module.hideScore.getCurrentValue();
         String titleText = this.getScoreboardTitle(objective).getString();
         float titleWidth = font.getWidth(titleText, fontSize);
         float maxWidth = titleWidth;

         for (PlayerScoreEntry entry : list) {
            String playerName = this.getScoreboardPlayerName(scoreboard, entry);
            String text = hideScore ? playerName : playerName + ": " + entry.value();
            float width = font.getWidth(text, fontSize);
            if (width > maxWidth) maxWidth = width;
         }

         int screenWidth = guiGraphics.guiWidth();
         float x = screenWidth - maxWidth - 8.0F;
         float y = module.down.getCurrentValue();
         float bgHeight = (list.size() + 1) * (float) font.getHeight(true, fontSize) + 10.0F;

         StencilUtils.write(false);
         stencilActive = true;
         RenderUtils.drawRoundedRect(guiGraphics.pose(), x - 2.0F, y, maxWidth + 8.0F, bgHeight, 5.0F, MODERN_BACKGROUND_COLOR);
         StencilUtils.erase(true);
         RenderUtils.fill(guiGraphics.pose(), x - 2.0F, y, maxWidth + 8.0F, 3.0F, MODERN_HEADER_COLOR);
         font.render(guiGraphics.pose(), titleText,
                 x + (maxWidth - titleWidth) / 2.0F, y + 4.0F,
                 Color.WHITE, true, fontSize);

         float currentY = y + (float) font.getHeight(true, fontSize) + 6.0F;
         for (PlayerScoreEntry entry : list) {
            String playerName = this.getScoreboardPlayerName(scoreboard, entry);
            RenderUtils.fill(guiGraphics.pose(), x - 2.0F, currentY - 1.0F, maxWidth + 8.0F,
                    (float) font.getHeight(true, fontSize) + 2.0F, MODERN_BODY_COLOR);
            font.render(guiGraphics.pose(), playerName, x, currentY, Color.WHITE, true, fontSize);
            if (!hideScore) {
               String scoreText = String.valueOf(entry.value());
               float scoreX = x + maxWidth - font.getWidth(scoreText, fontSize);
               font.render(guiGraphics.pose(), scoreText, scoreX, currentY, Color.RED, true, fontSize);
            }
            currentY += (float) font.getHeight(true, fontSize) + 2.0F;
         }

         ci.cancel();

      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (stencilActive) {
            StencilUtils.dispose();
         }
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

   private void renderVanillaScoreboard(GuiGraphics guiGraphics, Objective objective, awa.qwq.ovo.Naven.modules.impl.visual.Scoreboard module) {
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

      int left = guiGraphics.guiWidth() - maxWidth - 3;
      int right = guiGraphics.guiWidth() - 1;
      int titleTop = Math.round(module.down.getCurrentValue());
      int rowTop = titleTop + 10;
      int bottom = rowTop + lines.size() * 9;
      int titleY = rowTop - 9;
      Minecraft minecraft = Minecraft.getInstance();
      int backgroundColor = minecraft.options.getBackgroundColor(0.3F);
      int titleBackgroundColor = minecraft.options.getBackgroundColor(0.4F);

      guiGraphics.fill(left - 2, titleTop, right, rowTop - 1, titleBackgroundColor);
      guiGraphics.fill(left - 2, rowTop - 1, right, bottom, backgroundColor);
      guiGraphics.drawString(font, title, left + maxWidth / 2 - titleWidth / 2, titleY, -1, false);

      for (int i = 0; i < lines.size(); i++) {
         VanillaScoreboardLine line = lines.get(i);
         int y = rowTop + i * 9;
         guiGraphics.drawString(font, line.name(), left, y, -1, false);
         if (line.scoreWidth() > 0) {
            guiGraphics.drawString(font, line.score(), right - line.scoreWidth(), y, -1, false);
         }
      }
   }

   private VanillaScoreboardLine createVanillaScoreboardLine(Scoreboard scoreboard, NumberFormat numberFormat, boolean hideScore, PlayerScoreEntry entry) {
      Team team = scoreboard.getPlayersTeam(entry.owner());
      Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
      Component score = hideScore ? Component.empty() : entry.formatValue(numberFormat);
      return new VanillaScoreboardLine(name, score, this.getFont().width(score));
   }

   private record VanillaScoreboardLine(Component name, Component score, int scoreWidth) {
   }
}
