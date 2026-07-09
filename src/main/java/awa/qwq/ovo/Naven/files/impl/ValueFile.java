package awa.qwq.ovo.Naven.files.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.files.ClientFile;
import awa.qwq.ovo.Naven.values.HasValue;
import awa.qwq.ovo.Naven.values.HasValueManager;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueManager;
import awa.qwq.ovo.Naven.values.ValueType;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.StringJoiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValueFile extends ClientFile {
   private static final Logger logger = LogManager.getLogger(ValueFile.class);

   public ValueFile() {
      super("values.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      ValueManager valueManager = Naven.getInstance().getValueManager();
      HasValueManager hasValueManager = Naven.getInstance().getHasValueManager();

      String line;
      while ((line = reader.readLine()) != null) {
         try {
            String[] split = line.split(":", 4);
            if (split.length < 4) {
               logger.error("Failed to read line {}!", line);
               continue;
            }

            String valueType = split[0];
            String name = split[1];
            String valueName = split[2];
            String value = split[3];
            if ("ModuleList".equalsIgnoreCase(name) && isLegacyModuleListColorValue(valueName)) {
               name = "Theme";
               if ("Color Mode".equalsIgnoreCase(valueName)) {
                  valueName = "Color";
               }
            }
            HasValue module = hasValueManager.getHasValue(name);

            if (module == null) {
               logger.warn("Module not found: {}", name);
               continue;
            }

            Value val = valueManager.getValue(module, valueName);
            if (val == null) {
               logger.warn("Value not found: {}.{}", name, valueName);
               continue;
            }

            switch (valueType) {
               case "B":
                  val.getBooleanValue().setCurrentValue(Boolean.parseBoolean(value));
                  break;
               case "F":
                  val.getFloatValue().setCurrentValue(Float.parseFloat(value));
                  break;
               case "S":
                  val.getStringValue().setCurrentValue(value);
                  break;
               case "A":
                  AddonsValue addonsValue = val.getAddonsValue();
                  addonsValue.clearSelection();
                  if (!value.isEmpty()) {
                     String[] selectedValues = value.split(";");
                     String[] allValues = addonsValue.getValues();
                     for (String selectedValue : selectedValues) {
                        for (int i = 0; i < allValues.length; i++) {
                           if (allValues[i].equalsIgnoreCase(selectedValue.trim())) {
                              addonsValue.setSelected(i, true);
                              break;
                           }
                        }
                     }
                  }
                  break;
               case "M":
                  int index = Integer.parseInt(value);
                  ModeValue modeValue = val.getModeValue();
                  if (index >= 0 && index < modeValue.getValues().length) {
                     modeValue.setCurrentValue(index);
                  } else {
                     logger.error("Failed to read mode value {}!", line);
                  }
                  break;
               default:
                  logger.error("Unknown value type of {}!", name);
            }
         } catch (Exception var15) {
            logger.error("Failed to read value {}!", line, var15);
         }
      }
   }

   private static boolean isLegacyModuleListColorValue(String valueName) {
      return "Color Mode".equalsIgnoreCase(valueName)
              || "Color Speed".equalsIgnoreCase(valueName)
              || "Color Offset".equalsIgnoreCase(valueName);
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      ValueManager valueManager = Naven.getInstance().getValueManager();

      for (Value value : valueManager.getValues()) {
         try {
            ValueType valueType = value.getValueType();
            switch (valueType) {
               case BOOLEAN:
                  writer.write(String.format("B:%s:%s:%s\n",
                          value.getKey().getName(),
                          value.getName(),
                          value.getBooleanValue().getCurrentValue()));
                  break;
               case FLOAT:
                  writer.write(String.format("F:%s:%s:%s\n",
                          value.getKey().getName(),
                          value.getName(),
                          value.getFloatValue().getCurrentValue()));
                  break;
               case STRING:
                  writer.write(String.format("S:%s:%s:%s\n",
                          value.getKey().getName(),
                          value.getName(),
                          value.getStringValue().getCurrentValue()));
                  break;
               case ADDONS:
                  AddonsValue addonsValue = value.getAddonsValue();
                  StringJoiner joiner = new StringJoiner(";");
                  for (int index : addonsValue.getSelectedIndices()) {
                     joiner.add(addonsValue.getValues()[index]);
                  }
                  writer.write(String.format("A:%s:%s:%s\n",
                          value.getKey().getName(),
                          value.getName(),
                          joiner.toString()));
                  break;
               case MODE:
                  writer.write(String.format("M:%s:%s:%s\n",
                          value.getKey().getName(),
                          value.getName(),
                          value.getModeValue().getCurrentValue()));
                  break;
               default:
                  logger.error("Unknown value type of {}!", value.getKey().getName());
            }
         } catch (Exception var6) {
            logger.error("Failed to save value {}!", value.getKey().getName(), var6);
         }
      }
   }
}
