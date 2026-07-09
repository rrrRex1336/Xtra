package awa.qwq.ovo.Naven.protocol.heypixel.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoHelper {
   private static final int ITERATIONS = 1000;
   private static final int SALT_SIZE = 8;
   private static final SecureRandom RNG = new SecureRandom();
   private final String key;

   public CryptoHelper(String key) {
      this.key = key;
   }

   public String encryptToBase64(String plainText) {
      return Base64.getEncoder().encodeToString(this.encrypt((plainText == null ? "" : plainText).getBytes(StandardCharsets.UTF_8)));
   }

   public String decryptFromBase64(String base64Text) {
      return new String(this.decrypt(Base64.getDecoder().decode(base64Text)), StandardCharsets.UTF_8);
   }

   public byte[] encrypt(byte[] data) {
      if (this.key == null || this.key.isEmpty()) {
         throw new IllegalStateException("Encryption key not set");
      }

      try {
         byte[] salt = new byte[SALT_SIZE];
         RNG.nextBytes(salt);
         KeyAndIv keyAndIv = deriveKeyAndIv(this.key, salt);

         Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
         cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyAndIv.key, "DES"), new IvParameterSpec(keyAndIv.iv));
         byte[] encrypted = cipher.doFinal(data == null ? new byte[0] : data);
         byte[] result = new byte[SALT_SIZE + encrypted.length];
         System.arraycopy(salt, 0, result, 0, SALT_SIZE);
         System.arraycopy(encrypted, 0, result, SALT_SIZE, encrypted.length);
         return result;
      } catch (Exception e) {
         throw new IllegalStateException("Heypixel encrypt failed", e);
      }
   }

   public byte[] decrypt(byte[] data) {
      if (this.key == null || this.key.isEmpty()) {
         throw new IllegalStateException("Decryption key not set");
      }
      if (data == null || data.length <= SALT_SIZE) {
         throw new IllegalArgumentException("Data too short to decrypt");
      }

      try {
         byte[] salt = Arrays.copyOfRange(data, 0, SALT_SIZE);
         byte[] encrypted = Arrays.copyOfRange(data, SALT_SIZE, data.length);
         KeyAndIv keyAndIv = deriveKeyAndIv(this.key, salt);

         Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
         cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIv.key, "DES"), new IvParameterSpec(keyAndIv.iv));
         return cipher.doFinal(encrypted);
      } catch (Exception e) {
         throw new IllegalStateException("Heypixel decrypt failed", e);
      }
   }

   private static KeyAndIv deriveKeyAndIv(String password, byte[] salt) throws Exception {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
      byte[] combined = new byte[passwordBytes.length + salt.length];
      System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
      System.arraycopy(salt, 0, combined, passwordBytes.length, salt.length);

      byte[] hash = md5.digest(combined);
      for (int i = 1; i < ITERATIONS; i++) {
         hash = md5.digest(hash);
      }

      return new KeyAndIv(Arrays.copyOfRange(hash, 0, 8), Arrays.copyOfRange(hash, 8, 16));
   }

   private record KeyAndIv(byte[] key, byte[] iv) {
   }
}
