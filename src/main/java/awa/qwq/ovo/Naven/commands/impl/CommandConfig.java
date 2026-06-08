package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.files.FileManager;
import java.io.IOException;

@CommandInfo(
   name = "config",
   description = "Open client config folder.",
   aliases = {"conf"}
)
public class CommandConfig extends Command {
   @Override
   public void onCommand(String[] args) {
      try {
         Runtime.getRuntime().exec("explorer " + FileManager.clientFolder.getAbsolutePath());
      } catch (IOException var3) {
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
