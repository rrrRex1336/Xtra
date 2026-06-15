package awa.qwq.ovo.Naven.commands;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.commands.impl.*;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventClientChat;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
   public static final String PREFIX = ".";
   public final Map<String, Command> aliasMap = new HashMap<>();

   public CommandManager() {
      try {
         this.initCommands();
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }

      Naven.getInstance().getEventManager().register(this);
   }

   private void initCommands() {
      this.registerCommand(new CommandBind());
      this.registerCommand(new CommandToggle());
      this.registerCommand(new CommandConfig());
      this.registerCommand(new CommandLanguage());
      this.registerCommand(new CommandHide());
      this.registerCommand(new CommandProxy());
   }

   private void registerCommand(Command command) {
      command.initCommand();
      this.aliasMap.put(command.getName().toLowerCase(), command);

      for (String alias : command.getAliases()) {
         this.aliasMap.put(alias.toLowerCase(), command);
      }
   }

   @EventTarget
   public void onChat(EventClientChat e) {
      if (e.getMessage().startsWith(".")) {
         e.setCancelled(true);
         String chatMessage = e.getMessage().substring(".".length());
         String[] arguments = chatMessage.split(" ");
         if (arguments.length < 1) {
            ChatUtils.addChatMessage("Invalid command.");
            return;
         }

         String alias = arguments[0].toLowerCase();
         Command command = this.aliasMap.get(alias);
         if (command == null) {
            ChatUtils.addChatMessage("Invalid command.");
            return;
         }

         String[] args = new String[arguments.length - 1];
         System.arraycopy(arguments, 1, args, 0, args.length);
         command.onCommand(args);
      }
   }
}
