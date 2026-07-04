package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.chat.IrcClient;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import net.minecraft.ChatFormatting;
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
                        new Notification(NotificationLevel.WARNING, "让你关了?", 3000L)
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
        IrcClient.stop();
        this.setSuffix(null);
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        this.setSuffix(IrcClient.hasJoinedIrc() ? "Online" : IrcClient.isRunning() ? "Connecting" : "Offline");
        if (IrcClient.isRunning()) {
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
        if (!IrcClient.hasJoinedIrc() || event.getType() != EventType.NAME || event.getPlayerInfo() == null) {
            return;
        }

        String playerName = event.getPlayerInfo().getProfile().getName();
        String ircName = IrcClient.getIrcName(playerName);
        if (ircName.isEmpty()) {
            return;
        }

        Component friend = Component.literal("[Friend]").withStyle(ChatFormatting.GREEN);
        Component name = Component.literal(ircName + " " + playerName).withStyle(ChatFormatting.AQUA);
        event.setComponent(friend.copy().append(name));
    }

    private void tryConnect() {
        if (VerifyClient.getIrcToken().isEmpty()) {
            return;
        }
        IrcClient.init();
    }
}
