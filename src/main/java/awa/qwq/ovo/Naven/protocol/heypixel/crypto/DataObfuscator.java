package awa.qwq.ovo.Naven.protocol.heypixel.crypto;

import java.io.ByteArrayOutputStream;
import java.util.Random;

public final class DataObfuscator {
   private DataObfuscator() {
   }

   public static byte[] obfuscate(byte[] data, int algorithmId) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] safe = data == null ? new byte[0] : data;
      switch (algorithmId) {
         case 0 -> writeDirectly(out, safe);
         case 1 -> writeReversed(out, safe);
         case 2 -> writeOddEvenSeparated(out, safe);
         case 3 -> writeCircularShift(out, safe);
         case 4 -> writeXorMask(out, safe);
         case 5 -> write4ByteBlockReverse(out, safe);
         case 6 -> writeNibbleSwap(out, safe);
         case 7 -> writeBitReverse(out, safe);
         default -> writeSeededShuffle(out, safe);
      }
      return out.toByteArray();
   }

   public static byte[] deobfuscate(byte[] data, int algorithmId) {
      byte[] safe = data == null ? new byte[0] : data;
      return switch (algorithmId) {
         case 0 -> safe;
         case 1 -> deobfuscateReversed(safe);
         case 2 -> deobfuscateOddEvenSeparated(safe);
         case 3 -> deobfuscateCircularShift(safe);
         case 4 -> deobfuscateXorMask(safe);
         case 5 -> deobfuscate4ByteBlockReverse(safe);
         case 6 -> deobfuscateNibbleSwap(safe);
         case 7 -> deobfuscateBitReverse(safe);
         default -> deobfuscateSeededShuffle(safe);
      };
   }

   private static void writeDirectly(ByteArrayOutputStream out, byte[] data) {
      out.write(data, 0, data.length);
   }

   private static void writeReversed(ByteArrayOutputStream out, byte[] data) {
      for (int i = data.length - 1; i >= 0; i--) {
         out.write(data[i]);
      }
   }

   private static void writeOddEvenSeparated(ByteArrayOutputStream out, byte[] data) {
      for (int i = 0; i < data.length; i += 2) {
         out.write(data[i]);
      }
      for (int i = 1; i < data.length; i += 2) {
         out.write(data[i]);
      }
   }

   private static void writeCircularShift(ByteArrayOutputStream out, byte[] data) {
      if (data.length == 0) {
         return;
      }

      int shift = data.length / 3;
      for (int i = 0; i < data.length; i++) {
         out.write(data[(i + shift) % data.length]);
      }
   }

   private static void writeXorMask(ByteArrayOutputStream out, byte[] data) {
      for (byte b : data) {
         out.write(b ^ 0xA5);
      }
   }

   private static void write4ByteBlockReverse(ByteArrayOutputStream out, byte[] data) {
      for (int i = 0; i < data.length; i += 4) {
         int end = Math.min(i + 4, data.length);
         for (int j = end - 1; j >= i; j--) {
            out.write(data[j]);
         }
      }
   }

   private static void writeNibbleSwap(ByteArrayOutputStream out, byte[] data) {
      for (byte b : data) {
         int high = (b >>> 4) & 0x0F;
         int low = b & 0x0F;
         out.write((low << 4) | high);
      }
   }

   private static void writeBitReverse(ByteArrayOutputStream out, byte[] data) {
      for (byte b : data) {
         out.write(reverseBits(b));
      }
   }

   private static void writeSeededShuffle(ByteArrayOutputStream out, byte[] data) {
      int[] order = generateShuffleOrder(data.length);
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[order[i]] = data[i];
      }
      out.write(result, 0, result.length);
   }

   private static byte reverseBits(byte value) {
      int b = value & 0xFF;
      int result = 0;
      for (int i = 0; i < 8; i++) {
         result = (result << 1) | (b & 1);
         b >>>= 1;
      }
      return (byte)result;
   }

   private static int[] generateShuffleOrder(int length) {
      int[] order = new int[length];
      for (int i = 0; i < length; i++) {
         order[i] = i;
      }

      Random random = new Random(length * 31L);
      for (int i = length - 1; i > 0; i--) {
         int j = random.nextInt(i + 1);
         int tmp = order[i];
         order[i] = order[j];
         order[j] = tmp;
      }
      return order;
   }

   private static byte[] deobfuscateReversed(byte[] data) {
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[i] = data[data.length - 1 - i];
      }
      return result;
   }

   private static byte[] deobfuscateOddEvenSeparated(byte[] data) {
      byte[] result = new byte[data.length];
      int evenCount = (data.length + 1) / 2;
      int oddIndex = 0;
      int evenIndex = 0;
      for (int i = 0; i < data.length; i++) {
         result[i] = (i % 2 == 0) ? data[evenIndex++] : data[evenCount + oddIndex++];
      }
      return result;
   }

   private static byte[] deobfuscateCircularShift(byte[] data) {
      if (data.length == 0) {
         return data;
      }

      int shift = data.length / 3;
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[(i + shift) % data.length] = data[i];
      }
      return result;
   }

   private static byte[] deobfuscateXorMask(byte[] data) {
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[i] = (byte)(data[i] ^ 0xA5);
      }
      return result;
   }

   private static byte[] deobfuscate4ByteBlockReverse(byte[] data) {
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i += 4) {
         int end = Math.min(i + 4, data.length);
         int size = end - i;
         for (int j = 0; j < size; j++) {
            result[i + j] = data[i + size - 1 - j];
         }
      }
      return result;
   }

   private static byte[] deobfuscateNibbleSwap(byte[] data) {
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         int high = (data[i] >>> 4) & 0x0F;
         int low = data[i] & 0x0F;
         result[i] = (byte)((low << 4) | high);
      }
      return result;
   }

   private static byte[] deobfuscateBitReverse(byte[] data) {
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[i] = reverseBits(data[i]);
      }
      return result;
   }

   private static byte[] deobfuscateSeededShuffle(byte[] data) {
      int[] order = generateShuffleOrder(data.length);
      byte[] result = new byte[data.length];
      for (int i = 0; i < data.length; i++) {
         result[order[i]] = data[i];
      }
      return result;
   }
}
