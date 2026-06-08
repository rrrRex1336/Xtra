package awa.qwq.ovo.Naven.components;

import awa.qwq.ovo.Naven.utils.NetworkUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkComponent {
    private static final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private static boolean blinking = false;

    // 白名单包类型
    private static final Set<Class<?>> whitelist = new HashSet<Class<?>>() {{
        add(net.minecraft.network.protocol.handshake.ClientIntentionPacket.class);
        add(net.minecraft.network.protocol.status.ServerboundStatusRequestPacket.class);
        add(net.minecraft.network.protocol.status.ServerboundPingRequestPacket.class);
        add(net.minecraft.network.protocol.login.ServerboundHelloPacket.class);
        add(net.minecraft.network.protocol.login.ServerboundKeyPacket.class);
        add(net.minecraft.network.protocol.game.ServerboundUseItemPacket.class);
        add(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket.class);
    }};

    public static void startBlink() {
        blinking = true;
        packets.clear();
    }

    public static void stopBlink() {
        blinking = false;
        releasePackets();
    }

    public static boolean isBlinking() {
        return blinking;
    }

    public static void addPacket(Packet<?> packet) {
        if (blinking) {
            // 白名单包直接发送
            if (whitelist.contains(packet.getClass())) {
                NetworkUtils.sendPacket(packet);
                return;
            }

            // 只拦截移动包
            if (packet instanceof ServerboundMovePlayerPacket) {
                packets.add(packet);
            } else {
                // 其他包直接发送
                NetworkUtils.sendPacket(packet);
            }
        } else {
            NetworkUtils.sendPacket(packet);
        }
    }

    public static void releasePackets() {
        while (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();
            NetworkUtils.sendPacket(packet);
        }
    }

    public static void releaseSomePackets(int count) {
        for (int i = 0; i < count && !packets.isEmpty(); i++) {
            Packet<?> packet = packets.poll();
            if (packet != null) {
                NetworkUtils.sendPacket(packet);
            }
        }
    }

    public static int getPacketsCount() {
        return packets.size();
    }

    public static long getBlinkTicks() {
        return packets.stream().filter(packet -> packet instanceof ServerboundMovePlayerPacket).count();
    }
}