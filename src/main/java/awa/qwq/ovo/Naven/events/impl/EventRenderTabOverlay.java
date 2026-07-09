package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class EventRenderTabOverlay implements Event {
   private EventType type;
   private Text component;
   @Nullable
   private PlayerListEntry playerInfo;

   public void setType(EventType type) {
      this.type = type;
   }

   public void setComponent(Text component) {
      this.component = component;
   }

   public EventType getType() {
      return this.type;
   }

   public Text getComponent() {
      return this.component;
   }

   public @Nullable PlayerListEntry getPlayerInfo() {
      return playerInfo;
   }

   public EventRenderTabOverlay(EventType type, Text component, PlayerListEntry playerInfo) {
      this.type = type;
      this.component = component;
      this.playerInfo = playerInfo;
   }
}
