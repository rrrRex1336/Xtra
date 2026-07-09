package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;

public class GetC03StatusUtil {
    public static final GetC03StatusUtil INSTANCE = new GetC03StatusUtil();
    public static int noMovePackets = 0;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void packetEvent(Object packet) {
        if (packet instanceof PlayerMoveC2SPacket movePacket) {
            boolean moving = ((ServerboundMovePlayerPacketAccessor) movePacket).hasPos();
            noMovePackets = moving ? 0 : noMovePackets + 1;
        }
    }

    public static void update() {
        if (mc.player == null) {
            reset();
        }
    }

    public static void reset() {
        noMovePackets = 0;
    }

    public static boolean hasNoMovementFor(int ticks) {
        return noMovePackets >= ticks;
    }

    public static boolean shouldUpdatePosition() {
        return hasNoMovementFor(20);
    }
}
