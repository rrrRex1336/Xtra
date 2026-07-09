package awa.qwq.ovo.Naven.protocol.heypixel.data;

import java.security.SecureRandom;
import java.util.UUID;

public class HeypixelUUID {
   private static final SecureRandom RNG = new SecureRandom();
   private final long mostSignificantBits;
   private final long leastSignificantBits;

   public HeypixelUUID(long mostSignificantBits, long leastSignificantBits) {
      this.mostSignificantBits = mostSignificantBits;
      this.leastSignificantBits = leastSignificantBits;
   }

   public HeypixelUUID(byte[] bytes) {
      if (bytes == null || bytes.length != 16) {
         throw new IllegalArgumentException("UUID must be 16 bytes");
      }

      long msb = 0L;
      for (int i = 0; i < 8; i++) {
         msb = (msb << 8) | (bytes[i] & 0xFFL);
      }

      long lsb = 0L;
      for (int i = 8; i < 16; i++) {
         lsb = (lsb << 8) | (bytes[i] & 0xFFL);
      }

      this.mostSignificantBits = msb;
      this.leastSignificantBits = lsb;
   }

   public HeypixelUUID(String uuidString) {
      String hex = uuidString == null ? "" : uuidString.replace("-", "");
      if (hex.length() != 32) {
         throw new IllegalArgumentException("Invalid UUID format");
      }

      this.mostSignificantBits = Long.parseUnsignedLong(hex.substring(0, 16), 16);
      this.leastSignificantBits = Long.parseUnsignedLong(hex.substring(16), 16);
   }

   public static HeypixelUUID random() {
      byte[] bytes = new byte[16];
      RNG.nextBytes(bytes);
      bytes[6] = (byte)((bytes[6] & 0x0F) | 0x40);
      bytes[8] = (byte)((bytes[8] & 0x3F) | 0x80);
      return new HeypixelUUID(bytes);
   }

   public static HeypixelUUID fromUuid(UUID uuid) {
      return new HeypixelUUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
   }

   public int getObfuscationId() {
      long xor = this.mostSignificantBits ^ this.leastSignificantBits;
      int result = (int)(xor % 8L);
      return result < 0 ? result + 8 : result;
   }

   public String toEncoded() {
      return this.leastSignificantBits + "|-|" + this.mostSignificantBits;
   }

   public byte[] toBytes() {
      byte[] bytes = new byte[16];
      long msb = this.mostSignificantBits;
      long lsb = this.leastSignificantBits;
      for (int i = 7; i >= 0; i--) {
         bytes[i] = (byte)(msb & 0xFF);
         msb >>>= 8;
      }
      for (int i = 15; i >= 8; i--) {
         bytes[i] = (byte)(lsb & 0xFF);
         lsb >>>= 8;
      }
      return bytes;
   }

   @Override
   public String toString() {
      String hex = String.format("%016x%016x", this.mostSignificantBits, this.leastSignificantBits);
      return hex.substring(0, 8) + "-"
              + hex.substring(8, 12) + "-"
              + hex.substring(12, 16) + "-"
              + hex.substring(16, 20) + "-"
              + hex.substring(20);
   }
}
