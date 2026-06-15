package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.exceptions.NoSuchModuleException;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.ChatUtils;

@CommandInfo(
        name = "hide",
        description = "hide module in arraylist",
        aliases = {"h"}
)

public class CommandHide extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            String moduleName = args[0];

            try {
                Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
                if (module != null) {
                    module.setHidden(!module.isHidden());
                    Naven.getInstance().getFileManager().save();
                    ChatUtils.addChatMessage(module.getName() + (module.isHidden() ? " hidden." : " visible."));
                } else {
                    ChatUtils.addChatMessage("Invalid module.");
                }
            } catch (NoSuchModuleException var4) {
                ChatUtils.addChatMessage("Invalid module.");
            }
        } else {
            ChatUtils.addChatMessage("Usage: .hide <module>");
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
}
