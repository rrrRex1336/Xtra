package awa.qwq.ovo.Naven.protocol.heypixel;

public enum HeypixelRequestType {
   INFO(0),
   BLACK_CLASS(1),
   BLACK_MODULE(2),
   REFLECT_CHECK(3);

   private final int id;

   HeypixelRequestType(int id) {
      this.id = id;
   }

   public int id() {
      return this.id;
   }

   public static HeypixelRequestType fromId(int id) {
      for (HeypixelRequestType type : values()) {
         if (type.id == id) {
            return type;
         }
      }

      return INFO;
   }
}
