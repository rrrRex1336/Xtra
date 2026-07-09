package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.chat.ChatClient;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

@ModuleInfo(
        name = "IRC",
        description = "Connects to Naven IRC.",
        category = Category.MISC
)
public class IRC extends Module {
    private static final long IRC_FRIEND_COOLDOWN_MS = 15_000L;
    private static long ircFriendCooldownUntil;

    public final BooleanValue ircFriend = ValueBuilder.create(this, "Irc Friend")
            .setDefaultBooleanValue(true)
            .setOnUpdate(this::onIrcFriendChanged)
            .build()
            .getBooleanValue();

    public final BooleanValue hideAdmin = ValueBuilder.create(this, "Hide Admin")
            .setDefaultBooleanValue(false)
            .setVisibility(ChatClient::isAdmin)
            .setOnUpdate(value -> ChatClient.setChatHide(((BooleanValue) value).getCurrentValue() && ChatClient.isAdmin()))
            .build()
            .getBooleanValue();

    private int retryTicks;
    private boolean lastIrcFriend = true;

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
        ChatClient.setChatHide(this.hideAdmin.getCurrentValue() && ChatClient.isAdmin());
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

        event.setComponent(Text.literal(ircStatusPrefix(playerName) + playerName));
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (event.isPost() || !(event.getTarget() instanceof PlayerEntity player)) {
            return;
        }

        if (shouldProtectIrcUser(player.getName().getString())) {
            event.setCancelled(true);
        }
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
        String friend = shouldProtectIrcUser(mcName) ? "\u00a7a[Friend] " : "";
        return friend + clientLabel + ChatClient.getIrcColorCode(mcName) + "[" + label + "] \u00a7r";
    }

    public static boolean shouldProtectIrcUser(String mcName) {
        if (!ChatClient.isIrcUser(mcName) || !ChatClient.isIrcFriendEnabled(mcName)) {
            return false;
        }

        IRC module = module();
        if (module == null) {
            return true;
        }
        return module.ircFriend.getCurrentValue() || System.currentTimeMillis() < ircFriendCooldownUntil;
    }

    private void onIrcFriendChanged(Value value) {
        boolean current = ((BooleanValue) value).getCurrentValue();
        if (this.lastIrcFriend && !current) {
            ircFriendCooldownUntil = System.currentTimeMillis() + IRC_FRIEND_COOLDOWN_MS;
            if (ChatClient.hasJoinedIrc()) {
                ChatClient.send("IrcFriend disabled. Protection remains for 15s.");
            }
        }
        this.lastIrcFriend = current;
    }

    private static IRC module() {
        try {
            if (Naven.getInstance() == null || Naven.getInstance().getModuleManager() == null) {
                return null;
            }
            return (IRC) Naven.getInstance().getModuleManager().getModule(IRC.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
