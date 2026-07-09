package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public class PlayerEntityPacket extends HeypixelMessageBase {
   @Override
   public HeypixelMessageType messageId() {
      return HeypixelMessageType.PLAYER_ENTITY_PACKET;
   }

   @Override
   public void serialize(ByteBuffer buffer, HeypixelSession session) {
   }
}
