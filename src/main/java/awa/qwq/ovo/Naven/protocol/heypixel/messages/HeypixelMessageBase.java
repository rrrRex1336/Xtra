package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;

public abstract class HeypixelMessageBase implements IHeypixelMessage {
   @Override
   public abstract void serialize(ByteBuffer buffer, HeypixelSession session);
}
