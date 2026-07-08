package awa.qwq.ovo.Naven.auth;

import awa.qwq.ovo.Naven.Naven;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hoprc.obf.neko.NekoExclude;
import hoprc.obf.neko.NekoInclude;
import hoprc.obf.zkm.ZKMIndy;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VerifyClient {
    private static final Logger LOGGER = LogManager.getLogger("VerifyClient");
    private static final String API_BASE  = "http://neko.antichest.pw/api/index.php?route=";
    private static final String ENDPOINT  = API_BASE + "/verify";
    private static final String WL_START  = API_BASE + "/web-login/start";
    private static final String WL_POLL   = API_BASE + "/web-login/poll";
    private static final String USER_LOGIN = API_BASE + "/user/login";
    public  static final String CLIENT_NAME = "LinYiLI";
    public  static final String CLIENT_DISPLAY_NAME = Naven.CLIENT_DISPLAY_NAME;
    private static final String STORAGE_FOLDER_NAME = CLIENT_DISPLAY_NAME;
    private static final int    TIMEOUT_MS  = 8000;

    static final Path KEY_FILE = FabricLoader.getInstance()
            .getGameDir()
            .resolve(STORAGE_FOLDER_NAME)
            .resolve("license.key");
    static final Path LEGACY_KEY_FILE = FabricLoader.getInstance()
            .getGameDir()
            .resolve(CLIENT_NAME)
            .resolve("license.key");

    private static volatile String verifiedToken = "";
    private static volatile String verifiedHwid  = "";
    private static volatile String verifiedOwner = "";
    private static volatile String verifiedRole  = "";
    private static volatile String verifiedIrcToken = "";
    private static volatile String verifiedIrcName = "";
    private static volatile String verifiedIrcTokenSource = "";
    private static volatile String verifiedConsoleCookie = "";
    private static volatile String pendingWebLoginUrl = null;

    @ZKMIndy
    @NekoInclude
    public static boolean verify() {
        String hwid = DeviceFingerprint.getHWID();

        // 1. Try the local encrypted, HWID-bound .auth-session.
        String token = LinYiLITokenStore.loadToken();

        // 2. No token at all: try pending web login or start a new one.
        if (token.isEmpty()) {
            return handleNoToken(hwid);
        }

        // 3. Verify token with server.
        return verifyWithServer(token, hwid);
    }

    @ZKMIndy
    @NekoInclude
    private static boolean handleNoToken(String hwid) {
        String pendingCode = LinYiLITokenStore.loadPendingCode();
        if (!pendingCode.isEmpty()) {
            // Poll the pending web login
            String result = pollWebLogin(pendingCode, hwid);
            if (result == null) {
                // Expired or rejected — start fresh
                LinYiLITokenStore.clearPending();
                LOGGER.error("[{}] Web login expired. Starting a new one...", CLIENT_DISPLAY_NAME);
                return doStartWebLogin(hwid);
            }
            if (result.isEmpty()) {
                // Still waiting
                LOGGER.info("[{}] Web login not yet confirmed. Confirm in browser, then restart the game.", CLIENT_DISPLAY_NAME);
                return false;
            }
            // Got token from web login — now verify it
            return verifyWithServer(result, hwid);
        }
        return doStartWebLogin(hwid);
    }

    @ZKMIndy
    @NekoInclude
    private static boolean verifyWithServer(String token, String hwid) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", CLIENT_NAME + "-Auth/1.0");

            String body = "{"
                    + "\"token\":\"" + esc(token) + "\","
                    + "\"hwid\":\"" + esc(hwid) + "\","
                    + "\"client\":\"" + CLIENT_NAME + "\","
                    + "\"version\":\"1.0\""
                    + "}";
            writeBody(conn, body);

            int status = conn.getResponseCode();
            String resp = readResponse(conn, status);
            JsonObject root = parseObject(resp);

            boolean allowed = getBoolean(root, "allowed");
            boolean ok      = getBoolean(root, "ok");

            if (allowed && ok) {
                applyVerifiedAuth(token, hwid, root);
                long expiry   = parseExpiryEpoch(root);
                LinYiLITokenStore.saveToken(token, expiry);
                refreshConsoleSession(false);
                LOGGER.info("[{}] Auth OK — owner: {}, ircName: {}, ircTokenSource: {}",
                        CLIENT_DISPLAY_NAME, verifiedOwner, verifiedIrcName, verifiedIrcTokenSource);
                return true;
            } else {
                String reason = firstNonEmpty(getString(root, "reason"), getString(root, "error"));
                LOGGER.error("[{}] Auth denied: {}", CLIENT_DISPLAY_NAME, reason.isEmpty() ? resp : reason);
                // Token rejected — clear and start fresh web login
                LinYiLITokenStore.clearToken();
                return doStartWebLogin(hwid);
            }
        } catch (Exception e) {
            LOGGER.error("[{}] Auth error: {}", CLIENT_DISPLAY_NAME, e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @ZKMIndy
    @NekoInclude
    private static boolean doStartWebLogin(String hwid) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(WL_START).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", CLIENT_NAME + "-Auth/1.0");

            String body = "{"
                    + "\"hwid\":\"" + esc(hwid) + "\","
                    + "\"username\":\"\","
                    + "\"client\":\"" + CLIENT_NAME + "\","
                    + "\"version\":\"1.0\""
                    + "}";
            writeBody(conn, body);

            int status = conn.getResponseCode();
            String resp = readResponse(conn, status);
            JsonObject root = parseObject(resp);

            if (getBoolean(root, "ok")) {
                String code = getString(root, "code");
                String url  = getString(root, "url");
                if (!code.isEmpty() && !url.isEmpty()) {
                    LinYiLITokenStore.savePending(code);
                    writeWebLoginFile(url);
                    printWebLoginBanner(url);
                    pendingWebLoginUrl = url;
                    return false;
                }
            }
            LOGGER.error("[{}] Failed to start web login. Check server connectivity.", CLIENT_DISPLAY_NAME);
            return false;
        } catch (Exception e) {
            LOGGER.error("[{}] Web login start error: {}", CLIENT_DISPLAY_NAME, e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Returns: null=expired/failed, ""=pending, token string=success
    @ZKMIndy
    @NekoInclude
    private static String pollWebLogin(String code, String hwid) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(WL_POLL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", CLIENT_NAME + "-Auth/1.0");

            String body = "{"
                    + "\"code\":\"" + esc(code) + "\","
                    + "\"hwid\":\"" + esc(hwid) + "\","
                    + "\"client\":\"" + CLIENT_NAME + "\","
                    + "\"version\":\"1.0\""
                    + "}";
            writeBody(conn, body);

            int status = conn.getResponseCode();
            String resp = readResponse(conn, status);
            JsonObject root = parseObject(resp);

            boolean allowed = getBoolean(root, "allowed");
            boolean pending = getBoolean(root, "pending");

            if (allowed) {
                String token = getString(root, "token");
                if (token.isEmpty()) {
                    token = findString(root, "auth_token", "authToken", "license", "licenseToken");
                }
                if (token.isEmpty()) {
                    LOGGER.error("[{}] Web login poll succeeded without an auth token. responseKeys={}",
                            CLIENT_DISPLAY_NAME, summarizeKeys(root));
                    return null;
                }
                applyVerifiedAuth(token, hwid, root);
                long expiry = parseExpiryEpoch(root);
                LinYiLITokenStore.saveToken(token, expiry);
                LinYiLITokenStore.clearPending();
                refreshConsoleSession(false);
                return token;
            }
            if (pending) return "";   // still waiting
            return null;              // expired / rejected
        } catch (Exception e) {
            LOGGER.error("[{}] Web login poll error: {}", CLIENT_DISPLAY_NAME, e.getMessage());
            return "";
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @NekoExclude
    private static void printWebLoginBanner(String url) {
        LOGGER.error("[{}] ============================================================", CLIENT_DISPLAY_NAME);
        LOGGER.error("[{}]  WEB LOGIN REQUIRED", CLIENT_DISPLAY_NAME);
        LOGGER.error("[{}]  Open this URL in your browser:", CLIENT_DISPLAY_NAME);
        LOGGER.error("[{}]  {}", CLIENT_DISPLAY_NAME, url);
        LOGGER.error("[{}]  After confirming on the website, RESTART the game.", CLIENT_DISPLAY_NAME);
        LOGGER.error("[{}]  URL also saved to: {}", CLIENT_DISPLAY_NAME,
                KEY_FILE.getParent().resolve("weblogin.txt"));
        LOGGER.error("[{}] ============================================================", CLIENT_DISPLAY_NAME);
    }

    @NekoExclude
    private static void writeWebLoginFile(String url) {
        try {
            Path f = KEY_FILE.getParent().resolve("weblogin.txt");
            Files.createDirectories(f.getParent());
            Files.writeString(f,
                    "[" + CLIENT_DISPLAY_NAME + "] Web Login\n\n"
                    + "Please open this URL in your browser:\n"
                    + url + "\n\n"
                    + "After confirming on the website, restart the game.\n",
                    StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    @NekoExclude public static String getToken()    { return verifiedToken; }
    @NekoExclude public static String getHwid()     { return verifiedHwid; }
    @NekoExclude public static String getOwner()    { return verifiedOwner; }
    @NekoExclude public static String getUserName() { return verifiedOwner; }
    @NekoExclude public static String getUserRole() { return verifiedRole; }
    @NekoExclude public static String getIrcToken() { return verifiedIrcToken.isEmpty() ? verifiedToken : verifiedIrcToken; }
    @NekoExclude public static String getIrcName()  { return verifiedIrcName; }
    @NekoExclude public static String getIrcTokenSource() { return verifiedIrcTokenSource; }
    @NekoExclude public static String getConsoleCookie() { return verifiedConsoleCookie; }
    @NekoExclude public static boolean hasConsoleSession() { return !verifiedConsoleCookie.isEmpty(); }
    @NekoExclude public static boolean ensureConsoleSession() { return refreshConsoleSession(false); }
    @NekoExclude public static boolean refreshIrcSession() { return refreshConsoleSession(true); }
    @NekoExclude public static boolean hasPendingWebLogin() { return pendingWebLoginUrl != null; }
    @NekoExclude public static String getPendingWebLoginUrl() { return pendingWebLoginUrl; }
    @NekoExclude public static void clearPendingWebLoginUrl() { pendingWebLoginUrl = null; }
    @NekoExclude static boolean isLegacyStorageEnabled() { return !STORAGE_FOLDER_NAME.equals(CLIENT_NAME); }

    // ── helpers ───────────────────────────────────────────────────────────────

    @NekoExclude
    private static void writeBody(HttpURLConnection conn, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
    }

    private static String readResponse(HttpURLConnection conn, int status) {
        try {
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return "";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static void applyVerifiedAuth(String authToken, String hwid, JsonObject root) {
        verifiedToken = authToken;
        verifiedHwid  = hwid;
        verifiedOwner = firstNonEmpty(
                getString(root, "owner"),
                findString(root, "owner"),
                findString(root, "username"),
                findString(root, "name")
        );
        boolean admin = getBoolean(root, "admin") || getBoolean(findObject(root, "token"), "admin");
        verifiedRole = firstNonEmpty(
                getString(root, "role"),
                findString(root, "role"),
                admin ? "Admin" : ""
        );
        verifiedIrcName = firstNonEmpty(
                getString(root, "ircname"),
                getString(root, "irc_name"),
                getString(root, "note"),
                findString(root, "ircname", "irc_name", "ircName", "note", "remark"),
                verifiedOwner
        );
        ResolvedToken ircToken = resolveIrcToken(root, authToken);
        verifiedIrcToken = ircToken.token();
        verifiedIrcTokenSource = ircToken.source();
    }

    private static long parseExpiryEpoch(JsonObject root) {
        String v = firstNonEmpty(
                getString(root, "expiresAt"),
                findString(root, "expiresAtEpoch", "expiryEpoch", "expireEpoch", "expires")
        );
        if (!v.isEmpty()) {
            try { return Long.parseLong(v); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static ResolvedToken resolveIrcToken(JsonObject root, String fallback) {
        String explicit = findString(root,
                "irc_token", "ircToken",
                "chat_token", "chatToken",
                "irc_session", "ircSession",
                "chat_session", "chatSession"
        );
        if (!explicit.isEmpty()) {
            return new ResolvedToken(explicit, "irc/chat response field");
        }

        JsonObject chat = findObject(root, "irc", "chat");
        String nested = findString(chat, "token", "key", "session", "session_token", "sessionToken");
        if (!nested.isEmpty()) {
            return new ResolvedToken(nested, "nested irc/chat object");
        }

        return new ResolvedToken(fallback, "auth token fallback");
    }

    private record ResolvedToken(String token, String source) {}

    @NekoExclude
    private static synchronized boolean refreshConsoleSession(boolean force) {
        if (!force && !verifiedConsoleCookie.isEmpty()) {
            return true;
        }
        if (verifiedToken.isEmpty()) {
            return false;
        }
        if (force) {
            verifiedConsoleCookie = "";
        }

        String owner = firstNonEmpty(verifiedOwner, verifiedIrcName);
        if (owner.isEmpty()) {
            LOGGER.warn("[{}] Console login skipped: verified owner is empty.", CLIENT_DISPLAY_NAME);
            return false;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(USER_LOGIN).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", CLIENT_NAME + "-Auth/1.0");

            String body = "{"
                    + "\"owner\":\"" + esc(owner) + "\","
                    + "\"token\":\"" + esc(verifiedToken) + "\","
                    + "\"client\":\"" + CLIENT_NAME + "\""
                    + "}";
            writeBody(conn, body);

            int status = conn.getResponseCode();
            String resp = readResponse(conn, status);
            JsonObject root = parseObject(resp);
            if (status >= 200 && status < 300 && getBoolean(root, "ok")) {
                verifiedConsoleCookie = normalizeCookie(conn.getHeaderField("Set-Cookie"));
                verifiedOwner = firstNonEmpty(getString(root, "owner"), verifiedOwner);
                verifiedIrcName = firstNonEmpty(getString(root, "ircname"), getString(root, "note"), verifiedIrcName, verifiedOwner);
                if (getBoolean(root, "admin") && verifiedRole.isEmpty()) {
                    verifiedRole = "Admin";
                }

                // Only update the IRC token from explicit IRC-named fields.
                // "tokenValue" is a display-only license key for the web console UI
                // and must NOT be used as an IRC token — it may be in a different
                // format and will be rejected by /chat/poll and /chat/send.
                String consoleIrcToken = firstNonEmpty(
                        getString(root, "irc_token"),
                        getString(root, "ircToken"),
                        getString(root, "chat_token"),
                        getString(root, "chatToken"),
                        getString(root, "irc_session"),
                        getString(root, "ircSession")
                );
                if (!consoleIrcToken.isEmpty()) {
                    verifiedIrcToken = consoleIrcToken;
                    verifiedIrcTokenSource = "console ircToken";
                }

                LOGGER.info("[{}] Console session OK. owner={}, cookiePresent={}",
                        CLIENT_DISPLAY_NAME, verifiedOwner, !verifiedConsoleCookie.isEmpty());
                return !verifiedConsoleCookie.isEmpty();
            }

            String reason = firstNonEmpty(getString(root, "reason"), getString(root, "error"), resp);
            LOGGER.warn("[{}] Console login failed. http={}, reason={}", CLIENT_DISPLAY_NAME, status, reason);
        } catch (Exception e) {
            LOGGER.warn("[{}] Console login error: {}", CLIENT_DISPLAY_NAME, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private static String normalizeCookie(String setCookie) {
        if (setCookie == null || setCookie.isBlank()) {
            return "";
        }
        int end = setCookie.indexOf(';');
        return (end > 0 ? setCookie.substring(0, end) : setCookie).trim();
    }

    private static JsonObject parseObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject findObject(JsonElement element, String... keys) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        for (String key : keys) {
            JsonElement child = object.get(key);
            if (child != null && child.isJsonObject()) {
                return child.getAsJsonObject();
            }
        }
        return null;
    }

    private static String findString(JsonElement element, String... keys) {
        if (element == null || keys == null || keys.length == 0) return "";
        String direct = findStringRecursive(element, keys, false);
        return direct.isEmpty() ? findStringRecursive(element, keys, true) : direct;
    }

    private static String findStringRecursive(JsonElement element, String[] keys, boolean recursive) {
        if (element == null || !element.isJsonObject()) return "";
        JsonObject object = element.getAsJsonObject();
        for (String key : keys) {
            String value = primitiveString(object.get(key));
            if (!value.isEmpty()) return value;
        }
        for (String key : object.keySet()) {
            for (String wanted : keys) {
                if (normalizeKey(key).equals(normalizeKey(wanted))) {
                    String value = primitiveString(object.get(key));
                    if (!value.isEmpty()) return value;
                }
            }
        }
        if (!recursive) return "";
        for (var entry : object.entrySet()) {
            JsonElement child = entry.getValue();
            String value = findStringRecursive(child, keys, true);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static boolean getBoolean(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        if (element == null || !element.isJsonPrimitive()) return false;
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        return primitiveString(element);
    }

    private static String primitiveString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return "";
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.replace("_", "").replace("-", "").replace(".", "").toLowerCase();
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

    private static String summarizeKeys(JsonObject object) {
        if (object == null) return "[]";
        StringBuilder builder = new StringBuilder("[");
        int index = 0;
        for (String key : object.keySet()) {
            if (index++ > 0) builder.append(", ");
            builder.append(key);
        }
        return builder.append(']').toString();
    }

    static String extractStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int i = json.indexOf(search);
        if (i < 0) return "";
        int start = i + search.length();
        int end   = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
