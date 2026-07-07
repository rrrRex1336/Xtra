package awa.qwq.ovo.Naven.utils;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class HWIDUtil {
    private static final String OPEN_PLATFORM_SALT = "ForeverHack-6c6f8bac-f215-46ed-88cd-fe33ed4b42f3-";
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hardware = systemInfo.getHardware();

    public static String generateHWID() {
        String motherboardId = getBaseboardSerial();
        String cpuId = getProcessorId();
        String diskId = getDiskSerial();

        String combined = motherboardId + "|" + cpuId + "|" + diskId;
        return sha256Hash(combined);
    }

    public static String generateOpenPlatformHWID(String token) {
        if (token == null || token.isBlank()) {
            return generateHWID();
        }

        try {
            CentralProcessor processor = hardware.getProcessor();
            String processorName = processor.getProcessorIdentifier().getName();
            String processorIdentifier = processor.getProcessorIdentifier().getIdentifier();
            String osFamily = systemInfo.getOperatingSystem().getFamily();
            String input = OPEN_PLATFORM_SALT
                    + token
                    + "-" + safe(processorName)
                    + "-" + safe(processorIdentifier)
                    + "-" + safe(osFamily);
            return token + ":" + sha256Hash(input);
        } catch (Exception e) {
            return token + ":" + sha256Hash(OPEN_PLATFORM_SALT + token + "-" + generateHWID());
        }
    }

    private static String getBaseboardSerial() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            String serial = computerSystem.getBaseboard().getSerialNumber();
            return serial != null ? serial : "UNKNOWN_BASEBOARD";
        } catch (Exception e) {
            return "ERROR_BASEBOARD";
        }
    }

    private static String getProcessorId() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            String id = processor.getProcessorIdentifier().getProcessorID();
            return id != null ? id : "UNKNOWN_CPU";
        } catch (Exception e) {
            return "ERROR_CPU";
        }
    }

    private static String getDiskSerial() {
        try {
            List<HWDiskStore> diskStores = hardware.getDiskStores();
            if (!diskStores.isEmpty()) {
                String serial = diskStores.get(0).getSerial();
                return serial != null ? serial : "UNKNOWN_DISK";
            }
            return "NO_DISKS";
        } catch (Exception e) {
            return "ERROR_DISK";
        }
    }

    private static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
