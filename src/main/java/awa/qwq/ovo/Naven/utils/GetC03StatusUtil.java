package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * 从1.8.9的GetC03StatusUtil适配到现代版本
 * 保持相同的接口：GetC03StatusUtil.noMovePackets
 */
public class GetC03StatusUtil {
    public static final GetC03StatusUtil INSTANCE = new GetC03StatusUtil();
    public static int noMovePackets = 0;

    private static int lastMovementTick = 0;
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * 处理包事件（替换原来的packetEvent方法）
     * 对应1.8.9的：if (packet instanceof C03PacketPlayer)
     */
    public static void packetEvent(Object packet) {
        // 现代版本：ServerboundMovePlayerPacket = C03PacketPlayer
        if (packet instanceof ServerboundMovePlayerPacket) {
            noMovePackets = 0; // 移动包发送，重置计数器
            if (mc.player != null) {
                lastMovementTick = mc.player.tickCount;
            }
        }
    }

    /**
     * 每tick更新计数器
     */
    public static void update() {
        if (mc.player == null) {
            reset();
            return;
        }

        // 计算没有移动包的tick数
        if (lastMovementTick > 0) {
            noMovePackets = mc.player.tickCount - lastMovementTick;
        } else {
            noMovePackets = 999; // 表示很久没移动了
        }
    }

    /**
     * 重置计数器
     */
    public static void reset() {
        noMovePackets = 0;
        lastMovementTick = 0;
    }

    /**
     * 检查是否超过指定tick没有移动包
     */
    public static boolean hasNoMovementFor(int ticks) {
        return noMovePackets >= ticks;
    }

    /**
     * 是否应该更新位置（20tick = 1秒）
     */
    public static boolean shouldUpdatePosition() {
        return hasNoMovementFor(20);
    }
}