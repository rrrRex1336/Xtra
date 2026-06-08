package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import java.util.List;

import awa.qwq.ovo.Naven.modules.impl.Visual.Island;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerTabOverlay.class})
public abstract class MixinPlayerTabOverlay {
   @Shadow
   public abstract Component getNameForDisplay(PlayerInfo var1);

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
                   target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
                   ordinal = 0
           ),
           remap = true
   )
   private List<FormattedCharSequence> hookHeader(Font instance, FormattedText pText, int pMaxWidth) {
      try {
         Component component = (Component)pText;
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.HEADER, component, null);
         Naven.getInstance().getEventManager().call(event);
         return instance.split(event.getComponent(), pMaxWidth);
      } catch (Exception e) {
         return instance.split(pText, pMaxWidth);
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
                   ordinal = 1
           ),
           remap = true
   )
   private List<FormattedCharSequence> hookFooter(Font instance, FormattedText pText, int pMaxWidth) {
      try {
         Component component = (Component)pText;
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.FOOTER, component, null);

         Naven.getInstance().getEventManager().call(event);
         return instance.split(event.getComponent(), pMaxWidth);
      } catch (Exception e) {
         return instance.split(pText, pMaxWidth);
      }
   }

   @Redirect(
           method = "render",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;"
           ),
           remap = true
   )
   private Component hookName(PlayerTabOverlay instance, PlayerInfo pPlayerInfo) {
      try {
         Component nameForDisplay = this.getNameForDisplay(pPlayerInfo);
         EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.NAME, nameForDisplay, pPlayerInfo);
         Naven.getInstance().getEventManager().call(event);
         return event.getComponent();
      } catch (Exception e) {
         return this.getNameForDisplay(pPlayerInfo);
      }
   }
}
