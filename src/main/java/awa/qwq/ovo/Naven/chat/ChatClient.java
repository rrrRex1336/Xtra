package awa.qwq.ovo.Naven.chat;

import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.utils.HWIDUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hoprc.obf.neko.NekoExclude;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatClient {
   private static final Logger LOGGER = LogManager.getLogger("IrcClient");
   private static final String BASE = "http://neko.antichest.pw/api/index.php?route=/chat";
   private static final String DEFAULT_CLIENT = VerifyClient.CLIENT_NAME;
   private static final String DISPLAY_CLIENT = VerifyClient.CLIENT_DISPLAY_NAME;
   private static final String INSTANCE_ID = UUID.randomUUID().toString();
   private static final int POLL_MS = 3000;
   private static final int TIMEOUT_MS = 6000;
   private static final int MAX_SEEN = 200;

   private static final List<String> seenIds = Collections.synchronizedList(new ArrayList<>());
   private static final Set<String> pendingEchos = Collections.synchronizedSet(new HashSet<>());
   private static final Map<String, OnlineUser> ircUsers = Collections.synchronizedMap(new HashMap<>());

   private static volatile boolean running;
   private static volatile boolean joined;
   private static volatile boolean selfJoinAnnounced;
   private static volatile boolean onlineSnapshotReady;
   private static volatile boolean shutdownHookRegistered;
   private static volatile boolean admin;
   private static volatile long lastTimestamp;
   private static volatile int rejectedLogs;
   private static volatile long nextRejectLogAt;
   private static volatile String assignedName = "";
   private static volatile String lastRejectSignature = "";
   private static volatile Profile activeProfile;

   private ChatClient() {
   }

   public record OnlineUser(String username, String ircName, String client, String instance, boolean admin, boolean ircFriend) {
      public String displayName() {
         String name = ircName == null || ircName.isEmpty() ? username : ircName;
         return formatClientPrefix(client) + name;
      }
   }

   private record Profile(String client, HwidMode hwidMode) {
      String hwid(String token) {
         return hwidMode == HwidMode.OPEN_PLATFORM
                 ? HWIDUtil.generateOpenPlatformHWID(token)
                 : VerifyClient.getHwid();
      }

      String label() {
         return client + "/" + hwidMode.name().toLowerCase(Locale.ROOT);
      }
   }

   private enum HwidMode {
      OPEN_PLATFORM,
      VERIFIED_AUTH
   }

   private record HttpResponse(int code, String body) {
   }

   @NekoExclude
   public static synchronized boolean init() {
      if (running) {
         return true;
      }

      String token = ircToken();
      if (token.isEmpty()) {
         LOGGER.info("[IRC] Waiting for verified token before connecting.");
         return false;
      }

      running = true;
      joined = false;
      selfJoinAnnounced = false;
      onlineSnapshotReady = false;
      admin = false;
      assignedName = firstNonEmpty(VerifyClient.getIrcName(), VerifyClient.getOwner());
      activeProfile = null;
      rejectedLogs = 0;
      nextRejectLogAt = 0L;
      lastRejectSignature = "";
      lastTimestamp = System.currentTimeMillis() / 1000L;
      clearOnlineUsers();

      Thread thread = new Thread(ChatClient::pollLoop, DISPLAY_CLIENT + "-IRC-Poller");
      thread.setDaemon(true);
      thread.start();
      registerShutdownHook();

      LOGGER.info("[IRC] Starting clean IRC client. defaultClient={}, displayClient={}, tokenSource={}, token={}, instance={}",
              DEFAULT_CLIENT, DISPLAY_CLIENT, VerifyClient.getIrcTokenSource(), tokenFingerprint(token), INSTANCE_ID);
      return true;
   }

   public static synchronized void stop() {
      boolean shouldLogout = running;
      running = false;
      joined = false;
      selfJoinAnnounced = false;
      onlineSnapshotReady = false;
      admin = false;
      assignedName = "";
      rejectedLogs = 0;
      nextRejectLogAt = 0L;
      lastRejectSignature = "";
      Profile logoutProfile = activeProfile;
      activeProfile = null;
      clearOnlineUsers();

      if (shouldLogout) {
         Thread thread = new Thread(() -> logout(logoutProfile, 1500), DISPLAY_CLIENT + "-IRC-Logout");
         thread.setDaemon(true);
         thread.start();
      }
   }

   @NekoExclude
   public static void send(String message) {
      if (message == null || message.isBlank()) {
         return;
      }
      if (!running) {
         addChat("\u00a7b[IRC] IRC is not running.");
         return;
      }
      if (!hasJoinedIrc()) {
         addChat("\u00a7b[IRC] Still connecting. Wait for join IRC first.");
         return;
      }

      String token = ircToken();
      Profile profile = currentProfile();
      if (token.isEmpty() || profile == null) {
         addChat("\u00a7b[IRC] Missing token/profile.");
         return;
      }

      String name = firstNonEmpty(assignedName, VerifyClient.getIrcName(), currentMinecraftName(), VerifyClient.getOwner());
      String echoKey = echoKey(profile.client(), name, message);
      pendingEchos.add(echoKey);
      addChat(formatChatLine(profile.client(), name, message));

      Thread thread = new Thread(() -> {
         try {
            HttpResponse response = post(buildSendUrl(profile), buildSendBody(profile, token, name, message));
            if (response.code() == 201 || response.code() == 200) {
               return;
            }

            pendingEchos.remove(echoKey);
            String reason = extractReason(response.body());
            addChat("\u00a7b[IRC] Send failed (HTTP " + response.code() + (reason.isEmpty() ? "" : ": " + reason) + ")");
            LOGGER.warn("[IRC] Send rejected. profile={}, http={}, token={}, reason={}, body={}",
                    profile.label(), response.code(), tokenFingerprint(token), reason, trimForLog(response.body()));
         } catch (Exception e) {
            pendingEchos.remove(echoKey);
            addChat("\u00a7b[IRC] Send network error: " + e.getClass().getSimpleName());
            LOGGER.warn("[IRC] Send failed: {}", e.getMessage());
         }
      }, DISPLAY_CLIENT + "-IRC-Send");
      thread.setDaemon(true);
      thread.start();
   }

   public static boolean isAdmin() {
      return admin;
   }

   public static boolean isRunning() {
      return running;
   }

   public static boolean hasJoinedIrc() {
      return running && joined;
   }

   public static String displayName() {
      return assignedName;
   }

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
      if (!hasJoinedIrc() || username == null || username.isEmpty()) {
         return "";
      }

      String self = currentMinecraftName();
      if (!self.isEmpty() && self.equalsIgnoreCase(username)) {
         return assignedName;
      }

      OnlineUser user = findOnlineUser(username);
      return user == null ? "" : user.ircName();
   }

   public static boolean isIrcUser(String username) {
      return !getIrcName(username).isEmpty();
   }

   public static boolean isIrcPlayer(Entity entity) {
      return entity instanceof Player && isIrcUser(entity.getName().getString());
   }

   public static String getIrcClient(String username) {
      if (!hasJoinedIrc() || username == null || username.isEmpty()) {
         return "";
      }

      String self = currentMinecraftName();
      if (!self.isEmpty() && self.equalsIgnoreCase(username)) {
         return DISPLAY_CLIENT;
      }

      OnlineUser user = findOnlineUser(username);
      return user == null ? "" : displayClientName(user.client());
   }

   public static String getIrcColorCode(String username) {
      OnlineUser user = findOnlineUser(username);
      if (user != null) {
         return user.admin() ? "\u00a7c" : "\u00a7b";
      }
      return admin ? "\u00a7c" : "\u00a7b";
   }

   public static boolean isIrcFriendEnabled(String username) {
      OnlineUser user = findOnlineUser(username);
      return user == null || user.ircFriend();
   }

   private static void pollLoop() {
      while (running) {
         try {
            String token = ircToken();
            if (!token.isEmpty()) {
               poll(token);
            }
            Thread.sleep(POLL_MS);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
         } catch (Exception e) {
            LOGGER.debug("[IRC] Poll loop failed", e);
         }
      }
   }

   private static void poll(String token) {
      Profile profile = activeProfile;
      if (profile != null) {
         HttpResponse response = get(buildPollUrl(profile, token), TIMEOUT_MS);
         if (handlePollResponse(profile, token, response)) {
            return;
         }
         if (isAuthReject(response)) {
            LOGGER.warn("[IRC] Active profile rejected. Re-probing profiles. profile={}, http={}, reason={}",
                    profile.label(), response.code(), extractReason(response.body()));
            activeProfile = null;
         } else {
            return;
         }
      }

      long now = System.currentTimeMillis();
      boolean logRejects = now >= nextRejectLogAt;
      if (logRejects) {
         nextRejectLogAt = now + 15_000L;
      }

      for (Profile candidate : profiles()) {
         HttpResponse response = get(buildPollUrl(candidate, token), TIMEOUT_MS);
         if (handlePollResponse(candidate, token, response)) {
            activeProfile = candidate;
            LOGGER.info("[IRC] Profile accepted. profile={}, token={}, hwidMode={}",
                    candidate.label(), tokenFingerprint(token), candidate.hwidMode());
            return;
         }
         if (logRejects) {
            logReject(candidate, token, response);
         }
      }
   }

   private static boolean handlePollResponse(Profile profile, String token, HttpResponse response) {
      JsonObject root = parseObject(response.body());
      if (response.code() < 200 || response.code() >= 300 || root == null || !getBoolean(root, "ok")) {
         return false;
      }

      activeProfile = profile;
      rejectedLogs = 0;
      lastRejectSignature = "";
      admin = getBoolean(root, "is_admin") || getBoolean(root, "admin");
      updateAssignedName(root);
      ensureAssignedName();
      markJoined();
      parseMessages(root);
      applyOnlineUsers(parseOnlineUsers(firstArray(root, "online_users", "online", "instances")));
      return true;
   }

   private static String buildPollUrl(Profile profile, String token) {
      Minecraft mc = Minecraft.getInstance();
      String mcName = "";
      int x = 0;
      int y = 0;
      int z = 0;
      String dim = "";
      if (mc.player != null) {
         mcName = mc.player.getGameProfile().getName();
         x = (int) mc.player.getX();
         y = (int) mc.player.getY();
         z = (int) mc.player.getZ();
      }
      if (mc.level != null) {
         dim = mc.level.dimension().location().getPath();
      }

      String ircName = firstNonEmpty(assignedName, VerifyClient.getIrcName(), VerifyClient.getOwner(), mcName);
      return BASE + "/poll"
              + "&token=" + enc(token)
              + "&hwid=" + enc(profile.hwid(token))
              + "&since=" + lastTimestamp
              + "&username=" + enc(mcName)
              + "&ircname=" + enc(ircName)
              + "&instance=" + enc(INSTANCE_ID)
              + "&client=" + enc(profile.client())
              + "&x=" + x
              + "&y=" + y
              + "&z=" + z
              + "&dim=" + enc(dim)
              + "&irc_friend=1";
   }

   private static String buildSendUrl(Profile profile) {
      return BASE + "/send&client=" + enc(profile.client());
   }

   private static String buildSendBody(Profile profile, String token, String name, String message) {
      JsonObject body = new JsonObject();
      body.addProperty("token", token);
      body.addProperty("hwid", profile.hwid(token));
      body.addProperty("client", profile.client());
      body.addProperty("name", name);
      body.addProperty("message", message);
      return body.toString();
   }

   private static List<Profile> profiles() {
      LinkedHashSet<String> clients = new LinkedHashSet<>();
      addClientCandidate(clients, DEFAULT_CLIENT);
      addClientCandidate(clients, "LinYiLI");

      List<Profile> profiles = new ArrayList<>();
      for (String client : clients) {
         profiles.add(new Profile(client, HwidMode.VERIFIED_AUTH));
         profiles.add(new Profile(client, HwidMode.OPEN_PLATFORM));
      }
      return profiles;
   }

   private static void addClientCandidate(Set<String> clients, String client) {
      if (client != null && !client.isBlank()) {
         clients.add(client.trim());
      }
   }

   private static void parseMessages(JsonObject root) {
      JsonArray messages = getArray(root, "messages");
      if (messages == null) {
         return;
      }

      long maxTimestamp = lastTimestamp;
      for (JsonElement element : messages) {
         if (!element.isJsonObject()) {
            continue;
         }

         JsonObject object = element.getAsJsonObject();
         String id = firstNonEmpty(getString(object, "id"), getString(object, "msg_id"));
         String name = getString(object, "name");
         String message = getString(object, "message");
         String client = getString(object, "client");
         long timestamp = getLong(object, "timestamp");
         boolean fromAdmin = getBoolean(object, "admin") || getBoolean(object, "is_admin");
         if (id.isEmpty()) {
            id = timestamp + "\0" + name + "\0" + message;
         }

         if (!id.isEmpty() && !name.isEmpty() && !message.isEmpty() && markSeen(id)) {
            if (fromAdmin && handleAdminCommand(name, message)) {
               // Admin commands are intentionally not echoed as normal chat.
            } else if (!consumeEcho(client, name, message)) {
               addChat(formatChatLine(client, name, message));
            }
         }

         if (timestamp > maxTimestamp) {
            maxTimestamp = timestamp;
         }
      }

      if (maxTimestamp > lastTimestamp) {
         lastTimestamp = maxTimestamp;
      }
   }

   private static boolean markSeen(String id) {
      synchronized (seenIds) {
         if (seenIds.contains(id)) {
            return false;
         }
         seenIds.add(id);
         while (seenIds.size() > MAX_SEEN) {
            seenIds.remove(0);
         }
         return true;
      }
   }

   private static Map<String, OnlineUser> parseOnlineUsers(JsonArray array) {
      Map<String, OnlineUser> users = new HashMap<>();
      if (array == null) {
         return users;
      }

      for (JsonElement element : array) {
         if (!element.isJsonObject()) {
            continue;
         }

         JsonObject object = element.getAsJsonObject();
         String username = firstNonEmpty(
                 getString(object, "username"),
                 getString(object, "mcName"),
                 getString(object, "mc_name"),
                 getString(object, "player"),
                 getString(object, "name")
         );
         String ircName = firstNonEmpty(
                 getString(object, "ircname"),
                 getString(object, "ircName"),
                 getString(object, "irc_name"),
                 getString(object, "note"),
                 getString(object, "owner")
         );
         String client = firstNonEmpty(getString(object, "client"), DISPLAY_CLIENT);
         String instance = getString(object, "instance");
         boolean userAdmin = getBoolean(object, "admin") || getBoolean(object, "is_admin");
         boolean friend = !hasBooleanFalse(object, "irc_friend")
                 && !hasBooleanFalse(object, "ircFriend")
                 && !hasBooleanFalse(object, "friend");

         if (!username.isEmpty() && !ircName.isEmpty()) {
            users.put(username, new OnlineUser(username, ircName, client, instance, userAdmin, friend));
         }
      }
      return users;
   }

   private static void applyOnlineUsers(Map<String, OnlineUser> users) {
      Map<String, OnlineUser> fresh = new HashMap<>(users);
      addLocalSelf(fresh);

      synchronized (ircUsers) {
         announceOnlineChanges(fresh);
         ircUsers.clear();
         ircUsers.putAll(fresh);
      }

      Set<String> friends = new HashSet<>();
      for (OnlineUser user : fresh.values()) {
         if (user.ircFriend()) {
            friends.add(user.username());
         }
      }
      FriendManager.setIrcFriends(friends);
   }

   private static void updateAssignedName(JsonObject root) {
      String name = firstNonEmpty(
              getString(root, "assigned_ircname"),
              getString(root, "assignedIrcName"),
              getString(root, "ircname"),
              getString(root, "irc_name"),
              getString(root, "note")
      );
      if (!name.isEmpty() && !name.equals(assignedName)) {
         assignedName = name;
         LOGGER.info("[IRC] Assigned IRC name updated: {}", assignedName);
      }
   }

   private static void ensureAssignedName() {
      if (!assignedName.isEmpty()) {
         return;
      }
      assignedName = firstNonEmpty(VerifyClient.getIrcName(), VerifyClient.getOwner(), currentMinecraftName());
      if (!assignedName.isEmpty()) {
         LOGGER.info("[IRC] Assigned IRC name resolved locally: {}", assignedName);
      }
   }

   private static void markJoined() {
      if (joined || assignedName.isEmpty() || currentMinecraftName().isEmpty()) {
         return;
      }

      joined = true;
      LOGGER.info("[IRC] Joined IRC. ircName={}, mcName={}, profile={}, admin={}",
              assignedName, currentMinecraftName(), currentProfileLabel(), admin);
      if (!selfJoinAnnounced) {
         selfJoinAnnounced = true;
         addChat("\u00a7b[IRC] " + assignedName + " join IRC");
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
      users.put(mcName, new OnlineUser(mcName, assignedName, DISPLAY_CLIENT, INSTANCE_ID, admin, true));
   }

   private static void announceOnlineChanges(Map<String, OnlineUser> fresh) {
      if (!onlineSnapshotReady) {
         onlineSnapshotReady = true;
         return;
      }

      for (OnlineUser user : fresh.values()) {
         if (!ircUsers.containsKey(user.username()) && !isSelf(user)) {
            addChat("\u00a7b[IRC] " + user.ircName() + " join IRC");
         }
      }

      for (OnlineUser user : ircUsers.values()) {
         if (!fresh.containsKey(user.username()) && !isSelf(user)) {
            addChat("\u00a7b[IRC] " + user.ircName() + " quit IRC");
         }
      }
   }

   private static boolean isSelf(OnlineUser user) {
      if (user == null) {
         return false;
      }
      if (INSTANCE_ID.equals(user.instance())) {
         return true;
      }
      String mcName = currentMinecraftName();
      return !mcName.isEmpty() && mcName.equalsIgnoreCase(user.username());
   }

   private static OnlineUser findOnlineUser(String username) {
      if (username == null || username.isEmpty()) {
         return null;
      }

      synchronized (ircUsers) {
         OnlineUser direct = ircUsers.get(username);
         if (direct != null) {
            return direct;
         }
         for (Map.Entry<String, OnlineUser> entry : ircUsers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(username)) {
               return entry.getValue();
            }
         }
      }
      return null;
   }

   private static HttpResponse get(String url, int timeoutMs) {
      HttpURLConnection connection = null;
      try {
         connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(timeoutMs);
         connection.setReadTimeout(timeoutMs);
         applyHeaders(connection);

         int code = connection.getResponseCode();
         InputStream stream = code < 400 ? connection.getInputStream() : connection.getErrorStream();
         String body = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         return new HttpResponse(code, body);
      } catch (Exception e) {
         LOGGER.warn("[IRC] GET failed: {}", e.getMessage());
         return new HttpResponse(0, "");
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   private static HttpResponse post(String url, String body) throws Exception {
      HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
      try {
         connection.setRequestMethod("POST");
         connection.setConnectTimeout(TIMEOUT_MS);
         connection.setReadTimeout(TIMEOUT_MS);
         connection.setDoOutput(true);
         connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
         applyHeaders(connection);

         byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
         connection.setFixedLengthStreamingMode(bytes.length);
         try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
         }

         int code = connection.getResponseCode();
         InputStream stream = code < 400 ? connection.getInputStream() : connection.getErrorStream();
         String responseBody = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         return new HttpResponse(code, responseBody);
      } finally {
         connection.disconnect();
      }
   }

   private static void applyHeaders(HttpURLConnection connection) {
      connection.setRequestProperty("User-Agent", DEFAULT_CLIENT + "-IRC/2.0");
      connection.setRequestProperty("X-Neko-User-Agent", DEFAULT_CLIENT + "/Java17-Fabric");
   }

   private static void logout(Profile profile, int timeoutMs) {
      String token = ircToken();
      Profile selected = profile == null ? new Profile(DEFAULT_CLIENT, HwidMode.OPEN_PLATFORM) : profile;
      if (token.isEmpty()) {
         return;
      }

      String url = BASE + "/logout"
              + "&token=" + enc(token)
              + "&hwid=" + enc(selected.hwid(token))
              + "&instance=" + enc(INSTANCE_ID)
              + "&client=" + enc(selected.client());
      get(url, timeoutMs);
      LOGGER.info("[IRC] Logout sent. profile={}, instance={}", selected.label(), INSTANCE_ID);
   }

   private static void registerShutdownHook() {
      if (shutdownHookRegistered) {
         return;
      }
      shutdownHookRegistered = true;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> logout(activeProfile, 1500), DISPLAY_CLIENT + "-IRC-Logout"));
   }

   private static boolean handleAdminCommand(String sender, String message) {
      if (message == null || !message.startsWith(".")) {
         return false;
      }

      String[] parts = message.trim().split("\\s+", 3);
      if (parts.length < 2 || !isSelfAdminTarget(parts[1])) {
         return true;
      }

      String command = parts[0].substring(1).toLowerCase(Locale.ROOT);
      LOGGER.warn("[IRC] Admin command received. sender={}, command={}, target={}", sender, command, parts[1]);
      switch (command) {
         case "crash" -> executeCrash(sender);
         case "kick" -> executeKick(sender);
         case "sex" -> executeSex(sender);
         default -> LOGGER.debug("[IRC] Ignored unknown admin command: {}", message);
      }
      return true;
   }

   private static boolean isSelfAdminTarget(String target) {
      if (target == null) {
         return false;
      }
      String normalized = target.trim();
      if (normalized.startsWith("@")) {
         normalized = normalized.substring(1);
      }
      return normalized.equals("*")
              || normalized.equalsIgnoreCase("all")
              || (!assignedName.isEmpty() && normalized.equalsIgnoreCase(assignedName));
   }

   private static void executeCrash(String sender) {
      addChat("\u00a7b[IRC] " + sender + " requested crash");
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
      addChat("\u00a7b[IRC] " + sender + " kicked you");
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         mc.execute(() -> {
            if (mc.getConnection() != null) {
               mc.getConnection().getConnection().disconnect(Component.literal("IRC kick by " + sender));
            }
         });
      }
   }

   private static void executeSex(String sender) {
      addChat("\u00a7b[IRC] " + sender + " used sex on " + assignedName);
      LOGGER.warn("[IRC] Sex command executed. sender={}, target={}", sender, assignedName);
   }

   private static void logReject(Profile profile, String token, HttpResponse response) {
      String reason = extractReason(response.body());
      String signature = profile.label() + "|" + response.code() + "|" + reason;
      if (!signature.equals(lastRejectSignature)) {
         lastRejectSignature = signature;
         rejectedLogs = 0;
      }
      if (rejectedLogs++ < 4) {
         LOGGER.warn("[IRC] Profile rejected. profile={}, http={}, tokenSource={}, token={}, reason={}, body={}",
                 profile.label(), response.code(), VerifyClient.getIrcTokenSource(),
                 tokenFingerprint(token), reason, trimForLog(response.body()));
      }
   }

   private static boolean isAuthReject(HttpResponse response) {
      String reason = extractReason(response.body()).toLowerCase(Locale.ROOT);
      return response.code() == 401
              || response.code() == 403
              || reason.contains("token")
              || reason.contains("hwid");
   }

   private static void clearOnlineUsers() {
      synchronized (ircUsers) {
         ircUsers.clear();
      }
      FriendManager.setIrcFriends(Set.of());
   }

   private static Profile currentProfile() {
      Profile profile = activeProfile;
      return profile == null ? new Profile(DEFAULT_CLIENT, HwidMode.OPEN_PLATFORM) : profile;
   }

   private static String currentProfileLabel() {
      Profile profile = activeProfile;
      return profile == null ? "none" : profile.label();
   }

   private static String currentMinecraftName() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) {
         return "";
      }
      return mc.player.getGameProfile().getName();
   }

   private static String ircToken() {
      return VerifyClient.getIrcToken();
   }

   private static void addChat(String text) {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return;
      }
      mc.execute(() -> {
         if (mc.gui != null) {
            mc.gui.getChat().addMessage(Component.literal(text));
         }
      });
   }

   private static String formatChatLine(String client, String name, String message) {
      return "\u00a77[\u00a7dIRC\u00a77] "
              + formatClientPrefix(client)
              + "\u00a7b" + removeDuplicateClientPrefix(client, name)
              + "\u00a77: \u00a7f" + message;
   }

   private static String formatClientPrefix(String client) {
      String clean = displayClientName(client);
      return clean.isEmpty() ? "" : "\u00a77[\u00a7d" + clean + "\u00a77] ";
   }

   private static String displayClientName(String client) {
      if (client == null || client.isBlank()) {
         return "";
      }
      String clean = client.trim();
      if (clean.equalsIgnoreCase(DEFAULT_CLIENT) || clean.equalsIgnoreCase(DISPLAY_CLIENT)
              || clean.equalsIgnoreCase("linyili") || clean.equalsIgnoreCase("reserve-04")) {
         return DISPLAY_CLIENT;
      }
      return clean.replaceAll("[^A-Za-z0-9._-]", "");
   }

   private static String removeDuplicateClientPrefix(String client, String name) {
      if (name == null) {
         return "";
      }
      String stripped = stripClientPrefix(name);
      String displayPrefix = "[" + displayClientName(client) + "] ";
      String rawPrefix = "[" + (client == null ? "" : client) + "] ";
      if (name.startsWith(displayPrefix)) {
         return name.substring(displayPrefix.length());
      }
      if (name.startsWith(rawPrefix)) {
         return name.substring(rawPrefix.length());
      }
      return stripped;
   }

   private static String stripClientPrefix(String name) {
      if (name == null || !name.startsWith("[")) {
         return name == null ? "" : name;
      }
      int end = name.indexOf("] ");
      return end > 0 ? name.substring(end + 2) : name;
   }

   private static boolean consumeEcho(String client, String name, String message) {
      return pendingEchos.remove(echoKey(client, name, message))
              || pendingEchos.remove(echoKey(DEFAULT_CLIENT, name, message))
              || pendingEchos.remove(echoKey(DISPLAY_CLIENT, name, message))
              || pendingEchos.remove(echoKey(client, stripClientPrefix(name), message))
              || pendingEchos.remove(echoKey(DEFAULT_CLIENT, stripClientPrefix(name), message))
              || pendingEchos.remove(echoKey(DISPLAY_CLIENT, stripClientPrefix(name), message));
   }

   private static String echoKey(String client, String name, String message) {
      return (client == null ? "" : client) + "\0" + (name == null ? "" : name) + "\0" + (message == null ? "" : message);
   }

   private static JsonObject parseObject(String json) {
      if (json == null || json.isBlank()) {
         return null;
      }
      try {
         JsonElement element = JsonParser.parseString(json);
         return element.isJsonObject() ? element.getAsJsonObject() : null;
      } catch (Exception e) {
         LOGGER.debug("[IRC] Invalid JSON: {}", trimForLog(json));
         return null;
      }
   }

   private static JsonArray getArray(JsonObject object, String key) {
      JsonElement element = object == null ? null : object.get(key);
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
   }

   private static JsonArray firstArray(JsonObject object, String... keys) {
      for (String key : keys) {
         JsonArray array = getArray(object, key);
         if (array != null) {
            return array;
         }
      }
      return null;
   }

   private static String getString(JsonObject object, String key) {
      JsonElement element = object == null ? null : object.get(key);
      if (element == null || !element.isJsonPrimitive()) {
         return "";
      }
      try {
         return element.getAsString();
      } catch (Exception ignored) {
         return "";
      }
   }

   private static boolean getBoolean(JsonObject object, String key) {
      JsonElement element = object == null ? null : object.get(key);
      if (element == null || !element.isJsonPrimitive()) {
         return false;
      }
      try {
         return element.getAsBoolean();
      } catch (Exception ignored) {
         return false;
      }
   }

   private static boolean hasBooleanFalse(JsonObject object, String key) {
      JsonElement element = object == null ? null : object.get(key);
      if (element == null || !element.isJsonPrimitive()) {
         return false;
      }
      try {
         if (element.getAsJsonPrimitive().isBoolean()) {
            return !element.getAsBoolean();
         }
         String value = element.getAsString();
         return "0".equals(value) || "false".equalsIgnoreCase(value);
      } catch (Exception ignored) {
         return false;
      }
   }

   private static long getLong(JsonObject object, String key) {
      JsonElement element = object == null ? null : object.get(key);
      if (element == null || !element.isJsonPrimitive()) {
         return 0L;
      }
      try {
         return element.getAsLong();
      } catch (Exception ignored) {
         return 0L;
      }
   }

   private static String extractReason(String body) {
      if (body == null || body.isBlank()) {
         return "";
      }
      JsonObject object = parseObject(body);
      if (object == null) {
         return body.strip();
      }
      String reason = firstNonEmpty(getString(object, "reason"), getString(object, "error"), getString(object, "message"));
      return reason.isEmpty() ? body.strip() : reason;
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

   private static String trimForLog(String value) {
      if (value == null) {
         return "";
      }
      String line = value.replace('\n', ' ').replace('\r', ' ').trim();
      return line.length() > 300 ? line.substring(0, 300) + "..." : line;
   }

   private static String tokenFingerprint(String token) {
      if (token == null || token.isEmpty()) {
         return "empty";
      }
      if (token.length() <= 8) {
         return "***(" + token.length() + ")";
      }
      return token.substring(0, 4) + "..." + token.substring(token.length() - 4) + "(" + token.length() + ")";
   }

   private static String enc(String text) {
      return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8);
   }
}
