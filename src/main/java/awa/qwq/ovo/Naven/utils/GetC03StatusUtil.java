package awa.qwq.ovo.Naven.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;

public class GetC03StatusUtil {
    public static final GetC03StatusUtil INSTANCE = new GetC03StatusUtil();
    public static int noMovePackets = 0;

    private static final Minecraft mc = Minecraft.getInstance();

    public static void packetEvent(Object packet) {
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
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
