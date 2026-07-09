package awa.qwq.ovo.Naven.protocol.heypixel.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MappingData {
   private MappingData() {
   }

   public static class EnumData {
      public String enumName = "";
      public Map<String, FieldData> fields = new HashMap<>();
   }

   public static class FieldData {
      public String content = "";
      public int hashCode;
   }

   public static class ReflectStep {
      public String action = "";
      public String className;
      public String fieldName;
      public String enumName;
      public boolean instanceFromPrevious;
   }

   public static class ReflectCheckInfo {
      public String finalAction = "";
      public String accessPath = "";
      public int chainDepth;
      public List<String> chainSteps = new ArrayList<>();
      public boolean suspicious;
      public String suspiciousReason = "";
   }
}
