package awa.qwq.ovo.Naven.protocol.heypixel.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModPresets {
   private static final List<ModEntry> MOD_LIST = new ArrayList<>();

   static {
      MOD_LIST.add(new ModEntry("minecraft", "D:\\MCLDownload\\Game\\.minecraft\\libraries\\net\\minecraft\\client\\1.20.1-20230612.114412\\client-1.20.1-20230612.114412-srg.jar", "3c8aa19b710a3a68f721210eb69b74594d13e218"));
      MOD_LIST.add(new ModEntry("saturn", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379650004073160@2@8.jar", "4c5826a73bdbc052db2fe080c582e59ae8ed227d"));
      MOD_LIST.add(new ModEntry("viaforge", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379650013243317@2@8.jar", "9df7487be88cc018ab295618c1f88a0a4886608f"));
      MOD_LIST.add(new ModEntry("geckolib", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649923891439@2@8.jar", "040e42eb566a764792ba0c77e39a56f020cabb8c"));
      MOD_LIST.add(new ModEntry("heypixel", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649932755505@2@8.jar", "c0fc23fb4cf86d3e258391bc95326a3c4faea690"));
      MOD_LIST.add(new ModEntry("entityculling", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649906096494@2@8.jar", "09c17c8794a0e00d2ccb51b8d7c1b812498c5c33"));
      MOD_LIST.add(new ModEntry("mixinextras", "", "5c35573a7b76103724799ca1174a69699b8a9aec"));
      MOD_LIST.add(new ModEntry("netease_official", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4681704866889354274@3@0.jar", "a9c7c9e3ff02a30f1920b65ee85af9c5e7c25a45"));
      MOD_LIST.add(new ModEntry("waveycapes", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379650032761209@2@8.jar", "c838d45bd4bb53ed66d66ed3cf3822a7ebd7ba09"));
      MOD_LIST.add(new ModEntry("ferritecore", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649915296094@2@8.jar", "417fb6ce8f52abf40bd9d0390371790f9576f8ba"));
      MOD_LIST.add(new ModEntry("embeddium_extra", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649994597910@2@8.jar", "bb422d5626bf69841e444a3e545c8e71b6928cf8"));
      MOD_LIST.add(new ModEntry("cloth_config", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649885641233@2@8.jar", "c65d07748acc57ceb45d53b3964368b84f34d54f"));
      MOD_LIST.add(new ModEntry("forge", "D:\\MCLDownload\\Game\\.minecraft\\libraries\\net\\minecraftforge\\forge\\1.20.1-47.3.0\\forge-1.20.1-47.3.0-universal.jar", "7aeb6f58286b5398dc7dfa1d3db1757e601145c1"));
      MOD_LIST.add(new ModEntry("embeddium", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649896042937@2@8.jar", "b772ec4f03fbe9b218e9f28d9441ea0a5724d5cf"));
      MOD_LIST.add(new ModEntry("oculus", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649983246969@2@8.jar", "27410903d3af950378776106b76503cfebe7ea3a"));
      MOD_LIST.add(new ModEntry("imblocker", "D:\\MCLDownload\\Game\\.minecraft\\mods\\4684379649974400055@2@8.jar", "65c67fd0c62ad2198902f451bed1f02a5fd15aa5"));
   }

   private ModPresets() {
   }

   public static List<ModEntry> getModList() {
      return Collections.unmodifiableList(MOD_LIST);
   }

   public record ModEntry(String modName, String modPath, String modHash) {
   }
}
