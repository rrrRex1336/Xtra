package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public interface IHeypixelMessage {
   HeypixelMessageType messageId();

   default boolean isClientToServer() {
      return true;
   }

   default boolean requiresEncryption() {
      return false;
   }

   default boolean requiresObfuscation() {
      return false;
   }

   void serialize(ByteBuffer buffer, HeypixelSession session);

   default void deserialize(ByteBuffer buffer, HeypixelSession session) {
      throw new UnsupportedOperationException();
   }

   default void handle(HeypixelSession session) {
   }
}
