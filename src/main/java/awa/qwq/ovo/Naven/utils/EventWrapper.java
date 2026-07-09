package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClientChat;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

public class EventWrapper {

   public EventWrapper() {
      registerEvents();
   }

   private void registerEvents() {
      ClientSendMessageEvents.ALLOW_CHAT.register(this::onClientChat);
      ClientSendMessageEvents.MODIFY_CHAT.register(this::modifyClientChat);
   }

   private boolean onClientChat(String message) {
      EventClientChat event = new EventClientChat(message);
      Naven.getInstance().getEventManager().call(event);
      return !event.isCancelled();
   }

   private String modifyClientChat(String message) {
      EventClientChat event = new EventClientChat(message);
      Naven.getInstance().getEventManager().call(event);
      return event.getMessage();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.age <= 1) {
         Naven.getInstance().getEventManager().call(new EventRespawn());
      }
   }
}