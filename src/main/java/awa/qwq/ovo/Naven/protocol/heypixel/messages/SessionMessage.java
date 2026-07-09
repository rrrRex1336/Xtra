package awa.qwq.ovo.Naven.protocol.heypixel.messages;

import awa.qwq.ovo.Naven.protocol.heypixel.ByteBuffer;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelMessageType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelRequestType;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;
import awa.qwq.ovo.Naven.protocol.heypixel.data.DllHashProfiles;
import awa.qwq.ovo.Naven.protocol.heypixel.data.ModPresets;
import awa.qwq.ovo.Naven.protocol.heypixel.utils.ClientInfoCollector;
import awa.qwq.ovo.Naven.protocol.heypixel.utils.ReflectCheckParser;

public class SessionMessage extends HeypixelMessageBase {
   public HeypixelRequestType type = HeypixelRequestType.INFO;
   public String requestId = "";
   public long requestTimestamp;
   public String extraContent = "";

   @Override
   public HeypixelMessageType messageId() {
      return HeypixelMessageType.SESSION_MESSAGE;
   }

   @Override
   public boolean requiresEncryption() {
      return true;
   }

   @Override
   public boolean requiresObfuscation() {
      return true;
   }

   @Override
   public void serialize(ByteBuffer buffer, HeypixelSession session) {
      buffer.writeString(session.getSessionUUID().toString());
      buffer.writeSignedByte(this.type.id());
      buffer.writeString(this.requestId);
      buffer.writeLong(this.requestTimestamp);

      switch (this.type) {
         case INFO -> writeInfoPayload(buffer, session);
         case BLACK_CLASS -> writeBlackClassPayload(buffer, session);
         case BLACK_MODULE -> writeBlackModulePayload(buffer, session);
         case REFLECT_CHECK -> this.writeReflectCheckPayload(buffer, session);
      }
   }

   private static void writeInfoPayload(ByteBuffer buffer, HeypixelSession session) {
      ClientInfoCollector clientInfo = session.getClientInfo();

      buffer.writeArrayLength(ModPresets.getModList().size());
      for (ModPresets.ModEntry mod : ModPresets.getModList()) {
         buffer.writeStringRaw(mod.modName());
         buffer.writeStringRaw(mod.modPath());
         buffer.writeStringRaw(mod.modHash());
      }

      buffer.writeString(clientInfo.gamePath);
      buffer.writeString(clientInfo.jdkPath);

      buffer.writeArrayLength(clientInfo.cpuInfo.length);
      for (String info : clientInfo.cpuInfo) {
         buffer.writeStringRaw(info);
      }

      buffer.writeArrayLength(clientInfo.osInfo.length);
      for (String info : clientInfo.osInfo) {
         buffer.writeStringRaw(info);
      }

      buffer.writeArrayLength(clientInfo.networkAdapters.length);
      for (String[] adapter : clientInfo.networkAdapters) {
         buffer.writeArrayLength(adapter.length);
         for (String value : adapter) {
            buffer.writeStringRaw(value);
         }
      }

      buffer.writeArrayLength(clientInfo.hardwareIds.length);
      for (String[] hwId : clientInfo.hardwareIds) {
         buffer.writeArrayLength(hwId.length);
         for (String value : hwId) {
            buffer.writeStringRaw(value);
         }
      }

      buffer.writeArrayLength(clientInfo.userPaths.length);
      for (String path : clientInfo.userPaths) {
         buffer.writeStringRaw(path);
      }

      buffer.writeMapLength(1);
      buffer.writeStringRaw("UserId");
      long userId = session.getUserIdAsLong();
      if ((userId & 0x80000000L) == 0) {
         userId |= 0x80000000L;
      }
      buffer.writeLong(userId);

      buffer.writeArrayLength(clientInfo.encryptedMods.size());
      for (ClientInfoCollector.EncryptedModInfo mod : clientInfo.encryptedMods) {
         buffer.writeStringRaw(mod.encryptedName());
         buffer.writeStringRaw(mod.encryptedHash());
      }
   }

   private static void writeBlackClassPayload(ByteBuffer buffer, HeypixelSession session) {
      buffer.writeInt(session.getRandomPort());
      buffer.writeInt(1);
      buffer.writeArrayLength(1);
      buffer.writeString(session.getModuleVersion());
   }

   private static void writeBlackModulePayload(ByteBuffer buffer, HeypixelSession session) {
      buffer.writeInt(DllHashProfiles.MODULES.length);
      buffer.writeInt(1);
      buffer.writeArrayLength(1);
      buffer.writeString(session.getModuleId() + ":" + session.getEncryptedUUID());
   }

   private void writeReflectCheckPayload(ByteBuffer buffer, HeypixelSession session) {
      ReflectCheckParser.ReflectResponse response = ReflectCheckParser.processReflectCheck(this.extraContent, session.getCrypto());
      buffer.writeInt(response.hash());
      buffer.writeString(response.encryptedContent());
   }
}
