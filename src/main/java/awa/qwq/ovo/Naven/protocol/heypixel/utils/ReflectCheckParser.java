package awa.qwq.ovo.Naven.protocol.heypixel.utils;

import awa.qwq.ovo.Naven.protocol.heypixel.crypto.CryptoHelper;
import awa.qwq.ovo.Naven.protocol.heypixel.data.MappingData;
import awa.qwq.ovo.Naven.protocol.heypixel.data.MappingLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ReflectCheckParser {
   private static final String[] DANGER_KEYWORDS = {
           "password", "token", "session", "credential", "secret", "auth", "key", "private", "uuid", "hwid"
   };

   private ReflectCheckParser() {
   }

   public static MappingData.ReflectCheckInfo parse(String jsonContent) {
      MappingData.ReflectCheckInfo info = new MappingData.ReflectCheckInfo();
      try {
         JsonElement root = JsonParser.parseString(jsonContent == null ? "[]" : jsonContent);
         if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            info.finalAction = "EMPTY";
            info.accessPath = "null";
            return info;
         }

         JsonArray steps = root.getAsJsonArray();
         StringBuilder pathBuilder = new StringBuilder();
         for (int i = 0; i < steps.size(); i++) {
            MappingData.ReflectStep step = parseStep(steps.get(i).getAsJsonObject());
            info.chainSteps.add("Step " + (i + 1) + ": " + step.action + " - "
                    + (step.className == null ? "from previous" : step.className) + "."
                    + (step.fieldName == null ? step.enumName : step.fieldName));

            if (i == steps.size() - 1) {
               info.finalAction = step.action;
            }

            if (step.instanceFromPrevious) {
               info.chainDepth++;
            } else {
               pathBuilder.setLength(0);
               pathBuilder.append(step.className);
            }

            if (step.enumName != null && !step.enumName.isEmpty()) {
               pathBuilder.append('.').append(step.enumName);
            }
            if (step.fieldName != null && !step.fieldName.isEmpty()) {
               pathBuilder.append('.').append(step.fieldName);
            }
         }

         info.accessPath = pathBuilder.toString();
         checkSuspicious(info);
      } catch (Throwable ignored) {
         info.finalAction = "PARSE_ERROR";
         info.accessPath = "error";
      }
      return info;
   }

   public static ReflectResponse processReflectCheck(String jsonContent, CryptoHelper crypto) {
      try {
         MappingData.ReflectCheckInfo info = parse(jsonContent);
         JsonElement root = JsonParser.parseString(jsonContent == null ? "[]" : jsonContent);
         if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            return new ReflectResponse(0, crypto.encryptToBase64(""));
         }

         String className = null;
         String enumName = null;
         String fieldName = null;
         for (JsonElement element : root.getAsJsonArray()) {
            MappingData.ReflectStep step = parseStep(element.getAsJsonObject());
            if (step.className != null && !step.className.isEmpty()) {
               className = step.className;
            }
            if (step.enumName != null && !step.enumName.isEmpty()) {
               enumName = step.enumName;
            }
            if (step.fieldName != null && !step.fieldName.isEmpty()) {
               fieldName = step.fieldName;
            }
         }

         if (className != null && enumName != null && fieldName != null) {
            MappingData.FieldData fieldData = MappingLoader.getFieldData(className, enumName, fieldName);
            if (fieldData != null) {
            return new ReflectResponse(fieldData.hashCode, crypto.encryptToBase64(fieldData.content));
            }
         }

         return new ReflectResponse(fieldName == null ? 0 : javaHashCode(fieldName), crypto.encryptToBase64(info.accessPath));
      } catch (Throwable ignored) {
         return new ReflectResponse(0, crypto.encryptToBase64("error"));
      }
   }

   private static MappingData.ReflectStep parseStep(JsonObject object) {
      MappingData.ReflectStep step = new MappingData.ReflectStep();
      step.action = getString(object, "action");
      step.className = getNullableString(object, "className");
      step.fieldName = getNullableString(object, "fieldName");
      step.enumName = getNullableString(object, "enumName");
      step.instanceFromPrevious = object.has("instanceFromPrevious") && object.get("instanceFromPrevious").getAsBoolean();
      return step;
   }

   private static void checkSuspicious(MappingData.ReflectCheckInfo info) {
      String lowerPath = info.accessPath.toLowerCase();
      for (String keyword : DANGER_KEYWORDS) {
         if (lowerPath.contains(keyword)) {
            info.suspicious = true;
            info.suspiciousReason = "Access path contains sensitive keyword: " + keyword;
            return;
         }
      }
   }

   private static int javaHashCode(String value) {
      int hash = 0;
      for (int i = 0; i < value.length(); i++) {
         hash = 31 * hash + value.charAt(i);
      }
      return hash;
   }

   private static String getString(JsonObject object, String key) {
      String value = getNullableString(object, key);
      return value == null ? "" : value;
   }

   private static String getNullableString(JsonObject object, String key) {
      if (!object.has(key) || object.get(key).isJsonNull()) {
         return null;
      }
      return object.get(key).getAsString();
   }

   public record ReflectResponse(int hash, String encryptedContent) {
   }
}
