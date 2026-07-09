package awa.qwq.ovo.Naven.protocol.heypixel;

import awa.qwq.ovo.Naven.protocol.heypixel.crypto.CryptoHelper;
import awa.qwq.ovo.Naven.protocol.heypixel.data.HeypixelUUID;
import awa.qwq.ovo.Naven.protocol.heypixel.data.UuidDerivation;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.ClickSyncPacket;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.HeartbeatPacket;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.HeypixelMessageRegistry;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.IHeypixelMessage;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.ServerEventMessage;
import awa.qwq.ovo.Naven.protocol.heypixel.messages.SessionMessage;
import awa.qwq.ovo.Naven.protocol.heypixel.network.ChannelRegistry;
import awa.qwq.ovo.Naven.protocol.heypixel.network.HeypixelNetworkBridge;
import awa.qwq.ovo.Naven.protocol.heypixel.utils.ClickTracker;
import awa.qwq.ovo.Naven.protocol.heypixel.utils.ClientInfoCollector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;

public class HeypixelSession {
   private static HeypixelSession current;
   private static final Set<String> REGISTERED_CHANNELS = new HashSet<>(Arrays.asList(
           "worldedit:cui", "fml:loginwrapper", "forge:tier_sorting",
           "storemod:buy", "floodgate:custom", "floodgate:packet",
           "heypixel:s2cevent", "report:areport", "plugin:guild",
           "fml:play", "floodgate:netease", "floodgate:transfer",
           "fml:handshake", "heypixel:onlinestats", "forge:split",
           "floodgate:form", "geckolib:main", "floodgate:skin"
   ));

   private final HeypixelUUID initialUUID = HeypixelUUID.random();
   private final ClickTracker clickTracker = new ClickTracker();
   private final ChannelRegistry channelRegistry = new ChannelRegistry();
   private final String userId;
   private final int randomPort = new Random().nextInt(77772, 81079);
   private ScheduledExecutorService executor;
   private int lastLeftClickCount;
   private long keyRefreshTimestamp = -1L;
   private boolean sentChannelRegister;
   private volatile PacketSender packetSender;
   private HeypixelUUID sessionUUID;
   private CryptoHelper crypto;
   private ClientInfoCollector clientInfo;
   private String minecraftUUID = "";
   private String moduleId = "";
   private String moduleVersion = "";
   private String encryptedUUID = "";
   private boolean encryptionEnabled;
   private boolean handshakeComplete;
   private boolean sentInitialInfo;

   private HeypixelSession() {
      this.userId = deriveUserId(MinecraftClient.getInstance().getGameProfile() == null ? null : MinecraftClient.getInstance().getGameProfile().getId());
   }

   public static synchronized HeypixelSession current() {
      if (current == null) {
         current = new HeypixelSession();
      }
      return current;
   }

   public static synchronized void resetCurrent() {
      if (current != null) {
         current.dispose();
      }
      current = new HeypixelSession();
   }

   public static synchronized void disposeCurrent() {
      if (current != null) {
         current.dispose();
         current = null;
      }
   }

   public void setPacketSender(PacketSender packetSender) {
      this.packetSender = packetSender;
   }

   public void ensureDerived() {
      if (this.sessionUUID != null && this.crypto != null) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      UUID uuid = client.getGameProfile() == null ? UUID.randomUUID() : client.getGameProfile().getId();
      this.minecraftUUID = uuid.toString();
      String derived = UuidDerivation.deriveUuid(this.minecraftUUID, this.userId, this.initialUUID.toString());
      this.sessionUUID = new HeypixelUUID(derived);
      this.crypto = new CryptoHelper(this.sessionUUID.toString());
      this.refreshEncryptedStrings();
      this.clientInfo = new ClientInfoCollector(this.userId, this.crypto);
   }

   private void refreshEncryptedStrings() {
      this.moduleId = this.crypto.encryptToBase64(this.sessionUUID.toString());
      this.moduleVersion = this.crypto.encryptToBase64("0");
      this.encryptedUUID = this.crypto.encryptToBase64("0");
   }

   public void refreshKeyIfNeeded() {
      long now = System.currentTimeMillis();
      if (this.keyRefreshTimestamp <= 0L || now > this.keyRefreshTimestamp) {
         this.keyRefreshTimestamp = now + 30000L;
         this.refreshEncryptedStrings();
      }
   }

   public void activate() {
      this.ensureDerived();
      this.handshakeComplete = true;
      this.refreshKeyIfNeeded();
      this.startBackgroundTasks();
   }

   public void sendInitialInfo() {
      if (this.sentInitialInfo) {
         return;
      }

      this.ensureDerived();
      SessionMessage message = new SessionMessage();
      message.type = HeypixelRequestType.INFO;
      message.requestId = this.sessionUUID.toEncoded();
      message.requestTimestamp = System.currentTimeMillis();
      this.sendMessage(message);
      this.sentInitialInfo = true;
      this.encryptionEnabled = true;
   }

   public void handleChannelRegister(byte[] payload, PacketSender sender) {
      this.setPacketSender(sender);
      this.channelRegistry.incrementRegisterCount();
      this.channelRegistry.parseAndAdd(payload);
      if (this.channelRegistry.getRegisterCount() >= 2 && !this.sentChannelRegister) {
         this.sentChannelRegister = true;
         this.channelRegistry.addChannels(REGISTERED_CHANNELS);
         this.sendPluginMessage(HeypixelNetworkBridge.MINECRAFT_REGISTER, this.channelRegistry.serialize());
         this.sendSkinSync();
      }
   }

