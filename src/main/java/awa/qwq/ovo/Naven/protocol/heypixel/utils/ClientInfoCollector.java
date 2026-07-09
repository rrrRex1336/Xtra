package awa.qwq.ovo.Naven.protocol.heypixel.utils;

import awa.qwq.ovo.Naven.protocol.heypixel.crypto.CryptoHelper;
import awa.qwq.ovo.Naven.protocol.heypixel.data.ModPresets;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.MinecraftClient;

public class ClientInfoCollector {
   private final Random random;
   private final CryptoHelper crypto;
   private final String cachedOsType;
   public final List<ModInfo> modList = new ArrayList<>();
   public final List<EncryptedModInfo> encryptedMods = new ArrayList<>();
   public String gamePath = "E:\\MCLDownload\\Game\\.minecraft";
   public String jdkPath = "E:\\MCLDownload\\ext\\jre-v64-220420\\jdk17";
   public String[] cpuInfo = new String[0];
   public String[] osInfo = new String[0];
   public String[][] networkAdapters = new String[0][];
   public String[][] hardwareIds = new String[0][];
   public String[] userPaths = new String[0];

   public ClientInfoCollector(String userId, CryptoHelper crypto) {
      this.random = new Random(safeSeed(userId));
      this.crypto = crypto;
      this.cachedOsType = this.generateOsType();
      this.collectAllInfo();
   }

   private void collectAllInfo() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.runDirectory != null) {
         this.gamePath = client.runDirectory.getAbsolutePath();
      }
      this.jdkPath = System.getProperty("java.home", this.jdkPath);

      this.cpuInfo = this.collectCpuInfo();
      this.osInfo = this.collectOsInfo();
      this.networkAdapters = this.collectNetworkAdapters();
      this.hardwareIds = this.collectHardwareIds();
      this.userPaths = this.collectUserPaths();
      this.collectModInfo();
   }

   private String[] collectCpuInfo() {
      return HardwareProfiles.CPU_PROFILES[this.random.nextInt(HardwareProfiles.CPU_PROFILES.length)].split("\\|");
   }

   private String generateOsType() {
      String name = System.getProperty("os.name", "Windows 10");
      if (name.contains("11")) {
         return "Windows 11";
      }
      return name.contains("Windows") ? "Windows 10" : name;
   }

   private String[] collectOsInfo() {
      String osVersion = this.cachedOsType.equals("Windows 11") ? "10.0.22621" : "10.0.19045";
      return new String[]{
              this.cachedOsType,
              System.getProperty("os.arch", "amd64"),
              osVersion,
              "64",
              java.util.UUID.randomUUID().toString()
      };
   }

   private String[][] collectNetworkAdapters() {
      int count = 2 + this.random.nextInt(3);
      String[][] adapters = new String[count][];
      Set<Integer> used = new HashSet<>();
      for (int i = 0; i < count; i++) {
         int idx;
         do {
            idx = this.random.nextInt(HardwareProfiles.NETWORK_ADAPTER_PROFILES.length);
         } while (!used.add(idx));

         byte[] macBytes = new byte[6];
         this.random.nextBytes(macBytes);
         macBytes[0] = (byte)(macBytes[0] & 0xFE);
         adapters[i] = new String[]{HardwareProfiles.NETWORK_ADAPTER_PROFILES[idx], formatMac(macBytes), "Up"};
      }
      return adapters;
   }

   private String[][] collectHardwareIds() {
      String ip = this.random.nextInt(1, 255) + "." + this.random.nextInt(0, 255) + "." + this.random.nextInt(0, 255) + "." + this.random.nextInt(1, 255);
      String disk = HardwareProfiles.DISK_PROFILES[this.random.nextInt(HardwareProfiles.DISK_PROFILES.length)];
      return new String[][]{new String[]{ip, "BIOS", disk}};
   }

   private String[] collectUserPaths() {
      String home = System.getProperty("user.home", "C:\\Users\\Player");
      String[] paths = new String[]{
              home,
              Path.of(home, "AppData", "Roaming").toString(),
              Path.of(home, "Desktop").toString()
      };

      String[] encoded = new String[paths.length];
      for (int i = 0; i < paths.length; i++) {
         encoded[i] = Base64.getEncoder().encodeToString(paths[i].getBytes(StandardCharsets.UTF_8));
      }
      return encoded;
   }

   private void collectModInfo() {
      this.modList.clear();
      this.encryptedMods.clear();
      for (ModPresets.ModEntry preset : ModPresets.getModList()) {
         this.modList.add(new ModInfo(preset.modName(), Path.of(preset.modPath().isEmpty() ? preset.modName() : preset.modPath()).getFileName().toString(), preset.modHash()));
         if (preset.modPath().isEmpty()) {
            continue;
         }

         this.encryptedMods.add(new EncryptedModInfo(
                 Base64.getEncoder().encodeToString(this.crypto.encrypt(preset.modPath().getBytes(StandardCharsets.UTF_8))),
                 Base64.getEncoder().encodeToString(this.crypto.encrypt(preset.modHash().getBytes(StandardCharsets.UTF_8)))
         ));
      }
   }

   private static int safeSeed(String userId) {
      try {
         return (int)Long.parseLong(userId);
      } catch (NumberFormatException ignored) {
         return userId == null ? 0 : userId.hashCode();
      }
   }

   private static String formatMac(byte[] macBytes) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < macBytes.length; i++) {
         if (i > 0) {
            builder.append(':');
         }
         builder.append(String.format("%02X", macBytes[i] & 0xFF));
      }
      return builder.toString();
   }

   public record ModInfo(String modId, String fileName, String hash) {
   }

   public record EncryptedModInfo(String encryptedName, String encryptedHash) {
   }
}
