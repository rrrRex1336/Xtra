package awa.qwq.ovo.Naven.protocol.heypixel;

public enum HeypixelMessageType {
   SESSION_MESSAGE(1),
   HEARTBEAT_PACKET(2),
   CLICK_SYNC_PACKET(3),
   BLOCK_INTERACT_PACKET(5),
   PLAYER_ENTITY_PACKET(6),
   SERVER_EVENT_MESSAGE(101);

   private final int id;

   HeypixelMessageType(int id) {
      this.id = id;
   }

   public int id() {
      return this.id;
   }
}
