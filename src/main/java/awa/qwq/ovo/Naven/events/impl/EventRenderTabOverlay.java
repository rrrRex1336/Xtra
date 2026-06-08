package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class EventRenderTabOverlay implements Event {
   private EventType type;
   private Component component;
   @Nullable
   private PlayerInfo playerInfo;

   public void setType(EventType type) {
      this.type = type;
   }

   public void setComponent(Component component) {
      this.component = component;
   }

   public EventType getType() {
      return this.type;
   }

   public Component getComponent() {
      return this.component;
   }

   public @Nullable PlayerInfo getPlayerInfo() {
      return playerInfo;
   }

   public EventRenderTabOverlay(EventType type, Component component, PlayerInfo playerInfo) {
      this.type = type;
      this.component = component;
      this.playerInfo = playerInfo;
   }
}
