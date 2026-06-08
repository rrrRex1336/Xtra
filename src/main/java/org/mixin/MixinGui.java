package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderScoreboard;
import awa.qwq.ovo.Naven.events.impl.EventSetTitle;
import awa.qwq.ovo.Naven.modules.impl.Visual.NoRender;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {Gui.class}, priority = 100)
public class MixinGui {

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

   private static final int MODERN_HEADER_COLOR = new Color(150, 45, 45, 255).getRGB();
   private static final int MODERN_BODY_COLOR = new Color(0, 0, 0, 120).getRGB();
   private static final int MODERN_BACKGROUND_COLOR = new Color(0, 0, 0, 180).getRGB();

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   public void hookScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
      awa.qwq.ovo.Naven.modules.impl.Visual.Scoreboard module = (awa.qwq.ovo.Naven.modules.impl.Visual.Scoreboard) Naven.getInstance().getModuleManager().getModule(awa.qwq.ovo.Naven.modules.impl.Visual.Scoreboard.class);
      if (!module.isEnabled() || !module.modern.getCurrentValue()) {
         return;
      }

      try {
         Scoreboard scoreboard = objective.getScoreboard();
         List<PlayerScoreEntry> list = (List<PlayerScoreEntry>) scoreboard.listPlayerScores(objective);
         list = new ArrayList<>(list.stream()
                 .filter(entry -> !entry.owner().startsWith("#"))
                 .limit(15)
                 .toList());

         if (list.isEmpty()) {
            ci.cancel();
            return;
         }

         CustomTextRenderer font = Fonts.opensans;
         float fontSize = 0.4F;
         String titleText = objective.getDisplayName().getString();
         float titleWidth = font.getWidth(titleText, fontSize);
         float maxWidth = titleWidth;

         for (PlayerScoreEntry entry : list) {
            Team team = scoreboard.getPlayersTeam(entry.owner());
            String playerName = PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).getString();
            String text = playerName + ": " + entry.value();
            float width = font.getWidth(text, fontSize);
            if (width > maxWidth) maxWidth = width;
         }

         int screenWidth = guiGraphics.guiWidth();
         float x = screenWidth - maxWidth - 8.0F;
         float y = module.down.getCurrentValue();
         float bgHeight = (list.size() + 1) * (float) font.getHeight(true, fontSize) + 10.0F;

         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(guiGraphics.pose(), x - 2.0F, y, maxWidth + 8.0F, bgHeight, 5.0F, MODERN_BACKGROUND_COLOR);
         StencilUtils.erase(true);
         RenderUtils.fill(guiGraphics.pose(), x - 2.0F, y, maxWidth + 8.0F, 3.0F, MODERN_HEADER_COLOR);
         font.render(guiGraphics.pose(), titleText,
                 x + (maxWidth - titleWidth) / 2.0F, y + 4.0F,
                 Color.WHITE, true, fontSize);

         float currentY = y + (float) font.getHeight(true, fontSize) + 6.0F;
         for (PlayerScoreEntry entry : list) {
            Team team = scoreboard.getPlayersTeam(entry.owner());
            String playerName = PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).getString();
            RenderUtils.fill(guiGraphics.pose(), x - 2.0F, currentY - 1.0F, maxWidth + 8.0F,
                    (float) font.getHeight(true, fontSize) + 2.0F, MODERN_BODY_COLOR);
            font.render(guiGraphics.pose(), playerName, x, currentY, Color.WHITE, true, fontSize);
            if (!module.hideScore.getCurrentValue()) {
               String scoreText = String.valueOf(entry.value());
               float scoreX = x + maxWidth - font.getWidth(scoreText, fontSize);
               font.render(guiGraphics.pose(), scoreText, scoreX, currentY, Color.RED, true, fontSize);
            }
            currentY += (float) font.getHeight(true, fontSize) + 2.0F;
         }

         StencilUtils.dispose();
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
      Component component = instance.getDisplayName();
      EventRenderScoreboard event = new EventRenderScoreboard(component);
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
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
}