   public void handleHeypixelMessage(byte[] payload, PacketSender sender) {
      this.setPacketSender(sender);
      this.ensureDerived();
      try {
         ByteBuffer buffer = new ByteBuffer(payload);
         if ((buffer.readByte() & 0xFF) != 250) {
            return;
         }

         int messageId = buffer.readBigEndian32();
         int bodyLength = buffer.readVarInt();
         if (bodyLength < 0 || bodyLength > buffer.remaining()) {
            bodyLength = buffer.remaining();
         }
         byte[] bodyData = buffer.readBytes(bodyLength);
         byte[] decryptedData = this.crypto.decrypt(bodyData);
         IHeypixelMessage message = HeypixelMessageRegistry.createS2CMessage(messageId);
         if (message == null) {
            return;
         }

         message.deserialize(new ByteBuffer(decryptedData), this);
         if (message instanceof ServerEventMessage) {
            message.handle(this);
         }
      } catch (Throwable ignored) {
      }
   }

   public void handleFloodgateForm(byte[] payload, PacketSender sender) {
      this.setPacketSender(sender);
      if (payload == null || payload.length < 3) {
         return;
      }

      this.sendPluginMessage(HeypixelNetworkBridge.FLOODGATE_FORM, new byte[]{payload[1], payload[2], 48});
   }

   public void sendMessage(IHeypixelMessage message) {
      try {
         this.ensureDerived();
         byte[] payload = this.serializeMessage(message);
         this.sendPluginMessage(HeypixelNetworkBridge.HEYPIXEL_EVENT, payload);
      } catch (Throwable ignored) {
      }
   }

   private byte[] serializeMessage(IHeypixelMessage message) {
      ByteBuffer bodyBuffer = new ByteBuffer();
      bodyBuffer.writeLong(System.currentTimeMillis());
      message.serialize(bodyBuffer, this);
      byte[] bodyData = bodyBuffer.toArray();

      if (message instanceof SessionMessage && this.encryptionEnabled) {
         bodyData = this.crypto.encrypt(bodyData);
      }

      ByteBuffer packetBuffer = new ByteBuffer();
      packetBuffer.writeVarInt(HeypixelMessageRegistry.getMessageId(message.getClass()));
      packetBuffer.writeVarInt(bodyData.length + 1);

      packetBuffer.writeBytes(bodyData);
      return packetBuffer.toArray();
   }

   private void sendSkinSync() {
      this.sendPluginMessage(HeypixelNetworkBridge.HEYPIXEL_SKIN_SYNC,
              "ASQwMDAwMDAwMC0wMDAwLTQwMDAtODAwMC0wMDAwMzliYzYyMTM=".getBytes(StandardCharsets.UTF_8));
   }

   private void sendPluginMessage(net.minecraft.util.Identifier channel, byte[] data) {
      HeypixelNetworkBridge.send(channel, data, this.packetSender);
   }

   public synchronized void startBackgroundTasks() {
      if (this.executor != null && !this.executor.isShutdown()) {
         return;
      }

      this.executor = Executors.newScheduledThreadPool(2, runnable -> {
         Thread thread = new Thread(runnable, "Naven-Heypixel");
         thread.setDaemon(true);
         return thread;
      });
      this.executor.scheduleAtFixedRate(() -> {
         if (this.handshakeComplete) {
            this.sendMessage(new HeartbeatPacket());
         }
      }, 5L, 5L, TimeUnit.SECONDS);
      this.executor.scheduleAtFixedRate(() -> {
         int clicks = this.clickTracker.getLeftClickCount();
         if (clicks != this.lastLeftClickCount) {
            this.lastLeftClickCount = clicks;
            this.sendMessage(new ClickSyncPacket(clicks));
         }
      }, 50L, 50L, TimeUnit.MILLISECONDS);
   }

   public void dispose() {
      if (this.executor != null) {
         this.executor.shutdownNow();
         this.executor = null;
      }
   }

   public ClickTracker getClickTracker() {
      return this.clickTracker;
   }

   public HeypixelUUID getSessionUUID() {
      this.ensureDerived();
      return this.sessionUUID;
   }

   public CryptoHelper getCrypto() {
      this.ensureDerived();
      return this.crypto;
   }

   public ClientInfoCollector getClientInfo() {
      this.ensureDerived();
      return this.clientInfo;
   }

   public long getUserIdAsLong() {
      try {
         return Long.parseLong(this.userId);
      } catch (NumberFormatException ignored) {
         return Integer.toUnsignedLong(this.userId.hashCode());
      }
   }

   public int getRandomPort() {
      return this.randomPort;
   }

   public String getModuleId() {
      return this.moduleId;
   }

   public String getModuleVersion() {
      return this.moduleVersion;
   }

   public String getEncryptedUUID() {
      return this.encryptedUUID;
   }

   private static String deriveUserId(UUID uuid) {
      long value = uuid == null ? System.nanoTime() : uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
      value &= Long.MAX_VALUE;
      if (value < 1000000000L) {
         value += 1000000000L;
      }
      return Long.toString(value);
   }
}
