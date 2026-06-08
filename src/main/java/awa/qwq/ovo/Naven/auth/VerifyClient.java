package awa.qwq.ovo.Naven.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerifyClient {
    private static final Logger logger = LogManager.getLogger("VerifyClient");
    private static final String REPO_OWNER = "MengZeYu1337";
    private static final String REPO_NAME = "HWIDVerify";
    private static final String FILE_PATH = "HWID.txt";

    private static boolean isVerified = false;
    private static String userRole = "development";
    private static String userName = "LinYanLi1337";
    private static String boundQQ = null; // 绑定的QQ号
    private static boolean isFirstTime = false;

    private static final Map<String, String> roleWelcomeMessages = new HashMap<>();
    static {
        roleWelcomeMessages.put("development", "🚀 开发者通道已开启");
        roleWelcomeMessages.put("beta", "🔧 Beta测试者通道已开启");
        roleWelcomeMessages.put("user", "✨ 用户通道已开启");
        roleWelcomeMessages.put("unknown", "🌌 欢迎使用");
    }

    public static boolean verify() {
        try {
            String localHWID = HWIDCheck.getHWID();
            String enhancedHWID = HWIDCheck.getEnhancedHWID();
            logger.info("You HWID: {}", enhancedHWID);
            String hwidList = fetchHwidListFromGitHub();
            if (hwidList == null || hwidList.isEmpty()) {
                return false;
            }
            String[] lines = hwidList.split("\n");
            boolean hwidMatched = false;
            String matchedLine = null;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                String lineHWID = extractHWIDFromLine(line);
                if (lineHWID.equals(localHWID) || lineHWID.equals(enhancedHWID)) {
                    hwidMatched = true;
                    matchedLine = line;
                    break;
                }
            }

            if (!hwidMatched) {
                return false;
            }
            String expectedQQ = extractQQFromLine(matchedLine);
            if (expectedQQ == null || expectedQQ.isEmpty()) {
                logger.warn("Skip Other");
                isVerified = true;
                parseUserInfo(matchedLine);
                sendWelcomeMessage();
                return true;
            }
            Set<String> currentQQSet = QQUtils.getAllQQFromLocal();
            String currentQQ = QQUtils.getRecentQQ();
            if (currentQQ != null && currentQQ.equals(expectedQQ)) {
                isVerified = true;
                boundQQ = currentQQ;
                parseUserInfo(matchedLine);
                sendWelcomeMessage();
                return true;
            } else {
                String warningMessage = generateQQWarningMessage(expectedQQ, currentQQ, currentQQSet);
                logger.warn(warningMessage);
                System.out.println("\n⚠️ " + warningMessage);
                return false;
            }

        } catch (Exception e) {
            logger.error("验证过程发生异常", e);
            return false;
        }
    }

    private static String extractQQFromLine(String line) {
        if (line == null || !line.contains("[")) {
            return null;
        }
        int start = line.indexOf("[");
        while (start != -1) {
            int end = line.indexOf("]", start);
            if (end == -1) break;

            String bracketContent = line.substring(start + 1, end);
            if (bracketContent.startsWith("QQ:") || bracketContent.startsWith("QQ_")) {
                String qqPart = bracketContent.substring(3);
                if (qqPart.matches("\\d{5,11}")) {
                    return qqPart;
                }
            }
            else if (bracketContent.matches("\\d{5,11}")) {
                return bracketContent;
            }

            start = line.indexOf("[", start + 1);
        }

        return null;
    }

    private static void parseUserInfo(String line) {
        if (line == null || !line.contains("[")) return;

        try {
            int start = line.indexOf("[");
            int end = line.lastIndexOf("]");
            if (start < end) {
                String info = line.substring(start + 1, end);
                String[] parts = info.split("\\]\\[");
                for (String part : parts) {
                    if (part.startsWith("QQ:") || part.startsWith("QQ_")) {
                        continue;
                    }
                    if (part.matches("\\d{5,11}")) {
                        continue;
                    }
                }
                String[] filteredParts = new String[parts.length];
                int idx = 0;
                for (String part : parts) {
                    if (!part.startsWith("QQ:") && !part.startsWith("QQ_") && !part.matches("\\d{5,11}")) {
                        filteredParts[idx++] = part;
                    }
                }

                if (idx >= 2) {
                    userName = filteredParts[0];
                    userRole = filteredParts[1];
                } else if (idx == 1) {
                    userName = filteredParts[0];
                }
            }
        } catch (Exception e) {
            logger.debug("Failed user Session", e);
        }
    }

    private static String generateQQWarningMessage(String expectedQQ, String currentQQ, Set<String> allQQ) {
        StringBuilder sb = new StringBuilder();
        sb.append("sb?");
        sb.append("sb?");
        sb.append("sb?");

        if (currentQQ == null || allQQ == null || allQQ.isEmpty()) {
        } else if (!allQQ.contains(expectedQQ)) {
        } else if (currentQQ != null && !currentQQ.equals(expectedQQ)) {
        }

        return sb.toString();
    }

    public static boolean verifyHWIDOnly() {
        try {
            String localHWID = HWIDCheck.getHWID();
            String enhancedHWID = HWIDCheck.getEnhancedHWID();
            String hwidList = fetchHwidListFromGitHub();

            if (hwidList == null || hwidList.isEmpty()) {
                return false;
            }

            String[] lines = hwidList.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                String lineHWID = extractHWIDFromLine(line);
                if (lineHWID.equals(localHWID) || lineHWID.equals(enhancedHWID)) {
                    parseUserInfo(line);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getBoundQQ() {
        return boundQQ;
    }

    private static String extractHWIDFromLine(String line) {
        if (line.contains("[")) {
            return line.substring(0, line.indexOf("[")).trim();
        }
        if (line.contains(" ")) {
            return line.substring(0, line.indexOf(" ")).trim();
        }
        return line.trim();
    }

    private static String fetchHwidListFromGitHub() {
        HttpURLConnection conn = null;
        try {
            String token = linyanli1337.Loader.getGitHubToken();
            if (token == null || token.isEmpty() || token.equals("DEBUG_DETECTED")) {
                logger.error("Invalid GitHub token: {}", token);
                return null;
            }
            if (!linyanli1337.Loader.verifyIntegrity()) {
                logger.error("Integrity check failed");
                return null;
            }

            URL url = new URL("https://api.github.com/repos/" + REPO_OWNER + "/" +
                    REPO_NAME + "/contents/" + FILE_PATH);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "token " + token);
            conn.setRequestProperty("Accept", "application/vnd.github.v3.raw");
            conn.setRequestProperty("User-Agent", "Naven-MayRainEdit-Auth");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    return content.toString();
                }
            } else {
                logger.error("GitHub API returned: {}", conn.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching HWID list", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static void sendWelcomeMessage() {

        logger.info("User: {}, Rank: {}, QQ: {}", userName, userRole, boundQQ);
    }

    public static boolean isVerified() {
        return isVerified;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setFirstTime(boolean firstTime) {
        isFirstTime = firstTime;
    }

    public static void main(String[] args) {
        boolean result = verify();
        System.out.println("\n: " + (result ? "C" : "sb?"));
        if (result) {
            System.out.println("User: " + getUserName());
            System.out.println("Rank: " + getUserRole());
            System.out.println("Bind QQ: " + getBoundQQ());
        }
    }
}