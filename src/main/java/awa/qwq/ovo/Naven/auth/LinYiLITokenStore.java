package awa.qwq.ovo.Naven.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

final class LinYiLITokenStore {

    private static final int VERSION = 1;
    private static final byte[] PEPPER = {
        0x4C, 0x59, 0x4C, 0x54, 0x6F, 0x6B, 0x65, 0x6E,
        0x53, 0x74, 0x6F, 0x72, 0x65, 0x56, 0x31, 0x00
    };

    private static final Path TOKEN_FILE = VerifyClient.KEY_FILE.getParent().resolve(".auth-session");
    private static final Path PENDING_FILE = VerifyClient.KEY_FILE.getParent().resolve(".weblogin-pending");
    private static final Path LEGACY_TOKEN_FILE = VerifyClient.LEGACY_KEY_FILE.getParent().resolve(".auth-session");
    private static final Path LEGACY_PENDING_FILE = VerifyClient.LEGACY_KEY_FILE.getParent().resolve(".weblogin-pending");

    private LinYiLITokenStore() {}

    static void saveToken(String token, long expiryEpoch) {
        try {
            String payload = token + "|" + expiryEpoch;
            byte[] salt = randomBytes(16);
            byte[] iv   = randomBytes(12);
            byte[] key  = deriveKey(salt);
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(xorLayer(data, key, iv));

            ByteBuffer buf = ByteBuffer.allocate(1 + salt.length + iv.length + encrypted.length);
            buf.put((byte) VERSION).put(salt).put(iv).put(encrypted);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());

            Files.createDirectories(TOKEN_FILE.getParent());
            Files.writeString(TOKEN_FILE, encoded, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    static String loadToken() {
        String[] parts = load();
        return parts != null ? parts[0] : "";
    }

    static long loadExpiry() {
        String[] parts = load();
        if (parts != null) {
            try { return Long.parseLong(parts[1]); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    static boolean tokenExists() {
        return Files.isRegularFile(TOKEN_FILE)
                || (VerifyClient.isLegacyStorageEnabled() && Files.isRegularFile(LEGACY_TOKEN_FILE));
    }

    static void clearToken() {
        try { Files.deleteIfExists(TOKEN_FILE); } catch (Exception ignored) {}
        if (VerifyClient.isLegacyStorageEnabled()) {
            try { Files.deleteIfExists(LEGACY_TOKEN_FILE); } catch (Exception ignored) {}
        }
    }

    // ── Pending web login state ───────────────────────────────────────────────

    static void savePending(String code) {
        try {
            Files.createDirectories(PENDING_FILE.getParent());
            Files.writeString(PENDING_FILE, code + "|" + System.currentTimeMillis(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    static String loadPendingCode() {
        String code = loadPendingCode(PENDING_FILE);
        if (!code.isEmpty()) return code;
        if (!VerifyClient.isLegacyStorageEnabled()) return "";
        code = loadPendingCode(LEGACY_PENDING_FILE);
        if (!code.isEmpty()) {
            savePending(code);
            try { Files.deleteIfExists(LEGACY_PENDING_FILE); } catch (Exception ignored) {}
        }
        return code;
    }

    private static String loadPendingCode(Path file) {
        try {
            if (!Files.isRegularFile(file)) return "";
            String raw = Files.readString(file, StandardCharsets.UTF_8).trim();
            int sep = raw.lastIndexOf('|');
            if (sep < 0) return raw;
            String code = raw.substring(0, sep);
            long ts = Long.parseLong(raw.substring(sep + 1));
            if (System.currentTimeMillis() - ts > 5L * 60_000L) {
                Files.deleteIfExists(file);
                return "";
            }
            return code;
        } catch (Exception e) {
            return "";
        }
    }

    static void clearPending() {
        try { Files.deleteIfExists(PENDING_FILE); } catch (Exception ignored) {}
        if (VerifyClient.isLegacyStorageEnabled()) {
            try { Files.deleteIfExists(LEGACY_PENDING_FILE); } catch (Exception ignored) {}
        }
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private static String[] load() {
        String[] current = load(TOKEN_FILE);
        if (current != null) return current;
        if (!VerifyClient.isLegacyStorageEnabled()) return null;

        String[] legacy = load(LEGACY_TOKEN_FILE);
        if (legacy != null) {
            try {
                saveToken(legacy[0], Long.parseLong(legacy[1]));
            } catch (NumberFormatException ignored) {}
        }
        return legacy;
    }

    private static String[] load(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            String encoded = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (encoded.isEmpty()) return null;

            byte[] raw = Base64.getUrlDecoder().decode(encoded);
            ByteBuffer buf = ByteBuffer.wrap(raw);
            int version = Byte.toUnsignedInt(buf.get());
            if (version != VERSION || buf.remaining() < 28) return null;

            byte[] salt = new byte[16]; buf.get(salt);
            byte[] iv   = new byte[12]; buf.get(iv);
            byte[] encrypted = new byte[buf.remaining()]; buf.get(encrypted);

            byte[] key = deriveKey(salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] data = xorLayer(cipher.doFinal(encrypted), key, iv);
            String payload = new String(data, StandardCharsets.UTF_8);

            int sep = payload.indexOf('|');
            if (sep <= 0) return null;
            return new String[]{ payload.substring(0, sep), payload.substring(sep + 1) };
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] deriveKey(byte[] salt) throws Exception {
        byte[] machine = HWIDCheck.getHWID().getBytes(StandardCharsets.UTF_8);
        byte[] mixed   = new byte[PEPPER.length + machine.length];
        System.arraycopy(PEPPER, 0, mixed, 0, PEPPER.length);
        System.arraycopy(machine, 0, mixed, PEPPER.length, machine.length);
        PBEKeySpec spec = new PBEKeySpec(hexEncode(mixed).toCharArray(), salt, 20_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] xorLayer(byte[] data, byte[] key, byte[] salt) {
        byte[] out = Arrays.copyOf(data, data.length);
        for (int i = 0; i < out.length; i++) out[i] ^= key[i % key.length] ^ salt[i % salt.length];
        return out;
    }

    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}
