package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public class ClickSyncPacket extends HeypixelMessageBase {
   private final int leftClickCount;

   public ClickSyncPacket(int leftClickCount) {
      this.leftClickCount = leftClickCount;
   }

   @Override
   public HeypixelMessageType messageId() {
      return HeypixelMessageType.CLICK_SYNC_PACKET;
   }

   @Override
   public void serialize(ByteBuffer buffer, HeypixelSession session) {
      buffer.writeInt(this.leftClickCount);
      buffer.writeInt(0);
   }
}
