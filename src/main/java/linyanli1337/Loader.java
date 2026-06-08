package linyanli1337;

import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class Loader {

    @Setter
    @Getter
    private static boolean verified = false;
    @Getter
    private static boolean nativeLoaded = false;
    public static native String getGitHubToken();
    public static native boolean verifyIntegrity();

    static {
        long startTime = System.currentTimeMillis();
        System.out.println("[LinYanLi1337] loading native library...");

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();

            String arch = (osArch.equals("x86_64") || osArch.equals("amd64")) ? "x86_64"
                    : osArch.equals("aarch64") ? "aarch64" : null;

            String libName = osName.contains("win") ? "windows.dll"
                    : osName.contains("nix") || osName.contains("nux") ? "linux.so"
                      : osName.contains("mac") ? "macos.dylib" : null;

            if (arch == null || libName == null) {
                throw new Error("Unsupported platform");
            }

            String resourcePath = "/linyanli1337/" + arch + "-" + libName;

            try (InputStream inputStream = Loader.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Native library not found");
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }

                File tempFile = Files.createTempFile("native_linyanli1337", ".tmp").toFile();
                tempFile.deleteOnExit();
                Files.write(tempFile.toPath(), baos.toByteArray());
                System.load(tempFile.getAbsolutePath());
            }

            nativeLoaded = true;
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("[LinYanLi1337] native library loaded in " + elapsed + "ms");

        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("[LinYanLi1337] Failed to load in " + elapsed + "ms: " + t.getMessage());
            nativeLoaded = false;
            verified = false;
        }
    }
}