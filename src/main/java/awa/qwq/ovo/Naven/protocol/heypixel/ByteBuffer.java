package awa.qwq.ovo.Naven.protocol.heypixel;

import awa.qwq.ovo.Naven.protocol.heypixel.data.HeypixelUUID;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ByteBuffer {
   private final ByteArrayOutputStream output;
   private final byte[] input;
   private int position;

   public ByteBuffer() {
      this.output = new ByteArrayOutputStream();
      this.input = null;
   }

   public ByteBuffer(byte[] data) {
      this.output = null;
      this.input = data == null ? new byte[0] : data;
   }

   public String readString() {
      int header = this.readUnsignedByte();
      int length;
      if ((header & 0xE0) == 0xA0) {
         length = header & 0x1F;
      } else if (header == 0xD9) {
         length = this.readUnsignedByte();
      } else if (header == 0xDA) {
         length = this.readBigEndianUInt16();
      } else if (header == 0xDB) {
         length = this.readBigEndianInt32();
         if (length < 0) {
            throw new IllegalArgumentException("String length cannot be negative");
         }
      } else {
         throw new IllegalArgumentException("Expected string header, got 0x" + Integer.toHexString(header));
      }

      if (length == 0) {
         return "";
      }

      return new String(this.readBytes(length), StandardCharsets.UTF_8);
   }

   public long readLong() {
      int header = this.readUnsignedByte();
      if ((header & 0x80) == 0) {
         return header;
      }
      if ((header & 0xE0) == 0xE0) {
         return (byte) header;
      }

      return switch (header) {
         case 0xCC -> this.readUnsignedByte();
         case 0xCD -> this.readBigEndianUInt16();
         case 0xCE -> Integer.toUnsignedLong(this.readBigEndianInt32());
         case 0xCF -> this.readBigEndianInt64();
         case 0xD0 -> (byte) this.readUnsignedByte();
         case 0xD1 -> (short) this.readBigEndianUInt16();
         case 0xD2 -> this.readBigEndianInt32();
         case 0xD3 -> this.readBigEndianInt64();
         default -> throw new IllegalArgumentException("Expected integer header, got 0x" + Integer.toHexString(header));
      };
   }

   public int readInt() {
      return (int)this.readLong();
   }

   public float readFloat() {
      int header = this.readUnsignedByte();
      if (header != 0xCA) {
         throw new IllegalArgumentException("Expected float32 header 0xCA, got 0x" + Integer.toHexString(header));
      }

      return Float.intBitsToFloat(this.readBigEndianInt32());
   }

   public double readDouble() {
      int header = this.readUnsignedByte();
      if (header != 0xCB) {
         throw new IllegalArgumentException("Expected float64 header 0xCB, got 0x" + Integer.toHexString(header));
      }

      return Double.longBitsToDouble(this.readBigEndianInt64());
   }

   public boolean readBool() {
      int header = this.readUnsignedByte();
      if (header == 0xC3) {
         return true;
      }
      if (header == 0xC2) {
         return false;
      }

      throw new IllegalArgumentException("Expected bool header, got 0x" + Integer.toHexString(header));
   }

   public byte readByte() {
      return (byte)this.readUnsignedByte();
   }

   public void writeString(String value) {
      if (value == null || value.isEmpty()) {
         this.writeStringRaw(value);
         return;
      }

      String hex = value.replace("-", "");
      if (hex.length() == 32 && hex.chars().allMatch(ByteBuffer::isHex)) {
         try {
            this.writeStringRaw(new HeypixelUUID(value).toEncoded());
            return;
         } catch (RuntimeException ignored) {
         }
      }

      this.writeStringRaw(value);
   }

   public void writeStringRaw(String value) {
      byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
      int length = bytes.length;
      if (length <= 255) {
         this.writeByteDirect(0xD9);
         this.writeByteDirect(length);
      } else if (length <= 65535) {
         this.writeByteDirect(0xDA);
         this.writeBigEndianUInt16(length);
      } else {
         this.writeByteDirect(0xDB);
         this.writeBigEndianInt32(length);
      }

      this.writeBytes(bytes);
   }

   public void writeLong(long value) {
      if (value >= 0 && value <= 127) {
         this.writeByteDirect((int)value);
      } else if (value >= -32 && value < 0) {
         this.writeByteDirect((int)value & 0xFF);
      } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
         this.writeByteDirect(0xD0);
         this.writeByteDirect((int)value & 0xFF);
      } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
         this.writeByteDirect(0xD1);
         this.writeBigEndianUInt16((short)value & 0xFFFF);
      } else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
         this.writeByteDirect(0xD2);
         this.writeBigEndianInt32((int)value);
      } else {
         this.writeByteDirect(0xD3);
         this.writeBigEndianInt64(value);
      }
   }

   public void writeInt(int value) {
      this.writeLong(value);
   }

   public void writeFloat(float value) {
      this.writeByteDirect(0xCA);
      this.writeBigEndianInt32(Float.floatToIntBits(value));
   }

   public void writeDouble(double value) {
      this.writeByteDirect(0xCB);
      this.writeBigEndianInt64(Double.doubleToLongBits(value));
   }

   public void writeBool(boolean value) {
      this.writeByteDirect(value ? 0xC3 : 0xC2);
   }

   public void writeSignedByte(int value) {
      this.writeByteDirect(0xD0);
      this.writeByteDirect(value);
   }

   public void writeArrayLength(int length) {
      if (length <= 15) {
         this.writeByteDirect(0x90 | length);
      } else if (length <= 65535) {
         this.writeByteDirect(0xDC);
         this.writeBigEndianUInt16(length);
      } else {
         this.writeByteDirect(0xDD);
         this.writeBigEndianInt32(length);
      }
   }

   public void writeMapLength(int length) {
      if (length <= 15) {
         this.writeByteDirect(0x80 | length);
      } else if (length <= 65535) {
         this.writeByteDirect(0xDE);
         this.writeBigEndianUInt16(length);
      } else {
         this.writeByteDirect(0xDF);
         this.writeBigEndianInt32(length);
      }
   }

   public void writeVarInt(int value) {
      int unsigned = value;
      while ((unsigned & ~0x7F) != 0) {
         this.writeByteDirect((unsigned & 0x7F) | 0x80);
         unsigned >>>= 7;
      }
      this.writeByteDirect(unsigned);
   }

   public int readVarInt() {
      int result = 0;
      int shift = 0;
      int b;
      do {
         b = this.readUnsignedByte();
         result |= (b & 0x7F) << shift;
         shift += 7;
      } while ((b & 0x80) != 0);
      return result;
   }

   public int readBigEndian32() {
      return this.readBigEndianInt32();
   }

   public byte[] readBytes(int count) {
      if (this.input == null || this.position + count > this.input.length) {
         throw new IndexOutOfBoundsException("Not enough bytes in buffer");
      }

      byte[] bytes = new byte[count];
      System.arraycopy(this.input, this.position, bytes, 0, count);
      this.position += count;
      return bytes;
   }

   public void writeBytes(byte[] data) {
      if (data != null && data.length > 0) {
         this.output.write(data, 0, data.length);
      }
   }

   public byte[] toArray() {
      return this.output == null ? this.input.clone() : this.output.toByteArray();
   }

   public int length() {
      return this.output == null ? this.input.length : this.output.size();
   }

   public int position() {
      return this.position;
   }

   public void position(int position) {
      this.position = position;
   }

   public int remaining() {
      return this.input == null ? 0 : this.input.length - this.position;
   }

   public void reset() {
      this.position = 0;
   }

   private int readUnsignedByte() {
      if (this.input == null || this.position >= this.input.length) {
         throw new IndexOutOfBoundsException("Not enough bytes in buffer");
      }

      return this.input[this.position++] & 0xFF;
   }

   private int readBigEndianUInt16() {
      return (this.readUnsignedByte() << 8) | this.readUnsignedByte();
   }

   private int readBigEndianInt32() {
      return (this.readUnsignedByte() << 24)
              | (this.readUnsignedByte() << 16)
              | (this.readUnsignedByte() << 8)
              | this.readUnsignedByte();
   }

   private long readBigEndianInt64() {
      long result = 0L;
      for (int i = 0; i < 8; i++) {
         result = (result << 8) | this.readUnsignedByte();
      }
      return result;
   }

   private void writeByteDirect(int value) {
      this.output.write(value & 0xFF);
   }

   private void writeBigEndianUInt16(int value) {
      this.writeByteDirect(value >>> 8);
      this.writeByteDirect(value);
   }

   private void writeBigEndianInt32(int value) {
      this.writeByteDirect(value >>> 24);
      this.writeByteDirect(value >>> 16);
      this.writeByteDirect(value >>> 8);
      this.writeByteDirect(value);
   }

   private void writeBigEndianInt64(long value) {
      for (int i = 7; i >= 0; i--) {
         this.writeByteDirect((int)(value >>> (i * 8)));
      }
   }

   private static boolean isHex(int c) {
      return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
   }
}
