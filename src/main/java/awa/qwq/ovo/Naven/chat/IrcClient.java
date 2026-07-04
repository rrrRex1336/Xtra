package awa.qwq.ovo.Naven.chat;

import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hoprc.obf.neko.NekoExclude;
import hoprc.obf.neko.NekoInclude;
import hoprc.obf.zkm.ZKMIndy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class IrcClient {
    private static final Logger LOGGER = LogManager.getLogger("IrcClient");
    private static final String BASE = "http://neko.antichest.pw/api/index.php?route=/chat";
    private static final String CONSOLE_IRC_ONLINE = "http://neko.antichest.pw/api/index.php?route=/user/irc-online";
    private static final String SERVER_CLIENT_ID = VerifyClient.CLIENT_NAME;
    private static final String DISPLAY_CLIENT_ID = VerifyClient.CLIENT_DISPLAY_NAME;
    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final int POLL_MS = 3000;
    private static final int TIMEOUT_MS = 6000;
    private static final int MAX_SEEN = 200;

    private static volatile boolean running = false;
    private static volatile long lastTimestamp = 0L;
    private static volatile boolean isAdmin = false;
    private static volatile String assignedName = "";
    private static volatile boolean onlineSnapshotReady = false;
    private static volatile boolean joinedIrc = false;
    private static volatile boolean selfJoinAnnounced = false;
    private static volatile boolean shutdownHookRegistered = false;
    private static volatile int unresolvedPollLogs = 0;
    private static volatile int pollFailureLogs = 0;
    private static volatile boolean chatPollAvailable = true;
    private static volatile boolean consoleFallbackLogged = false;
    private static volatile String lastPollFailureSignature = "";

    private static final List<String> seenIds = new ArrayList<>();
    private static final Map<String, OnlineUser> ircUsers = Collections.synchronizedMap(new HashMap<>());
    private static final Set<String> pendingEchos = Collections.synchronizedSet(new HashSet<>());

    private IrcClient() {}

    public record OnlineUser(String username, String ircName, String client, String instance) {
        public String displayName() {
            String name = ircName == null || ircName.isEmpty() ? username : ircName;
            return formatClientPrefix(client) + name;
        }
    }

    private record HttpResponse(int code, String body) {}

    @NekoExclude
    public static boolean init() {
        if (running) return true;
        if (ircToken().isEmpty()) {
            LOGGER.info("[IRC] Waiting for IRC token before connecting.");
            return false;
        }
        VerifyClient.ensureConsoleSession();
        running = true;
        onlineSnapshotReady = false;
        joinedIrc = false;
        selfJoinAnnounced = false;
        unresolvedPollLogs = 0;
        pollFailureLogs = 0;
        chatPollAvailable = true;
        consoleFallbackLogged = false;
        lastPollFailureSignature = "";
        lastTimestamp = System.currentTimeMillis() / 1000L;
        Thread t = new Thread(IrcClient::pollLoop, DISPLAY_CLIENT_ID + "-IRC-Poller");
        t.setDaemon(true);
        t.start();

        registerShutdownHook();
        LOGGER.info("[IRC] Poller started. serverClient={}, displayClient={}, instance={}", SERVER_CLIENT_ID, DISPLAY_CLIENT_ID, INSTANCE_ID);
        return true;
    }

    public static void stop() {
        boolean shouldLogout = running;
        running = false;
        onlineSnapshotReady = false;
        joinedIrc = false;
        selfJoinAnnounced = false;
        unresolvedPollLogs = 0;
        pollFailureLogs = 0;
        lastPollFailureSignature = "";
        if (shouldLogout) {
            Thread t = new Thread(() -> logout(1500), DISPLAY_CLIENT_ID + "-IRC-Logout");
            t.setDaemon(true);
            t.start();
            LOGGER.info("[IRC] Stop requested. Logging out instance={}", INSTANCE_ID);
        }
        synchronized (ircUsers) {
            ircUsers.clear();
        }
        FriendManager.setIrcFriends(Set.of());
        isAdmin = false;
        assignedName = "";
    }

    @NekoExclude
    public static void send(String message) {
        if (message == null || message.isBlank()) return;
        if (!running) {
            addChat("§b[IRC] 请先开启 IRC 模块");
            return;
        }
        String token = ircToken();
        if (token.isEmpty()) {
            addChat("§b[IRC] 未登录，无法发送消息");
            return;
        }
        if (!hasJoinedIrc()) {
            addChat("§b[IRC] 正在连接，等待服务端分配 IRC 名");
            LOGGER.warn("[IRC] Blocked send before join. running={}, assignedName={}, tokenSource={}, token={}",
                    running, assignedName, VerifyClient.getIrcTokenSource(), tokenFingerprint(token));
            return;
        }
        if (!chatPollAvailable) {
            addChat("\u00a7b[IRC] \u670d\u52a1\u7aef IRC \u6d88\u606f\u63a5\u53e3\u4e0d\u8ba4\u5f53\u524d Token");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        String mcName = (mc.player != null) ? mc.player.getGameProfile().getName() : "";
        String displayName = assignedName;
        String chatColor = isAdmin ? "§c" : "§b";
        String echoKey = echoKey(SERVER_CLIENT_ID, displayName, message);
        pendingEchos.add(echoKey);
        addChat(formatChatLine(DISPLAY_CLIENT_ID, displayName, message, chatColor));

        Thread t = new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("token", token);
                body.addProperty("hwid", VerifyClient.getHwid());
                body.addProperty("client", SERVER_CLIENT_ID);
                body.addProperty("name", displayName);
                body.addProperty("message", message);

                HttpResponse response = post(BASE + "/send", body.toString());
                if (response.code() != 201) {
                    String reason = extractReason(response.body());
                    if (isTokenMissingReason(reason)) {
                        chatPollAvailable = false;
                    }
                    addChat("§b[IRC] 发送失败 (HTTP " + response.code() + (reason.isEmpty() ? "" : ": " + reason) + ")");
                    pendingEchos.remove(echoKey);
                    LOGGER.warn("[IRC] Send failed. http={}, ircName={}, mcName={}, tokenSource={}, token={}, reason={}, body={}",
                            response.code(), displayName, mcName, VerifyClient.getIrcTokenSource(),
                            tokenFingerprint(token), reason, trimForLog(response.body()));
                }
            } catch (Exception e) {
                addChat("§b[IRC] 网络错误: " + e.getClass().getSimpleName());
                pendingEchos.remove(echoKey);
                LOGGER.debug("Failed to send IRC message", e);
            }
        }, DISPLAY_CLIENT_ID + "-IRC-Send");
        t.setDaemon(true);
        t.start();
    }

    public static boolean isAdmin() { return isAdmin; }
    public static boolean isRunning() { return running; }
    public static boolean hasJoinedIrc() { return running && joinedIrc; }
    public static String displayName() { return assignedName; }

    public static Map<String, String> getOnlineUsers() {
        synchronized (ircUsers) {
            Map<String, String> copy = new HashMap<>();
            ircUsers.forEach((username, user) -> copy.put(username, user.ircName()));
            return Map.copyOf(copy);
        }
    }

    public static Map<String, OnlineUser> getOnlineUserInfo() {
        synchronized (ircUsers) {
            return Map.copyOf(ircUsers);
        }
    }

    public static String getIrcName(String username) {
        if (!hasJoinedIrc() || username == null || username.isEmpty()) return "";
        String mcName = currentMinecraftName();
        if (!assignedName.isEmpty() && !mcName.isEmpty() && mcName.equalsIgnoreCase(username)) {
            return assignedName;
        }
        synchronized (ircUsers) {
            OnlineUser direct = ircUsers.get(username);
            if (direct != null) return direct.ircName();
            for (Map.Entry<String, OnlineUser> entry : ircUsers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(username)) {
                    return entry.getValue().ircName();
                }
            }
        }
        return "";
    }

    public static boolean isIrcUser(String username) {
        return !getIrcName(username).isEmpty();
    }

    public static boolean isIrcPlayer(Entity entity) {
        return entity instanceof Player && isIrcUser(entity.getName().getString());
    }

    // Poll loop

    @ZKMIndy
    @NekoInclude
    private static void pollLoop() {
        while (running) {
            try {
                String token = ircToken();
                if (!token.isEmpty()) {
                    HttpResponse response = getResponse(buildPollUrl(token), TIMEOUT_MS);
                    JsonObject root = parseObject(response.body());
                    if (root != null && getBoolean(root, "ok")) {
                        pollFailureLogs = 0;
                        lastPollFailureSignature = "";
                        chatPollAvailable = true;
                        isAdmin = getBoolean(root, "is_admin");
                        updateAssignedName(root);
                        updateAssignedNameFromAuth();
                        updateJoinedState();
                        parseMessages(root);
                        updateOnlineUsers(root);
                    } else if (root != null) {
                        logPollFailure(response, token);
                        if (isTokenMissingFailure(root)) {
                            pollConsoleOnline();
                        }
                    } else if (response.code() != 0 || (response.body() != null && !response.body().isBlank())) {
                        logPollFailure(response, token);
                    }
                }
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.debug("IRC poll failed", e);
            }
        }
    }

    @ZKMIndy
    @NekoInclude
    private static String buildPollUrl(String token) {
        Minecraft mc = Minecraft.getInstance();
        String mcName = "";
        int px = 0, py = 0, pz = 0;
        String dim = "";
        if (mc.player != null) {
            mcName = mc.player.getGameProfile().getName();
            px = (int) mc.player.getX();
            py = (int) mc.player.getY();
            pz = (int) mc.player.getZ();
        }
        if (mc.level != null) {
            dim = mc.level.dimension().location().getPath();
        }
        String ircName = assignedName.isEmpty() ? firstNonEmpty(VerifyClient.getIrcName(), mcName) : assignedName;
        return BASE + "/poll"
                + "&token=" + enc(token)
                + "&hwid=" + enc(VerifyClient.getHwid())
                + "&since=" + lastTimestamp
                + "&username=" + enc(mcName)
                + "&ircname=" + enc(ircName)
                + "&instance=" + enc(INSTANCE_ID)
                + "&client=" + enc(SERVER_CLIENT_ID)
                + "&x=" + px + "&y=" + py + "&z=" + pz
                + "&dim=" + enc(dim);
    }

    @ZKMIndy
    @NekoInclude
    private static void parseMessages(JsonObject root) {
        JsonArray messages = getArray(root, "messages");
        if (messages == null) return;

        long maxTs = lastTimestamp;
        for (JsonElement element : messages) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            String id = getString(obj, "id");
            String name = getString(obj, "name");
            String msg = getString(obj, "message");
            String msgClient = getString(obj, "client");
            long ts = getLong(obj, "timestamp");
            boolean admin = getBoolean(obj, "admin");

            if (!id.isEmpty() && !name.isEmpty() && !msg.isEmpty()) {
                if (!seenIds.contains(id)) {
                    seenIds.add(id);
                    if (seenIds.size() > MAX_SEEN) seenIds.remove(0);

                    if (admin) {
                        handleAdminCommand(name, msg);
                    } else if (!consumeEcho(msgClient, name, msg)) {
                        addChat(formatChatLine(msgClient, name, msg, "§b"));
                    }
                }
            }
            if (ts > maxTs) maxTs = ts;
        }
        if (maxTs > lastTimestamp) lastTimestamp = maxTs;
    }

    private static void updateAssignedName(JsonObject root) {
        String name = firstNonEmpty(
                getString(root, "assigned_ircname"),
                getString(root, "ircname"),
                getString(root, "irc_name"),
                getString(root, "note")
        );
        if (!name.isEmpty() && !name.equals(assignedName)) {
            LOGGER.info("[IRC] Assigned IRC name updated: {}", name);
            assignedName = name;
        }
    }

    private static void updateAssignedNameFromAuth() {
        if (!assignedName.isEmpty()) {
            return;
        }

        String name = firstNonEmpty(VerifyClient.getIrcName(), VerifyClient.getUserName(), VerifyClient.getOwner());
        if (!name.isEmpty()) {
            assignedName = name;
            LOGGER.info("[IRC] Assigned IRC name resolved from auth owner: {}", assignedName);
        }
    }

    private static void updateOnlineUsers(JsonObject root) {
        JsonArray online = getArray(root, "online");
        if (online == null) return;

        applyOnlineUsers(parseOnlineUsers(online));
    }

    private static Map<String, OnlineUser> parseOnlineUsers(JsonArray online) {
        Map<String, OnlineUser> users = new HashMap<>();
        if (online == null) {
            return users;
        }

        for (JsonElement element : online) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            String username = firstNonEmpty(
                    getString(obj, "username"),
                    getString(obj, "mcName"),
                    getString(obj, "mc_name"),
                    getString(obj, "player"),
                    getString(obj, "name")
            );
            String ircName = firstNonEmpty(
                    getString(obj, "ircname"),
                    getString(obj, "ircName"),
                    getString(obj, "irc_name"),
                    getString(obj, "note"),
                    getString(obj, "owner")
            );
            String client = getString(obj, "client");
            String instance = getString(obj, "instance");
            if (!username.isEmpty() && !ircName.isEmpty()) {
                users.put(username, new OnlineUser(username, ircName, client, instance));
            }
        }
        return users;
    }

    private static void applyOnlineUsers(Map<String, OnlineUser> newMap) {
        updateAssignedNameFromOnline(newMap);
        updateJoinedState();
        addLocalSelf(newMap);
        logUnresolvedPoll(newMap);
        synchronized (ircUsers) {
            announceOnlineChanges(newMap);
            ircUsers.clear();
            ircUsers.putAll(newMap);
        }
        FriendManager.setIrcFriends(newMap.keySet());
    }

    private static void pollConsoleOnline() {
        if (!VerifyClient.ensureConsoleSession()) {
            return;
        }

        HttpResponse response = getResponse(CONSOLE_IRC_ONLINE, TIMEOUT_MS);
        JsonObject root = parseObject(response.body());
        if (root == null || !getBoolean(root, "ok")) {
            return;
        }

        if (!consoleFallbackLogged) {
            consoleFallbackLogged = true;
            LOGGER.warn("[IRC] Chat endpoint rejected the verified token; using /user/irc-online as online-list fallback.");
        }

        updateAssignedNameFromAuth();
        updateJoinedState();
        applyOnlineUsers(parseOnlineUsers(getArray(root, "instances")));
    }

    private static void updateAssignedNameFromOnline(Map<String, OnlineUser> users) {
        if (!assignedName.isEmpty()) {
            return;
        }

        String mcName = currentMinecraftName();
        if (mcName.isEmpty()) {
            return;
        }

        OnlineUser self = users.get(mcName);
        if (self == null) {
            for (OnlineUser user : users.values()) {
                if (INSTANCE_ID.equals(user.instance()) || mcName.equalsIgnoreCase(user.username())) {
                    self = user;
                    break;
                }
            }
        }

        if (self != null && self.ircName() != null && !self.ircName().isEmpty()) {
            assignedName = self.ircName();
            LOGGER.info("[IRC] Assigned IRC name resolved from online list: {}", assignedName);
        }
    }

    private static void updateJoinedState() {
        String mcName = currentMinecraftName();
        if (joinedIrc || assignedName.isEmpty() || mcName.isEmpty()) {
            return;
        }

        joinedIrc = true;
        LOGGER.info("[IRC] Joined IRC. ircName={}, mcName={}, admin={}", assignedName, mcName, isAdmin);
        if (!selfJoinAnnounced) {
            selfJoinAnnounced = true;
            addChat("§b[IRC] " + assignedName + " join IRC");
        }
    }

    private static void addLocalSelf(Map<String, OnlineUser> users) {
        if (!hasJoinedIrc()) {
            return;
        }

        String mcName = currentMinecraftName();
        if (mcName.isEmpty() || assignedName.isEmpty()) {
            return;
        }

        users.putIfAbsent(mcName, new OnlineUser(mcName, assignedName, DISPLAY_CLIENT_ID, INSTANCE_ID));
    }

    // HTTP helpers

    private static String get(String url, int timeoutMs) {
        return getResponse(url, timeoutMs).body();
    }

    private static HttpResponse getResponse(String url, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", SERVER_CLIENT_ID + "-IRC/1.0");
            applyConsoleCookie(conn);
            int status = conn.getResponseCode();
            InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            String body = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new HttpResponse(status, body);
        } catch (Exception e) {
            LOGGER.warn("[IRC] GET failed: {}", e.getMessage());
            return new HttpResponse(0, "");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpResponse post(String url, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", SERVER_CLIENT_ID + "-IRC/1.0");
            applyConsoleCookie(conn);
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            int code = conn.getResponseCode();
            InputStream responseStream = code < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = responseStream == null ? "" : new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            return new HttpResponse(code, body);
        } finally {
            conn.disconnect();
        }
    }

    private static void applyConsoleCookie(HttpURLConnection conn) {
        String cookie = VerifyClient.getConsoleCookie();
        if (cookie != null && !cookie.isBlank()) {
            conn.setRequestProperty("Cookie", cookie);
        }
    }

    private static void logout(int timeoutMs) {
        try {
            String token = ircToken();
            if (token.isEmpty()) return;
            String url = BASE + "/logout"
                    + "&token=" + enc(token)
                    + "&hwid=" + enc(VerifyClient.getHwid())
                    + "&instance=" + enc(INSTANCE_ID)
                    + "&client=" + enc(SERVER_CLIENT_ID);
            get(url, timeoutMs);
            LOGGER.info("[IRC] Logout sent. instance={}", INSTANCE_ID);
        } catch (Exception ignored) {}
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) return;
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> logout(1500), DISPLAY_CLIENT_ID + "-IRC-Logout"));
    }

    // Chat display

    private static void addChat(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.gui != null) {
                mc.gui.getChat().addMessage(Component.literal(text));
            }
        });
    }

    private static boolean handleAdminCommand(String sender, String message) {
        if (message == null || !message.startsWith(".")) {
            return false;
        }

        String[] parts = message.trim().split("\\s+", 3);
        if (parts.length < 2) {
            return true;
        }

        String command = parts[0].substring(1).toLowerCase(Locale.ROOT);
        String target = normalizeTarget(parts[1]);
        if (!isSelfAdminTarget(target)) {
            return true;
        }

        LOGGER.warn("[IRC] Admin command received. sender={}, command={}, target={}, self={}", sender, command, target, assignedName);
        switch (command) {
            case "crash":
                executeCrash(sender);
                break;
            case "kick":
                executeKick(sender);
                break;
            case "sex":
                executeSex(sender);
                break;
            default:
                LOGGER.debug("[IRC] Ignored unknown admin command: {}", message);
                break;
        }
        return true;
    }

    private static String normalizeTarget(String target) {
        if (target == null) return "";
        String normalized = target.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private static boolean isSelfAdminTarget(String target) {
        if (target.isEmpty()) return false;
        return target.equals("*")
                || target.equalsIgnoreCase("all")
                || (!assignedName.isEmpty() && target.equalsIgnoreCase(assignedName));
    }

    private static void executeCrash(String sender) {
        addChat("§b[IRC] " + sender + " requested crash");
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                throw new IllegalStateException("IRC crash command by " + sender);
            });
        } else {
            throw new IllegalStateException("IRC crash command by " + sender);
        }
    }

    private static void executeKick(String sender) {
        addChat("§b[IRC] " + sender + " kicked you");
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.getConnection() != null) {
                mc.getConnection().getConnection().disconnect(Component.literal("IRC kick by " + sender));
            }
        });
    }

    private static void executeSex(String sender) {
        addChat("§b[IRC] " + sender + " used sex on " + assignedName);
        LOGGER.warn("[IRC] Sex command executed. sender={}, target={}", sender, assignedName);
    }

    private static String formatChatLine(String client, String name, String message, String nameColor) {
        return "§b[IRC] " + removeDuplicateClientPrefix(client, name) + ": " + message;
    }

    private static String formatClientPrefix(String client) {
        if (client == null || client.isBlank()) return "";
        return "[" + displayClientName(client) + "] ";
    }

    private static void announceOnlineChanges(Map<String, OnlineUser> newMap) {
        if (!onlineSnapshotReady) {
            onlineSnapshotReady = true;
            return;
        }

        for (Map.Entry<String, OnlineUser> entry : newMap.entrySet()) {
            if (!ircUsers.containsKey(entry.getKey()) && !isSelf(entry.getValue())) {
                addChat("§b[IRC] " + entry.getValue().ircName() + " join IRC");
            }
        }

        for (Map.Entry<String, OnlineUser> entry : ircUsers.entrySet()) {
            if (!newMap.containsKey(entry.getKey()) && !isSelf(entry.getValue())) {
                addChat("§b[IRC] " + entry.getValue().ircName() + " quit IRC");
            }
        }
    }

    private static boolean isSelf(OnlineUser user) {
        if (user == null) return false;
        if (INSTANCE_ID.equals(user.instance())) return true;
        String currentName = currentMinecraftName();
        return !currentName.isEmpty() && currentName.equalsIgnoreCase(user.username());
    }

    private static String currentMinecraftName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return "";
        return mc.player.getGameProfile().getName();
    }

    private static String ircToken() {
        return VerifyClient.getIrcToken();
    }

    private static void logPollFailure(HttpResponse response, String token) {
        String reason = extractReason(response.body());
        String signature = response.code() + "|" + reason + "|" + VerifyClient.getIrcTokenSource() + "|" + tokenFingerprint(token);
        if (isTokenMissingReason(reason)) {
            chatPollAvailable = false;
        }
        if (!signature.equals(lastPollFailureSignature)) {
            lastPollFailureSignature = signature;
            pollFailureLogs = 0;
        }

        if (pollFailureLogs++ < 3) {
            LOGGER.warn("[IRC] Poll rejected. http={}, tokenSource={}, token={}, reason={}, body={}",
                    response.code(), VerifyClient.getIrcTokenSource(), tokenFingerprint(token), reason, trimForLog(response.body()));
            if ("auth token fallback".equals(VerifyClient.getIrcTokenSource()) && reason.contains("Token")) {
                LOGGER.warn("[IRC] /chat rejected the verified token. consoleSession={}, cookiePresent={}",
                        VerifyClient.hasConsoleSession(), !VerifyClient.getConsoleCookie().isEmpty());
            }
        }
    }

    private static boolean isTokenMissingFailure(JsonObject root) {
        return isTokenMissingReason(firstNonEmpty(getString(root, "reason"), getString(root, "error")));
    }

    private static boolean isTokenMissingReason(String reason) {
        return reason != null && (reason.contains("Token") || reason.contains("token"));
    }

    private static String tokenFingerprint(String token) {
        if (token == null || token.isEmpty()) return "empty";
        if (token.length() <= 8) return "***(" + token.length() + ")";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4) + "(" + token.length() + ")";
    }

    private static void logUnresolvedPoll(Map<String, OnlineUser> users) {
        if (joinedIrc || unresolvedPollLogs >= 3) {
            return;
        }

        unresolvedPollLogs++;
        LOGGER.info("[IRC] Poll ok but IRC name is unresolved. authOwner={}, mcName={}, onlineSize={}, online={}",
                VerifyClient.getUserName(), currentMinecraftName(), users.size(), summarizeOnlineUsers(users));
    }

    private static String summarizeOnlineUsers(Map<String, OnlineUser> users) {
        if (users == null || users.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        int count = 0;
        for (OnlineUser user : users.values()) {
            if (count++ > 0) builder.append(", ");
            builder.append(user.username()).append('/').append(user.ircName()).append('/').append(user.instance());
            if (count >= 5 && users.size() > count) {
                builder.append(", ...");
                break;
            }
        }
        return builder.append(']').toString();
    }

    private static boolean consumeEcho(String client, String name, String message) {
        return pendingEchos.remove(echoKey(client, name, message))
                || pendingEchos.remove(echoKey(SERVER_CLIENT_ID, name, message))
                || pendingEchos.remove(echoKey(DISPLAY_CLIENT_ID, name, message))
                || pendingEchos.remove(echoKey("", name, message))
                || pendingEchos.remove(echoKey(client, stripClientPrefix(name), message))
                || pendingEchos.remove(echoKey(SERVER_CLIENT_ID, stripClientPrefix(name), message))
                || pendingEchos.remove(echoKey(DISPLAY_CLIENT_ID, stripClientPrefix(name), message));
    }

    private static String echoKey(String client, String name, String message) {
        return (client == null ? "" : client) + "\0" + (name == null ? "" : name) + "\0" + (message == null ? "" : message);
    }

    private static String stripClientPrefix(String name) {
        if (name == null || !name.startsWith("[")) return name == null ? "" : name;
        int end = name.indexOf("] ");
        return end > 0 ? name.substring(end + 2) : name;
    }

    private static String removeDuplicateClientPrefix(String client, String name) {
        if (client == null || client.isBlank() || name == null) return name == null ? "" : name;
        String prefix = "[" + client + "] ";
        String displayPrefix = "[" + displayClientName(client) + "] ";
        if (name.startsWith(prefix)) return name.substring(prefix.length());
        if (name.startsWith(displayPrefix)) return name.substring(displayPrefix.length());
        return name;
    }

    private static String displayClientName(String client) {
        return SERVER_CLIENT_ID.equals(client) ? DISPLAY_CLIENT_ID : client;
    }

    // JSON and string helpers

    private static JsonObject parseObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            LOGGER.debug("Invalid IRC JSON: {}", json);
            return null;
        }
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return "";
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean getBoolean(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return false;
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String extractReason(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonObject object = parseObject(body);
            if (object == null) return body.strip();
            String reason = firstNonEmpty(
                    getString(object, "reason"),
                    getString(object, "message"),
                    getString(object, "error")
            );
            return reason.isEmpty() ? body.strip() : reason;
        } catch (Exception ignored) {
            return body.strip();
        }
    }

    private static String trimForLog(String value) {
        if (value == null) return "";
        String singleLine = value.replace('\n', ' ').replace('\r', ' ').trim();
        return singleLine.length() > 300 ? singleLine.substring(0, 300) + "..." : singleLine;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static long getLong(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return 0L;
        try {
            return element.getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
