package awa.qwq.ovo.Naven.protocol.heypixel.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MappingLoader {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type MAPPING_TYPE = new TypeToken<Map<String, List<MappingData.EnumData>>>() {
   }.getType();
   private static final Map<String, List<MappingData.EnumData>> MAPPINGS = new HashMap<>();
   private static boolean initialized;
   public static Path mappingFilePath = Path.of("mappings.json");

   private MappingLoader() {
   }

   public static synchronized void initialize() {
      if (initialized) {
         return;
      }

      try {
         if (Files.exists(mappingFilePath)) {
            loadMappingsFromFile(mappingFilePath);
         } else {
            MAPPINGS.clear();
            saveMappingsToFile(mappingFilePath);
         }
      } catch (Throwable ignored) {
         MAPPINGS.clear();
      }

      initialized = true;
   }

   public static void loadMappingsFromFile(Path filePath) throws IOException {
      String json = Files.readString(filePath, StandardCharsets.UTF_8);
      Map<String, List<MappingData.EnumData>> data = GSON.fromJson(json, MAPPING_TYPE);
      MAPPINGS.clear();
      if (data != null) {
         MAPPINGS.putAll(data);
      }
   }

   public static void saveMappingsToFile(Path filePath) throws IOException {
      Path parent = filePath.getParent();
      if (parent != null) {
         Files.createDirectories(parent);
      }
      Files.writeString(filePath, GSON.toJson(MAPPINGS), StandardCharsets.UTF_8);
   }

   public static List<MappingData.EnumData> getEnumData(String className) {
      initialize();
      return MAPPINGS.get(className);
   }

   public static MappingData.FieldData getFieldData(String className, String enumName, String fieldName) {
      List<MappingData.EnumData> enumList = getEnumData(className);
      if (enumList == null) {
         return null;
      }

      for (MappingData.EnumData enumData : enumList) {
         if (enumName.equals(enumData.enumName) && enumData.fields != null) {
            MappingData.FieldData data = enumData.fields.get(fieldName);
            if (data != null) {
               return data;
            }
         }
      }
      return null;
   }

   public static Iterable<String> getAllClassNames() {
      initialize();
      return Collections.unmodifiableSet(MAPPINGS.keySet());
   }

   public static String hashNonAscii(List<Character> chars) {
      try {
         StringBuilder builder = new StringBuilder(chars.size());
         for (Character c : chars) {
            builder.append(c.charValue());
         }
         byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
         byte[] salt = "netease".getBytes(StandardCharsets.UTF_8);
         byte[] combined = new byte[bytes.length + salt.length];
         System.arraycopy(bytes, 0, combined, 0, bytes.length);
         System.arraycopy(salt, 0, combined, bytes.length, salt.length);
         byte[] hash = MessageDigest.getInstance("SHA-256").digest(combined);
         StringBuilder out = new StringBuilder(hash.length * 2);
         for (byte b : hash) {
            out.append(String.format("%02x", b & 0xFF));
         }
         return out.toString();
      } catch (Exception e) {
         return "";
      }
   }
}
