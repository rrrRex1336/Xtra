package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import awa.qwq.ovo.Naven.exceptions.NoSuchModuleException;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

@CommandInfo(
   name = "bind",
   description = "Bind a command to a key",
   aliases = {"b"}
)
public class CommandBind extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         if (args[0].equalsIgnoreCase("s")) {
            this.listBoundModules();
            return;
         }

         final String moduleName = args[0];

         try {
            final Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               ChatUtils.addChatMessage("Press a key to bind " + moduleName + " to.");
               Naven.getInstance().getEventManager().register(new Object() {
                  @EventTarget
                  public void onKey(EventKey e) {
                     if (e.isState()) {
                        module.setKey(e.getKey());
                        Key key = InputConstants.getKey(e.getKey(), 0);
                        String keyName = key.getDisplayName().getString().toUpperCase();
                        ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName + ".");
                        Naven.getInstance().getEventManager().unregister(this);
                        Naven.getInstance().getFileManager().save();
                     }
                  }
               });
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var7) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else if (args.length == 2) {
         String moduleName = args[0];
         String keyName = args[1];

         try {
            Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               if (keyName.equalsIgnoreCase("none")) {
                  module.setKey(InputConstants.UNKNOWN.getValue());
                  ChatUtils.addChatMessage("Unbound " + moduleName + ".");
                  Naven.getInstance().getFileManager().save();
               } else {
                  Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase());
                  if (key != InputConstants.UNKNOWN) {
                     module.setKey(key.getValue());
                     ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName.toUpperCase() + ".");
                     Naven.getInstance().getFileManager().save();
                  } else {
                     ChatUtils.addChatMessage("Invalid key.");
                  }
               }
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var6) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else {
         ChatUtils.addChatMessage("Usage: .bind <module> [key] | .bind s");
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return Naven.getInstance()
         .getModuleManager()
         .getModules()
         .stream()
         .map(Module::getName)
         .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
         .toArray(String[]::new);
   }

   private void listBoundModules() {
      boolean hasBoundModule = false;

      for (Module module : Naven.getInstance().getModuleManager().getModules()) {
         if (this.isBoundKey(module.getKey())) {
            if (!hasBoundModule) {
               ChatUtils.addChatMessage("Bound modules:");
               hasBoundModule = true;
            }

            ChatUtils.addChatMessage(module.getName() + " [" + this.getKeyName(module.getKey()) + "]");
         }
      }

      if (!hasBoundModule) {
         ChatUtils.addChatMessage("No modules are bound.");
      }
   }

   private boolean isBoundKey(int keyCode) {
      return keyCode != 0 && keyCode != InputConstants.UNKNOWN.getValue();
   }

   private String getKeyName(int keyCode) {
      if (keyCode < 0) {
         return switch (-keyCode) {
            case 2 -> "MOUSE MIDDLE";
            case 3 -> "MOUSE 4";
            case 4 -> "MOUSE 5";
            default -> "MOUSE " + -keyCode;
         };
      }

      Key key = InputConstants.getKey(keyCode, 0);
      return key.getDisplayName().getString().toUpperCase();
   }
}
