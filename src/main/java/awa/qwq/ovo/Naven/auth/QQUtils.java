package awa.qwq.ovo.Naven.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;

public class QQUtils {
   private static final Logger logger = LogManager.getLogger("QQUtils");
   private static final byte[] header = new byte[]{-1, -40, -1, -32, 0, 16, 74, 70, 73, 70};
   private static Set<String> cachedAllQQ = null;
   private static String cachedRecentQQ = null;
   private static long cacheTime = 0;
   private static final long CACHE_DURATION = 60000; // 1分钟缓存

   public static Set<String> getAllQQ() throws IOException {
      return getAllQQFromLocal();
   }

   public static Set<String> getAllQQFromLocal() {
      if (cachedAllQQ != null && (System.currentTimeMillis() - cacheTime) < CACHE_DURATION) {
         return new HashSet<>(cachedAllQQ);
      }

      Set<String> qqs = new HashSet<>();

      if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
         return qqs;
      }

      String[] ntPaths = {
              System.getenv("APPDATA") + "\\Tencent\\QQ\\Misc",
              System.getenv("APPDATA") + "\\Tencent\\QQ\\nt_qq\\global\\nt_data\\QQ\\",
              System.getenv("APPDATA") + "\\Tencent\\QQ\\temp\\",
              System.getenv("LOCALAPPDATA") + "\\Tencent\\QQ\\UserData\\"
      };

