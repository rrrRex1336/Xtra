package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class HeypixelMessageRegistry {
   private static final Map<Class<? extends IHeypixelMessage>, Integer> C2S_MESSAGE_IDS = new HashMap<>();
   private static final Map<Integer, Supplier<IHeypixelMessage>> S2C_MESSAGE_TYPES = new HashMap<>();
   private static boolean initialized;

   private HeypixelMessageRegistry() {
   }

   public static void initialize() {
      if (initialized) {
         return;
      }

      registerC2S(SessionMessage.class, HeypixelMessageType.SESSION_MESSAGE.id());
      registerC2S(HeartbeatPacket.class, HeypixelMessageType.HEARTBEAT_PACKET.id());
      registerC2S(ClickSyncPacket.class, HeypixelMessageType.CLICK_SYNC_PACKET.id());
      registerC2S(BlockInteractPacket.class, HeypixelMessageType.BLOCK_INTERACT_PACKET.id());
      registerC2S(PlayerEntityPacket.class, HeypixelMessageType.PLAYER_ENTITY_PACKET.id());
      registerS2C(HeypixelMessageType.SERVER_EVENT_MESSAGE.id(), ServerEventMessage::new);
      initialized = true;
   }

   private static void registerC2S(Class<? extends IHeypixelMessage> type, int messageId) {
      C2S_MESSAGE_IDS.put(type, messageId);
   }

   private static void registerS2C(int messageId, Supplier<IHeypixelMessage> supplier) {
      S2C_MESSAGE_TYPES.put(messageId, supplier);
   }

   public static int getMessageId(Class<? extends IHeypixelMessage> type) {
      initialize();
      Integer id = C2S_MESSAGE_IDS.get(type);
      if (id == null) {
         throw new IllegalArgumentException("Unregistered message type: " + type.getName());
      }
      return id;
   }

   public static IHeypixelMessage createS2CMessage(int messageId) {
      initialize();
      Supplier<IHeypixelMessage> supplier = S2C_MESSAGE_TYPES.get(messageId);
      return supplier == null ? null : supplier.get();
   }
}
