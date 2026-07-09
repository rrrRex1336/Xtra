package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelRequestType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public class ServerEventMessage extends HeypixelMessageBase {
   private String requestId = "";
   private long timestamp;
   private HeypixelRequestType type = HeypixelRequestType.INFO;
   private String extraContent = "";

   @Override
   public HeypixelMessageType messageId() {
      return HeypixelMessageType.SERVER_EVENT_MESSAGE;
   }

   @Override
   public boolean isClientToServer() {
      return false;
   }

   @Override
   public void serialize(ByteBuffer buffer, HeypixelSession session) {
      throw new UnsupportedOperationException("ServerEventMessage is S2C only");
   }

   @Override
   public void deserialize(ByteBuffer buffer, HeypixelSession session) {
      this.requestId = buffer.readString();
      this.timestamp = buffer.readLong();
      this.type = HeypixelRequestType.fromId(buffer.readByte() & 0xFF);
      if (this.type == HeypixelRequestType.REFLECT_CHECK && buffer.remaining() > 0) {
         this.extraContent = buffer.readString();
      }
   }

   @Override
   public void handle(HeypixelSession session) {
      SessionMessage response = new SessionMessage();
      response.type = this.type;
      response.requestId = this.requestId;
      response.requestTimestamp = this.timestamp;
      response.extraContent = this.extraContent;
      session.sendMessage(response);
   }
}
