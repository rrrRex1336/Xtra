package awa.qwq.ovo.Naven.auth;

import awa.qwq.ovo.Naven.Version;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class VersionChecker {
    private static final Logger LOGGER = LogManager.getLogger("VersionChecker");
    private static final String ENDPOINT = "http://neko.antichest.pw/api/index.php?route=/version";
    private static final int TIMEOUT_MS = 6000;

    private VersionChecker() {
    }

    public static void verifyOrExit() {
        if (isDevelopment()) {
            return;
        }

        try {
            String remote = fetchVersion();
            if (remote.isEmpty()) {
                fail("empty remote version");
            }
            if (!Version.BUILD.equals(remote)) {
                fail("client version mismatch. local=" + Version.BUILD + ", remote=" + remote);
            }
            LOGGER.info("[Version] OK. client={}, version={}", VerifyClient.CLIENT_NAME, Version.BUILD);
        } catch (Exception e) {
            fail("version check failed: " + e.getClass().getSimpleName());
        }
    }

    private static String fetchVersion() throws Exception {
        String url = ENDPOINT + "&client=" + URLEncoder.encode(VerifyClient.CLIENT_NAME, StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int status = conn.getResponseCode();
            InputStream stream = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (status >= 400) {
                throw new IllegalStateException("http=" + status + ", body=" + body);
            }
            return parseVersion(body);
        } finally {
            conn.disconnect();
        }
    }

    private static String parseVersion(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element.isJsonPrimitive()) {
                return normalize(element.getAsString());
            }
            if (element.isJsonObject()) {
                String value = findVersion(element.getAsJsonObject());
                return normalize(value);
            }
        } catch (Exception ignored) {
            return normalize(body);
        }
        return "";
    }

    private static String findVersion(JsonObject object) {
        String[] keys = {
                "version", "latest", "latestVersion", "clientVersion",
                "build", "buildVersion", "required", "requiredVersion"
        };
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonPrimitive()) {
                return value.getAsString();
            }
        }
        for (var entry : object.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                String nested = findVersion(entry.getValue().getAsJsonObject());
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return "";
    }

    private static String normalize(String version) {
        if (version == null) {
            return "";
        }
        String value = version.trim();
        int colon = value.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < value.length()) {
            value = value.substring(colon + 1).trim();
        }
        return value.replace("\"", "").trim();
    }

    private static boolean isDevelopment() {
        try {
            return FabricLoader.getInstance().isDevelopmentEnvironment();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void fail(String reason) {
        LOGGER.error("[Version] {}", reason);
        System.err.println("[Version] " + reason);
        System.exit(1);
    }
}
