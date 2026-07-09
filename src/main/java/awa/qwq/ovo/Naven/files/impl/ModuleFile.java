package awa.qwq.ovo.Naven.files.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.exceptions.NoSuchModuleException;
import awa.qwq.ovo.Naven.files.ClientFile;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleFile extends ClientFile {
   private static final Logger logger = LogManager.getLogger(ModuleFile.class);

   public ModuleFile() {
      super("modules.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      ModuleManager moduleManager = Naven.getInstance().getModuleManager();

      String line;
      while ((line = reader.readLine()) != null) {
         String[] split = line.split(":", 4);
         if (split.length < 3) {
            logger.error("Failed to read line {}!", line);
         } else {
            String name = split[0];
            int key = Integer.parseInt(split[1]);
            boolean enabled = Boolean.parseBoolean(split[2]);
            boolean hidden = split.length >= 4 && Boolean.parseBoolean(split[3]);

            try {
               Module module = moduleManager.getModule(name);
               module.setKey(key);
               module.setHidden(hidden);
               if (enabled && (Naven.mc == null || Naven.mc.player == null || Naven.mc.world == null)) {
                  Naven.getInstance().queuePendingEnable(module);
               } else {
                  module.setEnabled(enabled);
               }
            } catch (NoSuchModuleException var9) {
               logger.error("Failed to find module {}!", name);
            }
         }
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      ModuleManager moduleManager = Naven.getInstance().getModuleManager();

      for (Module module : new ArrayList<>(moduleManager.getModules())) {
         writer.write(String.format("%s:%d:%s:%s\n", module.getName(), module.getKey(), module.isEnabled(), module.isHidden()));
      }
   }
}
