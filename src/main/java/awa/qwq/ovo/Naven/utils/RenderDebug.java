package awa.qwq.ovo.Naven.utils;

import org.lwjgl.opengl.GL11;

public class RenderDebug {

    private static boolean enabled = false;
    private static long lastLogTime = 0;
    private static boolean warnedCoreProfile = false;
    // 检测是否在 Core Profile
    private static boolean isCoreProfile() {
        String version = GL11.glGetString(GL11.GL_VERSION);
        return version != null && version.contains("Core");
    }

    public static void logRenderState(String stage, String methodName) {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        if (now - lastLogTime < 100) return;
        lastLogTime = now;

        boolean coreProfile = isCoreProfile();
        ChatUtils.addChatMessage(String.format(
                "§7[RenderDebug] §3%s §7| §f%s §7| CoreProfile: %s",
                stage, methodName, coreProfile
        ));
    }

    public static void checkAlphaState(String caller) {
        if (!enabled) return;
        // 什么都不做，避免任何 GL 错误
        // 或者只输出一行日志
        ChatUtils.addChatMessage(String.format(
                "§7[RenderDebug] §e%s §7| AlphaTest skipped (Core Profile)",
                caller
        ));
    }

    public static void checkBlendState(String caller) {
        if (!enabled) return;

        try {
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            ChatUtils.addChatMessage(String.format(
                    "§7[RenderDebug] §e%s §7| Blend: %s",
                    caller, blend
            ));
        } catch (Exception e) {
            // 忽略
        }
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
        ChatUtils.addChatMessage(String.format(
                "§7[RenderDebug] §%s%s",
                enable ? "a" : "c",
                enable ? "开启" : "关闭"
        ));
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static boolean isEnabled() {
        return enabled;
    }
}