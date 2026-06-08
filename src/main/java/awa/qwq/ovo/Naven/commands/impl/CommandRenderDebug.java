package awa.qwq.ovo.Naven.commands.impl;

import awa.qwq.ovo.Naven.commands.Command;
import awa.qwq.ovo.Naven.commands.CommandInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.RenderDebug;

@CommandInfo(
        name = "renderdebug",
        description = "Toggle render debug logging",
        aliases = {"rd", "rendebug"}
)
public class CommandRenderDebug extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            RenderDebug.toggle();
            ChatUtils.addChatMessage("§7[RenderDebug] §" + (RenderDebug.isEnabled() ? "a已开启" : "c已关闭"));
        } else if (args[0].equalsIgnoreCase("on")) {
            RenderDebug.setEnabled(true);
            ChatUtils.addChatMessage("§7[RenderDebug] §a已开启");
        } else if (args[0].equalsIgnoreCase("off")) {
            RenderDebug.setEnabled(false);
            ChatUtils.addChatMessage("§7[RenderDebug] §c已关闭");
        } else if (args[0].equalsIgnoreCase("status")) {
            ChatUtils.addChatMessage("§7[RenderDebug] §e当前状态: " + (RenderDebug.isEnabled() ? "§a开启" : "§c关闭"));
        } else {
            ChatUtils.addChatMessage("§7用法: .renderdebug [on/off/status]");
        }
    }

    @Override
    public String[] onTab(String[] args) {
        if (args.length == 1) {
            return new String[]{"on", "off", "status"};
        }
        return new String[0];
    }
}