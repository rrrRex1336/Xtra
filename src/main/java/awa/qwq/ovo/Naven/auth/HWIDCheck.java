package awa.qwq.ovo.Naven.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HWIDCheck {
    private static final Logger logger = LogManager.getLogger("HWID-Check");

    public static String getHWID() {
        try {
            String hardwareInfo = collectStableHardwareInfo();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hardwareInfo.getBytes());
            return formatHWID(hash);
        } catch (Exception e) {
            logger.error("Error generating HWID", e);
            return "ERROR-GENERATING-HWID";
        }
    }

    public static String getEnhancedHWID() {
        try {
            String hardwareInfo = collectStableHardwareInfo();
            String qqSalt = QQUtils.getQQForHWID();
            String combinedInfo = hardwareInfo + "|" + qqSalt + "|ENHANCED_SALT_2024";

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combinedInfo.getBytes());
            return formatHWID(hash);
        } catch (Exception e) {
            logger.error("Error generating enhanced HWID", e);
            return getHWID();
        }
    }

    public static String getBoundQQ() {
        return QQUtils.getRecentQQ();
    }

    private static String collectStableHardwareInfo() {
        StringBuilder sb = new StringBuilder();

        try {
            String motherboardInfo = getMotherboardInfo();
            String cpuInfo = getCPUInfo();
            String diskInfo = getDiskInfo();

            sb.append(motherboardInfo).append("|");
            sb.append(cpuInfo).append("|");
            sb.append(diskInfo).append("|");
            sb.append("HARDWARE_HWID_SALT_2024").append("|");

        } catch (Exception e) {
            logger.warn("Failed to collect hardware information", e);
            return "STABLE_FALLBACK_" + System.getProperty("user.name", "unknown") + "|";
        }

        return sb.toString();
    }

    private static String getMotherboardInfo() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "baseboard", "get", "SerialNumber"}
                );
                String output = readProcessOutput(process);
                String serial = extractWmicValue(output, "SerialNumber");
                if (!serial.isEmpty()) {
                    return "MB_SN_" + serial.hashCode();
                }
                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "baseboard", "get", "Product"}
                );
                output = readProcessOutput(process);
                String product = extractWmicValue(output, "Product");
                if (!product.isEmpty()) {
                    return "MB_PRODUCT_" + product.hashCode();
                }
                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "baseboard", "get", "Manufacturer"}
                );
                output = readProcessOutput(process);
                String manufacturer = extractWmicValue(output, "Manufacturer");
                if (!manufacturer.isEmpty()) {
                    return "MB_MANUF_" + manufacturer.hashCode();
                }
            } else {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"dmidecode", "-s", "baseboard-serial-number"}
                );
                String output = readProcessOutput(process);
                if (output != null && !output.trim().isEmpty()) {
                    return "MB_" + output.trim().hashCode();
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting motherboard info", e);
        }
        return "MB_FALLBACK";
    }

    private static String getCPUInfo() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "cpu", "get", "ProcessorId"}
                );
                String output = readProcessOutput(process);
                String processorId = extractWmicValue(output, "ProcessorId");
                if (!processorId.isEmpty()) {
                    return "CPU_ID_" + processorId.hashCode();
                }
                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "cpu", "get", "Name"}
                );
                output = readProcessOutput(process);
                String cpuName = extractWmicValue(output, "Name");
                if (!cpuName.isEmpty()) {
                    return "CPU_NAME_" + cpuName.hashCode();
                }
            } else {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"dmidecode", "-s", "processor-version"}
                );
                String output = readProcessOutput(process);
                if (output != null && !output.trim().isEmpty()) {
                    return "CPU_" + output.trim().hashCode();
                }
            }
            String processorId = System.getenv("PROCESSOR_IDENTIFIER");
            if (processorId != null) {
                return "CPU_ENV_" + processorId.hashCode();
            }

        } catch (Exception e) {
            logger.debug("Error getting CPU info", e);
        }

        return "CPU_FALLBACK";
    }

    private static String getDiskInfo() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "diskdrive", "get", "SerialNumber"}
                );
                String output = readProcessOutput(process);
                String serial = extractWmicValue(output, "SerialNumber");
                if (!serial.isEmpty()) {
                    return "DISK_SN_" + serial.hashCode();
                }
                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "diskdrive", "get", "Model"}
                );
                output = readProcessOutput(process);
                String model = extractWmicValue(output, "Model");
                if (!model.isEmpty()) {
                    return "DISK_MODEL_" + model.hashCode();
                }

                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "diskdrive", "get", "Manufacturer"}
                );
                output = readProcessOutput(process);
                String manufacturer = extractWmicValue(output, "Manufacturer");
                if (!manufacturer.isEmpty()) {
                    return "DISK_MANUF_" + manufacturer.hashCode();
                }

                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "diskdrive", "get", "InterfaceType"}
                );
                output = readProcessOutput(process);
                String interfaceType = extractWmicValue(output, "InterfaceType");
                if (!interfaceType.isEmpty()) {
                    return "DISK_IFACE_" + interfaceType.hashCode();
                }
            } else {
                try {
                    Process process = Runtime.getRuntime().exec(
                            new String[]{"lsblk", "-d", "-o", "SERIAL", "-n"}
                    );
                    String output = readProcessOutput(process);
                    if (output != null && !output.trim().isEmpty()) {
                        return "DISK_" + output.trim().hashCode();
                    }
                } catch (Exception e) {
                    logger.debug("Error getting disk info via lsblk", e);
                }
                try {
                    Process process = Runtime.getRuntime().exec(
                            new String[]{"udevadm", "info", "--query=property", "--name=sda"}
                    );
                    String output = readProcessOutput(process);
                    if (output != null && output.contains("ID_SERIAL")) {
                        String[] lines = output.split("\n");
                        for (String line : lines) {
                            if (line.startsWith("ID_SERIAL=")) {
                                return "DISK_SERIAL_" + line.substring(10).hashCode();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error getting disk info via udevadm", e);
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting disk info", e);
        }
        return "DISK_FIXED_BACKUP";
    }

    private static String extractWmicValue(String output, String fieldName) {
        if (output == null) return "";

        try {
            String[] lines = output.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.equals(fieldName)) {
                    continue;
                }
                if (!line.isEmpty()) {
                    return line;
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting WMIC value", e);
        }

        return "";
    }

    private static String readProcessOutput(Process process) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatHWID(byte[] hash) {
        StringBuilder hwid = new StringBuilder();
        String hexString = bytesToHex(hash);
        int neededLength = 15 * 3;
        if (hexString.length() < neededLength) {
            StringBuilder extended = new StringBuilder(hexString);
            while (extended.length() < neededLength) {
                extended.append(hexString);
            }
            hexString = extended.toString();
        }
        for (int i = 0; i < 45; i += 3) {
            if (i > 0) {
                hwid.append("-");
            }
            hwid.append(hexString.substring(i, i + 3).toUpperCase());
        }

        return hwid.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void logHWID() {
        String hwid = getHWID();
        String enhancedHwid = getEnhancedHWID();
        String boundQQ = getBoundQQ();
    }

    public static void main(String[] args) {
        logHWID();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}