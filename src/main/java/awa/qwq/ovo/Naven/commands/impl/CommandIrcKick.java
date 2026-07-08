package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.chat.ChatClient;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;

@CommandInfo(name = "irckick", description = "Kick an IRC user from the server.", aliases = {"ikick"})
public class CommandIrcKick extends Command {
    @Override
    public void onCommand(String[] args) {
        if (!ChatClient.isAdmin()) {
            ChatUtils.addChatMessage("IRC admin permission required.");
            return;
        }
        if (args.length < 1) {
            ChatUtils.addChatMessage("Usage: .irckick <IRC name>");
            return;
        }
        ChatClient.send(".kick " + args[0]);
    }

    @Override
    public String[] onTab(String[] args) {
        String prefix = args.length == 0 ? "" : args[0].toLowerCase();
        return ChatClient.getOnlineIrcNames().stream()
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toArray(String[]::new);
    }
}
