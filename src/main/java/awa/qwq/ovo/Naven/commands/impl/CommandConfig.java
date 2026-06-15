package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.files.FileManager;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueManager;
import awa.qwq.ovo.Naven.values.ValueType;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@CommandInfo(
   name = "config",
   description = "Manage client configs.",
   aliases = {"cfg"}
)
public class CommandConfig extends Command {
   private static final Logger logger = LogManager.getLogger(CommandConfig.class);
   private static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().create();
   private static final String CONFIG_EXTENSION = ".cfg";

   @Override
   public void onCommand(String[] args) {
      if (args.length == 0) {
         this.sendUsage();
         return;
      }

      switch (args[0].toLowerCase(Locale.ROOT)) {
         case "folder":
            this.openConfigFolder();
            break;
         case "save":
            if (args.length != 2) {
               ChatUtils.addChatMessage("Usage: .config save <name>");
               return;
            }

            this.savePreset(args[1]);
            break;
         case "load":
            if (args.length != 2) {
               ChatUtils.addChatMessage("Usage: .config load <name>");
               return;
            }

            this.loadPreset(args[1]);
            break;
         case "list":
            this.listPresets();
            break;
         default:
            this.sendUsage();
            break;
      }
   }

   @Override
   public String[] onTab(String[] args) {
      if (args.length <= 1) {
         String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
         return Arrays.stream(new String[]{"folder", "save", "load", "list"})
            .filter(command -> command.startsWith(prefix))
            .toArray(String[]::new);
      }

      if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
         String prefix = args[1].toLowerCase(Locale.ROOT);
         File[] files = this.getPresetsFolder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(CONFIG_EXTENSION));
         if (files == null) {
            return new String[0];
         }