      for (String path : ntPaths) {
         File dir = new File(path);
         if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
               for (File file : files) {
                  String fileName = file.getName();
                  if (file.isDirectory() && fileName.matches("\\d{5,10}")) {
                     qqs.add(fileName);
                  }
                  else if (!file.isDirectory() && fileName.matches("[0-9]+") &&
                          fileName.length() >= 5 && fileName.length() <= 10 &&
                          checkNTQQFile(file)) {
                     qqs.add(fileName);
                  }
               }
            }
         }
      }
      File legacyPath = new File(System.getenv("PUBLIC") + "\\Documents\\Tencent\\QQ\\UserDataInfo.ini");
      if (legacyPath.exists()) {
         try {
            List<String> lines = Files.readAllLines(legacyPath.toPath());
            for (String line : lines) {
               if (line.startsWith("UserDataSavePath=")) {
                  String dataPath = line.split("=")[1];
                  File dataDir = new File(dataPath);
                  if (dataDir.exists() && dataDir.isDirectory()) {
                     File[] qqDirs = dataDir.listFiles();
                     if (qqDirs != null) {
                        for (File qqDir : qqDirs) {
                           if (qqDir.isDirectory() && qqDir.getName().matches("\\d{5,10}")) {
                              qqs.add(qqDir.getName());
                           }
                        }
                     }
                  }
                  break;
               }
            }
         } catch (Exception e) {
            logger.debug("Error reading legacy QQ path", e);
         }
      }
      String regQQ = getQQFromRegistry();
      if (regQQ != null && !regQQ.isEmpty()) {
         qqs.add(regQQ);
      }
      Set<String> installQQ = getQQFromInstallPath();
      if (installQQ != null && !installQQ.isEmpty()) {
         qqs.addAll(installQQ);
      }
      cachedAllQQ = new HashSet<>(qqs);
      cacheTime = System.currentTimeMillis();

      return qqs;
   }

   public static String getRecentQQ() {
      if (cachedRecentQQ != null && (System.currentTimeMillis() - cacheTime) < CACHE_DURATION) {
         return cachedRecentQQ;
      }

      Set<String> allQQ = getAllQQFromLocal();
      if (allQQ == null || allQQ.isEmpty()) {
         return null;
      }

      String recentQQ = findMostRecentQQ(allQQ);

      if (recentQQ != null) {
         cachedRecentQQ = recentQQ;
         cacheTime = System.currentTimeMillis();
      }

      return recentQQ;
   }

   private static String findMostRecentQQ(Set<String> qqs) {
      String latestQQ = null;
      long latestTime = 0;

      for (String qq : qqs) {
         long lastAccessTime = getQQLastAccessTime(qq);
         if (lastAccessTime > latestTime) {
            latestTime = lastAccessTime;
            latestQQ = qq;
         }
      }

      if (latestQQ == null && !qqs.isEmpty()) {
         latestQQ = qqs.iterator().next();
      }

      return latestQQ;
   }

   private static long getQQLastAccessTime(String qq) {
      long maxTime = 0;

      String[] possiblePaths = {
              System.getenv("APPDATA") + "\\Tencent\\QQ\\" + qq,
              System.getenv("APPDATA") + "\\Tencent\\QQ\\Misc\\" + qq,
              System.getenv("LOCALAPPDATA") + "\\Tencent\\QQ\\" + qq,
              System.getenv("APPDATA") + "\\Tencent\\QQ\\nt_qq\\global\\nt_data\\QQ\\" + qq
      };

      for (String path : possiblePaths) {
         File file = new File(path);
         if (file.exists()) {
            maxTime = Math.max(maxTime, file.lastModified());
         }
      }

      return maxTime;
   }

   private static String getQQFromRegistry() {
      try {
         Process process = Runtime.getRuntime().exec(
                 new String[]{"reg", "query", "HKEY_CURRENT_USER\\Software\\Tencent\\QQ", "/v", "CurrentQQ"}
         );
         String output = readProcessOutput(process);
         if (output != null && output.contains("CurrentQQ")) {
            String[] lines = output.split("\n");
            for (String line : lines) {
               if (line.contains("CurrentQQ")) {
                  String[] parts = line.split("\\s+");
                  for (String part : parts) {
                     if (part.matches("\\d{5,10}")) {
                        return part;
                     }
                  }
               }
            }
         }

         process = Runtime.getRuntime().exec(
                 new String[]{"reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Tencent\\QQ", "/v", "Install"}
         );
         output = readProcessOutput(process);
         if (output != null && output.contains("Install")) {
            String installPath = extractInstallPath(output);
            if (installPath != null) {
               return extractQQFromInstallPath(installPath);
            }
         }
      } catch (Exception e) {
         logger.debug("Error reading registry", e);
      }
      return null;
   }

   private static Set<String> getQQFromInstallPath() {
      Set<String> qqs = new HashSet<>();

      try {
         Process process = Runtime.getRuntime().exec(
                 new String[]{"reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Tencent\\QQ", "/v", "Install"}
         );
         String output = readProcessOutput(process);
         String installPath = extractInstallPath(output);

         if (installPath != null) {
            File installDir = new File(installPath);
            if (installDir.exists()) {
               File[] files = installDir.listFiles();
               if (files != null) {
                  for (File file : files) {
                     if (file.isDirectory() && file.getName().matches("\\d{5,10}")) {
                        qqs.add(file.getName());
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         logger.debug("Error scanning install path", e);
      }

      return qqs;
   }

   private static String extractQQFromInstallPath(String installPath) {
      if (installPath == null) return null;

      String[] parts = installPath.split("\\\\");
      for (String part : parts) {
         if (part.matches("\\d{5,10}")) {
            return part;
         }
      }
      return null;
   }

   private static String extractInstallPath(String regOutput) {
      if (regOutput == null) return null;

      String[] lines = regOutput.split("\n");
      for (String line : lines) {
         if (line.contains("REG_SZ") || line.contains("REG_EXPAND_SZ")) {
            String[] parts = line.split("\\s+");
            for (String part : parts) {
               if (part.contains(":\\") && (part.contains("Tencent") || part.contains("QQ"))) {
                  return part.trim();
               }
            }
         }
      }
      return null;
   }

   public static String getQQForHWID() {
      String recentQQ = getRecentQQ();
      if (recentQQ != null && !recentQQ.isEmpty()) {
         return "QQ_" + recentQQ.hashCode();
      }

      return "NO_QQ_" + System.getProperty("user.name").hashCode();
   }

   public static List<String> getSortedQQList() {
      Set<String> allQQ = getAllQQFromLocal();
      List<String> qqList = new ArrayList<>(allQQ);
      qqList.sort((a, b) -> Long.compare(getQQLastAccessTime(b), getQQLastAccessTime(a)));

      return qqList;
   }

   public static boolean hasQQ() {
      Set<String> allQQ = getAllQQFromLocal();
      return allQQ != null && !allQQ.isEmpty();
   }

   public static void clearCache() {
      cachedAllQQ = null;
      cachedRecentQQ = null;
      cacheTime = 0;
   }

   private static boolean checkNTQQFile(File file) {
      try (FileInputStream stream = new FileInputStream(file)) {
         byte[] headerBytes = new byte[10];
         if (stream.read(headerBytes) == 10) {
            return Arrays.equals(headerBytes, header);
         }
      } catch (Exception e) {
         logger.debug("Error checking NTQQ file", e);
      }
      return false;
   }

   private static String readProcessOutput(Process process) {
      try {
         BufferedReader reader = new BufferedReader(
                 new InputStreamReader(process.getInputStream())
         );
         StringBuilder output = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
         }
         process.waitFor();
         return output.toString();
      } catch (Exception e) {
         return null;
      }
   }

   public static void main(String[] args) {
   }
}