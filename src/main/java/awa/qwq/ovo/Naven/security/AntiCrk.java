package awa.qwq.ovo.Naven.security;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.chat.ChatClient;
import linyanli1337.Loader;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AntiCrk {
    private static final Logger LOGGER = LogManager.getLogger("AntiCrk");
    private static final AtomicBoolean FAILED = new AtomicBoolean(false);
    private static final AtomicBoolean EARLY_CHECKED = new AtomicBoolean(false);
    private static final AtomicBoolean RUNTIME_CHECKED = new AtomicBoolean(false);
    private static final AtomicInteger ACCESS_COUNTER = new AtomicInteger();
    private static int tickCounter;

    private static final String[] JVM_PATTERNS = {
            "-javaagent", "-agentpath", "-agentlib:jdwp", "-xdebug", "jnihook", "jni-hook",
            "frida", "xposed", "substrate", "bytebuddy", "byteman", "jrebel", "hotswap",
            "async-profiler", "yourkit", "jdwp"
    };

    private static final String[] PROPERTY_PATTERNS = {
            "jnihook", "jni-hook", "frida", "xposed", "bytebuddy", "byteman", "jrebel",
            "recaf", "jd-gui", "jadx", "cfr", "fernflower"
    };

    private static final String[] PROBE_CLASSES = {
            "net.bytebuddy.agent.ByteBuddyAgent",
            "org.jboss.byteman.agent.Main",
            "de.robv.android.xposed.XposedBridge",
            "org.jire.arrowhead.Frida"
    };

    private AntiCrk() {
    }

    public static void verifyEarly() {
        if (!EARLY_CHECKED.compareAndSet(false, true) || isDevelopment()) {
            return;
        }

        checkJvmArguments();
        checkLaunchProperties();
        checkInjectedClasses();
    }

    public static void verifyRuntime() {
        if (!RUNTIME_CHECKED.compareAndSet(false, true) || isDevelopment()) {
            return;
        }

        verifyEarly();
        checkLoaderState();
        checkCodeSource();
        checkDuplicateCriticalClasses();
    }

    public static void verifyAccess() {
        if (isDevelopment()) {
            return;
        }
        if (FAILED.get()) {
            Runtime.getRuntime().halt(37);
        }
        if (!RUNTIME_CHECKED.get()) {
            verifyRuntime();
        }
        if ((ACCESS_COUNTER.incrementAndGet() & 511) == 0) {
            checkLoaderState();
        }
    }

    public static void tick() {
        if (isDevelopment()) {
            return;
        }
        if (++tickCounter < 200) {
            return;
        }
        tickCounter = 0;
        checkJvmArguments();
        checkLoaderState();
        checkDuplicateCriticalClasses();
    }

    private static void checkJvmArguments() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            String lower = normalize(arg);
            for (String pattern : JVM_PATTERNS) {
                if (lower.contains(pattern)) {
                    fail("blocked jvm argument: " + pattern);
                }
            }
        }
    }

    private static void checkLaunchProperties() {
        List<String> values = new ArrayList<>();
        add(values, System.getProperty("java.class.path"));
        add(values, System.getProperty("sun.java.command"));
        add(values, System.getProperty("jdk.module.path"));
        add(values, System.getenv("JAVA_TOOL_OPTIONS"));
        add(values, System.getenv("_JAVA_OPTIONS"));
        add(values, System.getenv("CLASSPATH"));
        add(values, System.getenv("PATH"));

        for (String value : values) {
            String lower = normalize(value);
            for (String pattern : PROPERTY_PATTERNS) {
                if (lower.contains(pattern)) {
                    fail("blocked launch property: " + pattern);
                }
            }
        }
    }

    private static void checkInjectedClasses() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String className : PROBE_CLASSES) {
            try {
                Class.forName(className, false, loader);
                fail("instrumentation class present: " + className);
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                fail("instrumentation probe failed: " + className);
            }
        }
    }

    private static void checkLoaderState() {
        try {
            if (!Loader.isNativeLoaded()) {
                fail("native loader missing");
            }
            if (!Loader.isVerified()) {
                fail("loader verification missing");
            }
        } catch (Throwable throwable) {
            fail("loader state check failed");
        }
    }

    private static void checkCodeSource() {
        URL naven = codeSource(Naven.class);
        URL verify = codeSource(VerifyClient.class);
        URL chat = codeSource(ChatClient.class);
        URL anti = codeSource(AntiCrk.class);
        if (naven == null || verify == null || chat == null || anti == null) {
            fail("missing code source");
        }
        if (!naven.equals(verify) || !naven.equals(chat) || !naven.equals(anti)) {
            fail("critical classes loaded from different sources");
        }
    }

    private static void checkDuplicateCriticalClasses() {
        checkDuplicate("awa/qwq/ovo/Naven/auth/VerifyClient.class");
        checkDuplicate("awa/qwq/ovo/Naven/chat/ChatClient.class");
        checkDuplicate("awa/qwq/ovo/Naven/security/AntiCrk.class");
        checkDuplicate("linyanli1337/Loader.class");
    }

    private static void checkDuplicate(String resource) {
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(resource);
            int count = 0;
            while (resources.hasMoreElements()) {
                resources.nextElement();
                count++;
                if (count > 1) {
                    fail("duplicate critical class: " + resource);
                }
            }
        } catch (Throwable throwable) {
            fail("duplicate class scan failed: " + resource);
        }
    }

    private static URL codeSource(Class<?> clazz) {
        try {
            CodeSource source = clazz.getProtectionDomain().getCodeSource();
            return source == null ? null : source.getLocation();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isDevelopment() {
        try {
            return FabricLoader.getInstance().isDevelopmentEnvironment();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void add(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static void fail(String reason) {
        if (!FAILED.compareAndSet(false, true)) {
            return;
        }
        LOGGER.error("[AntiCrk] {}", reason);
        System.err.println("[AntiCrk] " + reason);
        Runtime.getRuntime().halt(37);
    }
}