         return Arrays.stream(files)
            .map(file -> file.getName().substring(0, file.getName().length() - CONFIG_EXTENSION.length()))
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toArray(String[]::new);
      }

      return new String[0];
   }

   private void openConfigFolder() {
      try {
         FileManager.clientFolder.mkdirs();
         new ProcessBuilder("explorer", FileManager.clientFolder.getAbsolutePath()).start();
      } catch (IOException ignored) {
         ChatUtils.addChatMessage("Failed to open config folder.");
      }
   }

   private void savePreset(String name) {
      File presetFile = this.getPresetFile(name);
      if (presetFile == null) {
         ChatUtils.addChatMessage("Invalid config name.");
         return;
      }

      File tempFile = new File(presetFile.getParentFile(), presetFile.getName() + ".tmp");
      try {
         this.getPresetsFolder().mkdirs();
         JsonObject root = new JsonObject();
         ModuleManager moduleManager = Naven.getInstance().getModuleManager();
         ValueManager valueManager = Naven.getInstance().getValueManager();

         for (Module module : moduleManager.getModules()) {
            JsonObject moduleObject = new JsonObject();
            JsonObject valuesObject = new JsonObject();
            moduleObject.addProperty("enabled", module.isEnabled());
            moduleObject.addProperty("keyCode", module.getKey());
            moduleObject.addProperty("hidden", module.isHidden());

            for (Value value : valueManager.getValuesByHasValue(module)) {
               try {
                  this.writeValue(valuesObject, value);
               } catch (Exception exception) {
                  logger.error("Failed to save preset value {}.{}!", module.getName(), value.getName(), exception);
               }
            }

            moduleObject.add("values", valuesObject);
            root.add(module.getName(), moduleObject);
         }

         try (Writer writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         }

         Files.move(tempFile.toPath(), presetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
         Naven.getInstance().getFileManager().save();
         ChatUtils.addChatMessage("Saved config " + this.stripExtension(presetFile.getName()) + ".");
      } catch (Exception exception) {
         logger.error("Failed to save config {}!", name, exception);
         if (tempFile.exists() && !tempFile.delete()) {
            logger.warn("Failed to delete temp config file {}.", tempFile.getAbsolutePath());
         }

         ChatUtils.addChatMessage("Failed to save config " + name + ".");
      }
   }

   private void loadPreset(String name) {
      File presetFile = this.getPresetFile(name);
      if (presetFile == null) {
         ChatUtils.addChatMessage("Invalid config name.");
         return;
      }

      if (!presetFile.exists()) {
         ChatUtils.addChatMessage("Config not found: " + this.stripExtension(presetFile.getName()));
         return;
      }

      try (Reader reader = Files.newBufferedReader(presetFile.toPath(), StandardCharsets.UTF_8)) {
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         ModuleManager moduleManager = Naven.getInstance().getModuleManager();
         ValueManager valueManager = Naven.getInstance().getValueManager();

         for (Map.Entry<String, JsonElement> moduleEntry : root.entrySet()) {
            if (!moduleEntry.getValue().isJsonObject()) {
               continue;
            }

            Module module;
            try {
               module = moduleManager.getModule(moduleEntry.getKey());
            } catch (Exception ignored) {
               continue;
            }

            JsonObject moduleObject = moduleEntry.getValue().getAsJsonObject();
            if (moduleObject.has("keyCode")) {
               module.setKey(moduleObject.get("keyCode").getAsInt());
            }

            if (moduleObject.has("hidden")) {
               module.setHidden(moduleObject.get("hidden").getAsBoolean());
            }

            if (moduleObject.has("values") && moduleObject.get("values").isJsonObject()) {
               JsonObject valuesObject = moduleObject.getAsJsonObject("values");
               for (Map.Entry<String, JsonElement> valueEntry : valuesObject.entrySet()) {
                  try {
                     Value value = valueManager.getValue(module, valueEntry.getKey());
                     this.readValue(value, valueEntry.getValue());
                  } catch (Exception exception) {
                     logger.error("Failed to load preset value {}.{}!", module.getName(), valueEntry.getKey(), exception);
                  }
               }
            }

            if (moduleObject.has("enabled")) {
               boolean enabled = moduleObject.get("enabled").getAsBoolean();
               if (module.isEnabled() != enabled) {
                  module.setEnabled(enabled);
               }
            }
         }

         Naven.getInstance().getFileManager().save();
         ChatUtils.addChatMessage("Loaded config " + this.stripExtension(presetFile.getName()) + ".");
      } catch (Exception exception) {
         logger.error("Failed to load config {}!", name, exception);
         ChatUtils.addChatMessage("Failed to load config " + name + ".");
      }
   }

   private void listPresets() {
      File[] files = this.getPresetsFolder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(CONFIG_EXTENSION));
      if (files == null || files.length == 0) {
         ChatUtils.addChatMessage("No configs found.");
         return;
      }

      Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
      String[] names = Arrays.stream(files)
         .map(file -> this.stripExtension(file.getName()))
         .toArray(String[]::new);
      ChatUtils.addChatMessage("Configs: " + String.join(", ", names));
   }

   private void writeValue(JsonObject valuesObject, Value value) {
      ValueType valueType = value.getValueType();
      switch (valueType) {
         case BOOLEAN:
            valuesObject.addProperty(value.getName(), value.getBooleanValue().getCurrentValue());
            break;
         case FLOAT:
            valuesObject.addProperty(value.getName(), value.getFloatValue().getCurrentValue());
            break;
         case STRING:
            valuesObject.addProperty(value.getName(), value.getStringValue().getCurrentValue());
            break;
         case ADDONS:
            AddonsValue addonsValue = value.getAddonsValue();
            StringBuilder builder = new StringBuilder();
            String[] addonValues = addonsValue.getValues();
            for (int i = 0; i < addonValues.length; i++) {
               if (addonsValue.isSelected(i)) {
                  if (builder.length() > 0) {
                     builder.append(";");
                  }

                  builder.append(addonValues[i]);
               }
            }

            valuesObject.addProperty(value.getName(), builder.toString());
            break;
         case MODE:
            valuesObject.addProperty(value.getName(), value.getModeValue().getCurrentMode());
            break;
         default:
            break;
      }
   }

   private void readValue(Value value, JsonElement element) {
      if (element == null || element.isJsonNull()) {
         if (value.getValueType() == ValueType.STRING) {
            value.getStringValue().setCurrentValue(null);
         }

         return;
      }

      switch (value.getValueType()) {
         case BOOLEAN:
            value.getBooleanValue().setCurrentValue(element.getAsBoolean());
            break;
         case FLOAT:
            value.getFloatValue().setCurrentValue(element.getAsFloat());
            break;
         case STRING:
            value.getStringValue().setCurrentValue(element.getAsString());
            break;
         case ADDONS:
            this.readAddonsValue(value.getAddonsValue(), element.getAsString());
            break;
         case MODE:
            this.readModeValue(value.getModeValue(), element);
            break;
         default:
            break;
      }
   }

   private void readAddonsValue(AddonsValue addonsValue, String savedValue) {
      addonsValue.clearSelection();
      if (savedValue == null || savedValue.isEmpty()) {
         return;
      }

      String[] selectedValues = savedValue.split(";");
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

   private void readModeValue(ModeValue modeValue, JsonElement element) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         int index = element.getAsInt();
         if (index >= 0 && index < modeValue.getValues().length) {
            modeValue.setCurrentValue(index);
         }

         return;
      }

      String savedMode = element.getAsString();
      String[] values = modeValue.getValues();
      for (int i = 0; i < values.length; i++) {
         if (values[i].equalsIgnoreCase(savedMode)) {
            modeValue.setCurrentValue(i);
            return;
         }
      }

      try {
         int index = Integer.parseInt(savedMode);
         if (index >= 0 && index < values.length) {
            modeValue.setCurrentValue(index);
         }
      } catch (NumberFormatException ignored) {
      }
   }

   private File getPresetFile(String name) {
      String normalizedName = this.normalizePresetName(name);
      if (normalizedName == null) {
         return null;
      }

      return new File(this.getPresetsFolder(), normalizedName + CONFIG_EXTENSION);
   }

   private File getPresetsFolder() {
      return new File(FileManager.clientFolder, "presets");
   }

   private String normalizePresetName(String name) {
      if (name == null) {
         return null;
      }

      String normalizedName = name;
      if (normalizedName.toLowerCase(Locale.ROOT).endsWith(CONFIG_EXTENSION)) {
         normalizedName = normalizedName.substring(0, normalizedName.length() - CONFIG_EXTENSION.length());
      }

      normalizedName = normalizedName.trim();
      if (normalizedName.isEmpty()
         || normalizedName.contains("..")
         || normalizedName.matches(".*[\\\\/:*?\"<>|].*")
         || normalizedName.endsWith(".")
         || normalizedName.endsWith(" ")) {
         return null;
      }

      return normalizedName;
   }

   private String stripExtension(String fileName) {
      if (fileName.toLowerCase(Locale.ROOT).endsWith(CONFIG_EXTENSION)) {
         return fileName.substring(0, fileName.length() - CONFIG_EXTENSION.length());
      }

      return fileName;
   }

   private void sendUsage() {
      ChatUtils.addChatMessage("Usage: .config <folder | save | load | list>");
   }
}
