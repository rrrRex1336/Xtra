package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.chat.IrcClient;
import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

@CommandInfo(
        name = "irc",
        description = "Send or inspect IRC chat.",
        aliases = {"chat", "c"}
)
public class CommandIrc extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "online":
            case "list":
                sendOnlineUsers();
                break;
            case "name":
                sendName();
                break;
            case "send":
                if (args.length < 2) {
                    sendUsage();
                    return;
                }
                IrcClient.send(join(args, 1));
                break;
            default:
                IrcClient.send(join(args, 0));
                break;
        }
    }

    @Override
    public String[] onTab(String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(new String[]{"online", "list", "name", "send"})
                    .filter(command -> command.startsWith(prefix))
                    .toArray(String[]::new);
        }
        return new String[0];
    }

    private void sendOnlineUsers() {
        Map<String, IrcClient.OnlineUser> users = IrcClient.getOnlineUserInfo();
        if (users.isEmpty()) {
            ChatUtils.addChatMessage("IRC online list is empty.");
            return;
        }

        ChatUtils.addChatMessage("IRC online users: " + users.size());
        users.values().stream()
                .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                .forEach(user -> ChatUtils.addChatMessage(false,
                        "§7 - §f" + user.displayName() + " §8(" + user.username() + ")"));
    }

    private void sendName() {
        String name = IrcClient.displayName();
        ChatUtils.addChatMessage("IRC name: " + (name == null || name.isEmpty() ? "not assigned yet" : name));
    }

    private void sendUsage() {
        ChatUtils.addChatMessage("Usage: .irc <message> | .irc online | .irc name");
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
