package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public class BlockInteractPacket extends HeypixelMessageBase {
   public float playerX;
   public float playerY;
   public float playerZ;
   public int direction;
   public int interactType;
   public float clickX;
   public float clickY;
   public float clickZ;
   public float blockX;
   public float blockY;
   public float blockZ;
   public boolean insideBlock;
   public float yaw;
   public float pitch;
   public boolean mainHand;

   @Override
   public HeypixelMessageType messageId() {
      return HeypixelMessageType.BLOCK_INTERACT_PACKET;
   }

   @Override
   public void serialize(ByteBuffer buffer, HeypixelSession session) {
      buffer.writeFloat(this.playerX);
      buffer.writeFloat(this.playerY);
      buffer.writeFloat(this.playerZ);
      buffer.writeInt(this.direction);
      buffer.writeInt(this.interactType);
      buffer.writeFloat(this.clickX);
      buffer.writeFloat(this.clickY);
      buffer.writeFloat(this.clickZ);
      buffer.writeFloat(this.blockX);
      buffer.writeFloat(this.blockY);
      buffer.writeFloat(this.blockZ);
      buffer.writeBool(this.insideBlock);
      buffer.writeFloat(this.yaw);
      buffer.writeFloat(this.pitch);
      buffer.writeBool(this.mainHand);
   }
}
