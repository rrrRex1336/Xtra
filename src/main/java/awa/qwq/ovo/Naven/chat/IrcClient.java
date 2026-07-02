package awa.qwq.ovo.Naven.chat;

import awa.qwq.ovo.Naven.auth.VerifyClient;
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
import java.util.Set;
import java.util.UUID;

public final class IrcClient {
    private static final Logger LOGGER = LogManager.getLogger("IrcClient");
    private static final String BASE = "http://neko.antichest.pw/api/index.php?route=/chat";
    private static final String CLIENT_ID = VerifyClient.CLIENT_NAME;
    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final int POLL_MS = 3000;
    private static final int TIMEOUT_MS = 6000;
    private static final int MAX_SEEN = 200;

    private static volatile boolean running = false;
    private static volatile long lastTimestamp = 0L;
    private static volatile boolean isAdmin = false;
    private static volatile String assignedName = "";

    private static final List<String> seenIds = new ArrayList<>();
    private static final Map<String, String> ircUsers = Collections.synchronizedMap(new HashMap<>());
    private static final Set<String> pendingEchos = Collections.synchronizedSet(new HashSet<>());

    private IrcClient() {}

    @NekoExclude
    public static void init() {
        if (running) return;
        running = true;
        lastTimestamp = System.currentTimeMillis() / 1000L;
        Thread t = new Thread(IrcClient::pollLoop, CLIENT_ID + "-IRC-Poller");
        t.setDaemon(true);
        t.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                String token = VerifyClient.getToken();
                if (token.isEmpty()) return;
                String url = BASE + "/logout"
                        + "&token=" + enc(token)
                        + "&hwid=" + enc(VerifyClient.getHwid())
                        + "&instance=" + enc(INSTANCE_ID)
                        + "&client=" + enc(CLIENT_ID);
                get(url, 1500);
            } catch (Exception ignored) {}
        }, CLIENT_ID + "-IRC-Logout"));
    }

    public static void stop() {
        running = false;
        ircUsers.clear();
        isAdmin = false;
        assignedName = "";
    }

    @NekoExclude
    public static void send(String message) {
        if (message == null || message.isBlank()) return;
        String token = VerifyClient.getToken();
        if (token.isEmpty()) {
            addChat("§c[IRC] 未登录，无法发送消息");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        String mcName = (mc.player != null) ? mc.player.getGameProfile().getName() : "";
        String displayName = assignedName.isEmpty() ? mcName : assignedName;
        String chatColor = isAdmin ? "§c" : "§b";
        String echoKey = "[" + CLIENT_ID + "] " + displayName + "\0" + message;
        pendingEchos.add(echoKey);
        addChat("§7[§dChat§7] " + chatColor + "[" + CLIENT_ID + "] " + displayName + "§7: §f" + message);

        Thread t = new Thread(() -> {
            try {
                String body = "{"
                        + "\"token\":\"" + esc(token) + "\","
                        + "\"hwid\":\"" + esc(VerifyClient.getHwid()) + "\","
                        + "\"client\":\"" + CLIENT_ID + "\","
                        + "\"name\":\"" + esc(displayName) + "\","
                        + "\"message\":\"" + esc(message) + "\""
                        + "}";
                int code = post(BASE + "/send", body);
                if (code != 201) {
                    addChat("§c[IRC] 发送失败 (HTTP " + code + ")");
                    pendingEchos.remove(echoKey);
                }
            } catch (Exception e) {
                addChat("§c[IRC] 网络错误: " + e.getClass().getSimpleName());
                pendingEchos.remove(echoKey);
            }
        }, CLIENT_ID + "-IRC-Send");
        t.setDaemon(true);
        t.start();
    }

    public static boolean isAdmin() { return isAdmin; }
    public static String displayName() { return assignedName; }
    public static Map<String, String> getOnlineUsers() { return Map.copyOf(ircUsers); }

    // ── poll loop ──────────────────────────────────────────────────────────────

    @ZKMIndy
    @NekoInclude
    private static void pollLoop() {
        while (running) {
            try {
                String token = VerifyClient.getToken();
                if (!token.isEmpty()) {
                    String resp = get(buildPollUrl(token), TIMEOUT_MS);
                    if (resp != null && resp.contains("\"ok\":true")) {
                        isAdmin = resp.contains("\"is_admin\":true");
                        updateAssignedName(resp);
                        parseMessages(resp);
                        updateOnlineUsers(resp);
                    }
                }
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {}
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
        String ircName = assignedName.isEmpty() ? mcName : assignedName;
        return BASE + "/poll"
                + "&token=" + enc(token)
                + "&hwid=" + enc(VerifyClient.getHwid())
                + "&since=" + lastTimestamp
                + "&username=" + enc(mcName)
                + "&ircname=" + enc(ircName)
                + "&instance=" + enc(INSTANCE_ID)
                + "&client=" + enc(CLIENT_ID)
                + "&x=" + px + "&y=" + py + "&z=" + pz
                + "&dim=" + enc(dim);
    }

    @ZKMIndy
    @NekoInclude
    private static void parseMessages(String resp) {
        int arrStart = resp.indexOf("\"messages\":");
        if (arrStart < 0) return;
        String arr = resp.substring(arrStart + 11).trim();
        if (!arr.startsWith("[")) return;

        int depth = 0, start = -1;
        long maxTs = lastTimestamp;

        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String obj  = arr.substring(start, i + 1);
                    String id   = extractStr(obj, "id");
                    String name = extractStr(obj, "name");
                    String msg  = extractStr(obj, "message");
                    String msgClient = extractStr(obj, "client");
                    long ts     = extractLong(obj, "timestamp");
                    boolean admin = obj.contains("\"admin\":true");

                    if (id != null && !seenIds.contains(id) && name != null && msg != null) {
                        seenIds.add(id);
                        if (seenIds.size() > MAX_SEEN) seenIds.remove(0);

                        if (admin) {
                            if (ts > maxTs) maxTs = ts;
                            start = -1;
                            continue;
                        }

                        boolean wasEcho = pendingEchos.remove(name + "\0" + msg);
                        if (!wasEcho) {
                            final String line = "§7[§dChat§7] §b" + name + "§7: §f" + msg;
                            addChat(line);
                        }
                    }
                    if (ts > maxTs) maxTs = ts;
                    start = -1;
                }
            }
        }
        if (maxTs > lastTimestamp) lastTimestamp = maxTs;
    }

    private static void updateAssignedName(String resp) {
        String name = extractStr(resp, "assigned_ircname");
        if (!name.isEmpty()) assignedName = name;
    }

    private static void updateOnlineUsers(String resp) {
        int start = resp.indexOf("\"online\":");
        if (start < 0) return;
        String sub = resp.substring(start + 9).trim();
        if (!sub.startsWith("[")) return;

        Map<String, String> newMap = new HashMap<>();
        int depth = 0, objStart = -1;
        for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c == '{') { if (depth == 0) objStart = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = sub.substring(objStart, i + 1);
                    String mc  = extractStr(obj, "username");
                    String irc = extractStr(obj, "ircname");
                    if (!mc.isEmpty() && !irc.isEmpty()) newMap.put(mc, irc);
                    objStart = -1;
                }
            }
        }
        ircUsers.clear();
        ircUsers.putAll(newMap);
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private static String get(String url, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", CLIENT_ID + "-IRC/1.0");
            int status = conn.getResponseCode();
            InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static int post(String url, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", CLIENT_ID + "-IRC/1.0");
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            return conn.getResponseCode();
        } finally {
            conn.disconnect();
        }
    }

    // ── chat display ───────────────────────────────────────────────────────────

    private static void addChat(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.gui != null) {
                mc.gui.getChat().addMessage(Component.literal(text));
            }
        });
    }

    // ── string helpers ─────────────────────────────────────────────────────────

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int i = json.indexOf(search);
        if (i < 0) return "";
        int start = i + search.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int i = json.indexOf(search);
        if (i < 0) return 0L;
        int start = i + search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0L; }
    }
}
