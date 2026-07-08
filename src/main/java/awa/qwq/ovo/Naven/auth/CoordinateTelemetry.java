package awa.qwq.ovo.Naven.auth;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CoordinateTelemetry {
    private static final Logger LOGGER = LogManager.getLogger("CoordinateTelemetry");
    private static final String ENDPOINT = "http://neko.antichest.pw/api/index.php?route=/coordinate-log";
    private static final int TIMEOUT_MS = 5000;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static volatile boolean enabledAfterLogin;

    private int ticks;
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private double lastZ = Double.NaN;

    private CoordinateTelemetry() {
    }

    public static void enableAfterLogin() {
        enabledAfterLogin = true;
    }

    public static void initialize() {
        if (!enabledAfterLogin || !initialized.compareAndSet(false, true)) {
            return;
        }
        if (Naven.getInstance() == null || Naven.getInstance().getEventManager() == null) {
            initialized.set(false);
            return;
        }
        Naven.getInstance().getEventManager().register(new CoordinateTelemetry());
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.POST) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (++ticks < 200) {
            return;
        }
        ticks = 0;

        Player player = mc.player;
        if (!shouldReport(player)) {
            return;
        }

        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        String token = firstNonEmpty(VerifyClient.getToken(), ClientTelemetry.currentToken());
        if (token.isEmpty()) {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("token", token);
        body.addProperty("hwid", VerifyClient.getHwid());
        body.addProperty("server", serverName(mc));
        String dimension = mc.level.dimension().location().toString();
        body.addProperty("dimension", dimension);
        body.addProperty("dimensionLabel", dimensionLabel(dimension));
        body.addProperty("x", Math.floor(player.getX()));
        body.addProperty("y", Math.floor(player.getY()));
        body.addProperty("z", Math.floor(player.getZ()));
        body.addProperty("client", VerifyClient.CLIENT_NAME);
        body.addProperty("version", "1.0");
        body.addProperty("time", Instant.now().toString());
        body.addProperty("timestamp", Instant.now().getEpochSecond());

        Thread thread = new Thread(() -> post(body), "Naven-CoordinateTelemetry");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean shouldReport(Player player) {
        if (Double.isNaN(lastX)) {
            return true;
        }
        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        return dx * dx + dy * dy + dz * dz >= 64.0D;
    }

    private static String serverName(Minecraft mc) {
        if (mc.hasSingleplayerServer()) {
            return "SinglePlayer";
        }
        ServerData data = mc.getCurrentServer();
        return data == null ? "Unknown" : data.ip;
    }

    private static String dimensionLabel(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "地狱";
            case "minecraft:the_end" -> "末地";
            default -> "未知";
        };
    }

    private static void post(JsonObject body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
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
                LOGGER.warn("[CoordinateTelemetry] Rejected. http={}", status);
            }
        } catch (Exception e) {
            LOGGER.debug("[CoordinateTelemetry] Failed: {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
