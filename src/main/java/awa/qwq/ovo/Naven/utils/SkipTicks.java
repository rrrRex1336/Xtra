package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class SkipTicks {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean positionUpdate = false;
    private static boolean active = false;
    private static int skipTickCounter = 0;
    private static int skipTicksCount = 0;

    // 添加一个 ThreadLocal 标志，标记当前正在发送 SkipTicks 的包
    public static final ThreadLocal<Boolean> isSendingStuckPacket = ThreadLocal.withInitial(() -> false);

    public static boolean tick() {
        if (active) {
            skipTickCounter++;
            if (skipTickCounter > skipTicksCount) {
                positionUpdate = false;
                skipTickCounter = 0;
                return false;
            }

            positionUpdate = true;

            if (mc.player != null && mc.getNetworkHandler() != null) {
                isSendingStuckPacket.set(true);  // 设置标志
                try {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()
                    ));
                } finally {
                    isSendingStuckPacket.set(false);  // 清除标志
                }
            }

            return true;
        }

        skipTickCounter = 0;
        positionUpdate = false;
        return false;
    }

    public static void skipTicks(float ticks) {
        active = true;
        skipTicksCount = (int) ticks;
        skipTickCounter = 0;
    }

    public static void dispatch() {
        active = false;
        skipTickCounter = 0;
        positionUpdate = false;
    }

    public static boolean isActive() {
        return active;
    }
}