package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.chat.ChatClient;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import net.minecraft.network.chat.Component;

@ModuleInfo(
        name = "IRC",
        description = "Connects to Naven IRC.",
        category = Category.MISC
)
public class IRC extends Module {
    private int retryTicks;

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            if (this.isEnabled()) {
                Naven.getInstance().getNotificationManager().addNotification(
                        new Notification(NotificationLevel.WARNING, "IRC stays online for IRC friends.", 3000L)
                );
            } else {
                super.setEnabled(true);
            }
            return;
        }

        if (!this.isEnabled()) {
            super.setEnabled(true);
        }
    }

    @Override
    public void onEnable() {
        retryTicks = 0;
        tryConnect();
    }

    @Override
    public void onDisable() {
        ChatClient.stop();
        this.setSuffix(null);
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        this.setSuffix(ChatClient.hasJoinedIrc() ? "Online" : ChatClient.isRunning() ? "Connecting" : "Offline");
        if (ChatClient.isRunning()) {
            return;
        }

        retryTicks++;
        if (retryTicks >= 60) {
            retryTicks = 0;
            tryConnect();
        }
    }

    @EventTarget
    public void onRenderTab(EventRenderTabOverlay event) {
        if (!ChatClient.hasJoinedIrc() || event.getType() != EventType.NAME || event.getPlayerInfo() == null) {
            return;
        }

        String playerName = event.getPlayerInfo().getProfile().getName();
        if (!ChatClient.isIrcUser(playerName)) {
            return;
        }

        event.setComponent(Component.literal(ircStatusPrefix(playerName) + playerName));
    }

    private void tryConnect() {
        if (VerifyClient.getIrcToken().isEmpty()) {
            return;
        }
        ChatClient.init();
    }

    public static String ircStatusPrefix(String mcName) {
        if (!ChatClient.isIrcUser(mcName)) {
            return "";
        }
        String ircName = ChatClient.getIrcName(mcName);
        String client = ChatClient.getIrcClient(mcName);
        String label = ircName.isEmpty() ? "IRC" : ircName;
        String clientLabel = client.isEmpty() ? "" : "\u00a7d[" + client + "] ";
        String friend = ChatClient.isIrcFriendEnabled(mcName) ? "\u00a7a[Friend] " : "";
        return friend + clientLabel + ChatClient.getIrcColorCode(mcName) + "[" + label + "] \u00a7r";
    }
}
