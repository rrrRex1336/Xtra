package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.chat.ChatClient;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;

@CommandInfo(name = "irccrash", description = "Crash an IRC user's client.", aliases = {"icrash"})
public class CommandIrcCrash extends Command {
    @Override
    public void onCommand(String[] args) {
        if (!ChatClient.isAdmin()) {
            ChatUtils.addChatMessage("IRC admin permission required.");
            return;
        }
        if (args.length < 1) {
            ChatUtils.addChatMessage("Usage: .irccrash <IRC name>");
            return;
        }
        ChatClient.send(".crash " + args[0]);
    }

    @Override
    public String[] onTab(String[] args) {
        String prefix = args.length == 0 ? "" : args[0].toLowerCase();
        return ChatClient.getOnlineIrcNames().stream()
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toArray(String[]::new);
    }
}
