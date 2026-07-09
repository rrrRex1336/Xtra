package awa.qwq.ovo.Naven.protocol.heypixel.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

public final class UuidDerivation {
   private static final Random RNG = new Random();
   public static final DerivationVersion V1 = new DerivationVersion("V1", new String[]{"4", "1", "5", "2"},
           new DerivationRule[]{new DerivationRule(1, 0, 2), new DerivationRule(2, 2, 1), new DerivationRule(4, 4, 3)});
   public static final DerivationVersion V2 = new DerivationVersion("V2", new String[]{"c", "f", "0", "d"},
           new DerivationRule[]{new DerivationRule(1, 1, 1), new DerivationRule(2, 1, 2), new DerivationRule(4, 2, 3)});
   public static final DerivationVersion V3 = new DerivationVersion("V3", new String[]{"e", "3", "9", "8"},
           new DerivationRule[]{new DerivationRule(1, 1, 3), new DerivationRule(2, 1, 2), new DerivationRule(4, 2, 1)});
   public static final DerivationVersion V4 = new DerivationVersion("V4", new String[]{"a", "b", "7", "6"},
           new DerivationRule[]{new DerivationRule(1, 1, 2), new DerivationRule(2, 1, 2), new DerivationRule(4, 2, 2)});
   public static final DerivationVersion[] ALL_VERSIONS = {V1, V2, V3, V4};

   private UuidDerivation() {
   }

   public static DerivationVersion matchVersion(String versionStr) {
      for (DerivationVersion version : ALL_VERSIONS) {
         for (String num : version.versionNums()) {
            if (num.equalsIgnoreCase(versionStr)) {
               return version;
            }
         }
      }
      return ALL_VERSIONS[RNG.nextInt(ALL_VERSIONS.length)];
   }

   public static String deriveUuid(String accountId, String localLong, String currentUUID) {
      return deriveUuid(accountId, localLong, currentUUID, ALL_VERSIONS[RNG.nextInt(ALL_VERSIONS.length)]);
   }

   public static String deriveUuid(String accountId, String localLong, String currentUUID, DerivationVersion version) {
      String[] segments = currentUUID.split("-");
      if (segments.length != 5) {
         throw new IllegalArgumentException("Invalid UUID format: " + currentUUID);
      }

      String versionNum = version.versionNums()[RNG.nextInt(version.versionNums().length)];
      char[] seg0 = segments[0].toCharArray();
      seg0[2] = versionNum.charAt(0);
      segments[0] = new String(seg0);

      String hashHex = sha256Hex(accountId + ":" + localLong + ":" + versionNum);
      for (int i = 0; i < version.rules().length; i++) {
         DerivationRule rule = version.rules()[i];
         String hashSlice = hashHex.substring(i * rule.replaceLen(), i * rule.replaceLen() + rule.replaceLen());
         String seg = segments[rule.targetSegment()];
         segments[rule.targetSegment()] = seg.substring(0, rule.insertOffset())
                 + hashSlice
                 + seg.substring(rule.insertOffset() + rule.replaceLen());
      }

      return String.join("-", segments);
   }

   private static String sha256Hex(String input) {
      try {
         byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
         StringBuilder builder = new StringBuilder(hash.length * 2);
         for (byte b : hash) {
            builder.append(String.format("%02x", b & 0xFF));
         }
         return builder.toString();
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   public record DerivationRule(int targetSegment, int insertOffset, int replaceLen) {
   }

   public record DerivationVersion(String name, String[] versionNums, DerivationRule[] rules) {
   }
}
