package awa.qwq.ovo.Naven.auth;

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
    public  static final String CLIENT_NAME = "LinYiLI";
    private static final int    TIMEOUT_MS  = 8000;

    static final Path KEY_FILE = FabricLoader.getInstance()
            .getGameDir()
            .resolve(CLIENT_NAME)
            .resolve("license.key");

    private static volatile String verifiedToken = "";
    private static volatile String verifiedHwid  = "";
    private static volatile String verifiedOwner = "";
    private static volatile String verifiedRole  = "";
    private static volatile String pendingWebLoginUrl = null;

    @ZKMIndy
    @NekoInclude
    public static boolean verify() {
        String hwid = HWIDCheck.getHWID();

        // 1. Try stored .auth-session
        String token = LinYiLITokenStore.loadToken();

        // 2. No session — try legacy license.key (backward compat, one-time migration)
        if (token.isEmpty() && Files.isRegularFile(KEY_FILE)) {
            try {
                String key = Files.readString(KEY_FILE, StandardCharsets.UTF_8).trim();
                if (!key.isEmpty()) token = key;
            } catch (Exception ignored) {}
        }

        // 3. No token at all — try pending web login or start a new one
        if (token.isEmpty()) {
            return handleNoToken(hwid);
        }

        // 4. Verify token with server
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
                LOGGER.error("[{}] Web login expired. Starting a new one...", CLIENT_NAME);
                return doStartWebLogin(hwid);
            }
            if (result.isEmpty()) {
                // Still waiting
                LOGGER.info("[{}] Web login not yet confirmed. Confirm in browser, then restart the game.", CLIENT_NAME);
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

            boolean allowed = resp.contains("\"allowed\":true");
            boolean ok      = resp.contains("\"ok\":true");

            if (allowed && ok) {
                verifiedToken = token;
                verifiedHwid  = hwid;
                verifiedOwner = extractStr(resp, "owner");
                verifiedRole  = extractStr(resp, "role");
                long expiry   = parseExpiryEpoch(resp);
                LinYiLITokenStore.saveToken(token, expiry);
                LOGGER.info("[{}] Auth OK — owner: {}", CLIENT_NAME, verifiedOwner);
                return true;
            } else {
                String reason = extractStr(resp, "reason");
                LOGGER.error("[{}] Auth denied: {}", CLIENT_NAME, reason.isEmpty() ? resp : reason);
                // Token rejected — clear and start fresh web login
                LinYiLITokenStore.clearToken();
                return doStartWebLogin(hwid);
            }
        } catch (Exception e) {
            LOGGER.error("[{}] Auth error: {}", CLIENT_NAME, e.getMessage());
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

            if (resp.contains("\"ok\":true")) {
                String code = extractStr(resp, "code");
                String url  = extractStr(resp, "url");
                if (!code.isEmpty() && !url.isEmpty()) {
                    LinYiLITokenStore.savePending(code);
                    writeWebLoginFile(url);
                    printWebLoginBanner(url);
                    pendingWebLoginUrl = url;
                    return false;
                }
            }
            LOGGER.error("[{}] Failed to start web login. Check server connectivity.", CLIENT_NAME);
            return false;
        } catch (Exception e) {
            LOGGER.error("[{}] Web login start error: {}", CLIENT_NAME, e.getMessage());
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

            boolean allowed = resp.contains("\"allowed\":true");
            boolean pending = resp.contains("\"pending\":true");

            if (allowed) {
                String token = extractStr(resp, "token");
                verifiedOwner = extractStr(resp, "owner");
                long expiry = parseExpiryEpoch(resp);
                LinYiLITokenStore.saveToken(token, expiry);
                LinYiLITokenStore.clearPending();
                return token;
            }
            if (pending) return "";   // still waiting
            return null;              // expired / rejected
        } catch (Exception e) {
            LOGGER.error("[{}] Web login poll error: {}", CLIENT_NAME, e.getMessage());
            return "";
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @NekoExclude
    private static void printWebLoginBanner(String url) {
        LOGGER.error("[{}] ============================================================", CLIENT_NAME);
        LOGGER.error("[{}]  WEB LOGIN REQUIRED", CLIENT_NAME);
        LOGGER.error("[{}]  Open this URL in your browser:", CLIENT_NAME);
        LOGGER.error("[{}]  {}", CLIENT_NAME, url);
        LOGGER.error("[{}]  After confirming on the website, RESTART the game.", CLIENT_NAME);
        LOGGER.error("[{}]  URL also saved to: {}", CLIENT_NAME,
                KEY_FILE.getParent().resolve("weblogin.txt"));
        LOGGER.error("[{}] ============================================================", CLIENT_NAME);
    }

    @NekoExclude
    private static void writeWebLoginFile(String url) {
        try {
            Path f = KEY_FILE.getParent().resolve("weblogin.txt");
            Files.createDirectories(f.getParent());
            Files.writeString(f,
                    "[LinYiLI] Web Login\n\n"
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
    @NekoExclude public static boolean hasPendingWebLogin() { return pendingWebLoginUrl != null; }
    @NekoExclude public static String getPendingWebLoginUrl() { return pendingWebLoginUrl; }
    @NekoExclude public static void clearPendingWebLoginUrl() { pendingWebLoginUrl = null; }

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

    private static long parseExpiryEpoch(String json) {
        String v = extractStr(json, "expiresAt");
        if (!v.isEmpty()) {
            try { return Long.parseLong(v); } catch (NumberFormatException ignored) {}
        }
        return 0;
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
