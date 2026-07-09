package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.ui.Island.TabOverlayState;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import awa.qwq.ovo.Naven.modules.impl.visual.Island;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerListHud.class})
public abstract class MixinPlayerTabOverlay {
   @Shadow
   public abstract Text getPlayerName(PlayerListEntry var1);

   @Inject(
           method = "render",
           at = @At("HEAD"),
           cancellable = true,
           remap = true
   )
   private void hookRenderHead(CallbackInfo ci) {
      try {
         Island islandModule =
                 (Island) Naven.getInstance()
                         .getModuleManager().getModule(Island.class);
         if (islandModule != null && islandModule.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception e) {
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;",
                   ordinal = 0
           ),
           remap = true
   )
   private List<OrderedText> hookHeader(TextRenderer instance, StringVisitable pText, int pMaxWidth) {
      try {
         Text component = (Text)pText;
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.HEADER, component, null);
         Naven.getInstance().getEventManager().call(event);
         TabOverlayState.setHeader(event.getComponent());
         return instance.wrapLines(event.getComponent(), pMaxWidth);
      } catch (Exception e) {
         TabOverlayState.setHeader((Text)pText);
         return instance.wrapLines(pText, pMaxWidth);
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;",
                   ordinal = 1
           ),
           remap = true
   )
   private List<OrderedText> hookFooter(TextRenderer instance, StringVisitable pText, int pMaxWidth) {
      try {
         Text component = (Text)pText;
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.FOOTER, component, null);

         Naven.getInstance().getEventManager().call(event);
         TabOverlayState.setFooter(event.getComponent());
         return instance.wrapLines(event.getComponent(), pMaxWidth);
      } catch (Exception e) {
         TabOverlayState.setFooter((Text)pText);
         return instance.wrapLines(pText, pMaxWidth);
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"
           ),
           remap = true
   )
   private Text hookName(PlayerListHud instance, PlayerListEntry pPlayerInfo) {
      try {
         Text nameForDisplay = this.getPlayerName(pPlayerInfo);
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.NAME, nameForDisplay, pPlayerInfo);
         Naven.getInstance().getEventManager().call(event);
         return event.getComponent();
      } catch (Exception e) {
         return this.getPlayerName(pPlayerInfo);
      }
   }
}
