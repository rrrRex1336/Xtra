package awa.qwq.ovo.Naven.utils;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.packet.Packet;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class BlinkUtils {
    private static final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private static boolean blinking = false;
    private static final Map<Class<?>, Predicate<Packet<?>>> cancelReturnPredicates = new HashMap<>();
    private static final Map<Class<?>, Consumer<Packet<?>>> cancelActions = new HashMap<>();
    private static final Map<Class<?>, Consumer<Packet<?>>> releaseActions = new HashMap<>();
    private static final Map<Class<?>, Predicate<Packet<?>>> releaseReturnPredicates = new HashMap<>();
    private static final List<Class<?>> blackList = new ArrayList<>();
    private static final List<Class<?>> whiteList = new ArrayList<>();
    private static final Map<Class<?>, Predicate<Packet<?>>> addReturnPredicates = new HashMap<>();
    private static boolean passEvent = false;

    public static void blink(Class<?>... packetsToBlink) {
        blinking = true;
        packets.clear();
        blackList.clear();
        blackList.addAll(Arrays.asList(packetsToBlink));
    }

    public static void stopBlink() {
        blinking = false;
        releasePackets();
        resetBlackList();
    }

    public static boolean isBlinking() {
        return blinking;
    }

    public static void setCancelReturnPredicate(Class<?> packetClass, Predicate<Packet<?>> predicate) {
        cancelReturnPredicates.put(packetClass, predicate);
    }

    public static void setCancelAction(Class<?> packetClass, Consumer<Packet<?>> action) {
        cancelActions.put(packetClass, action);
    }

    public static void setReleaseAction(Class<?> packetClass, Consumer<Packet<?>> action) {
        releaseActions.put(packetClass, action);
    }

    public static void setReleaseReturnPredicate(Class<?> packetClass, Predicate<Packet<?>> predicate) {
        releaseReturnPredicates.put(packetClass, predicate);
    }

    public static void setAddReturnPredicate(Class<?> packetClass, Predicate<Packet<?>> predicate) {
        addReturnPredicates.put(packetClass, predicate);
    }

    public static void addWhiteList(Class<?>... classes) {
        whiteList.addAll(Arrays.asList(classes));
    }

    public static void removeBlackList(Class<?> packetClazz) {
        blackList.remove(packetClazz);
    }

    public static void resetBlackList() {
        blackList.clear();
        whiteList.clear();
        cancelReturnPredicates.clear();
        cancelActions.clear();
        releaseActions.clear();
        releaseReturnPredicates.clear();
        addReturnPredicates.clear();
    }

    public static void sendPacket(Packet<?> packet, boolean immediate) {
        if (immediate) {
            NetworkUtils.sendPacket(packet);
        } else {
            packets.add(packet);
        }
    }

    public static void releasePacket(boolean immediate) {
        if (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();
            if (packet != null) {
                Predicate<Packet<?>> predicate = releaseReturnPredicates.get(packet.getClass());
                if (predicate != null && predicate.test(packet)) {
                    return;
                }
                Consumer<Packet<?>> action = releaseActions.get(packet.getClass());
                if (action != null) {
                    action.accept(packet);
                }

                if (immediate) {
                    NetworkUtils.sendPacket(packet);
                }
            }
        }
    }

    public static void releasePackets() {
        while (!packets.isEmpty()) {
            Packet<?> packet = packets.poll();
            NetworkUtils.sendPacket(packet);
        }
    }

    public static void releasePacket(int count, boolean immediate) {
        for (int i = 0; i < count && !packets.isEmpty(); i++) {
            releasePacket(immediate);
        }
    }

    public static boolean handlePacket(Packet<?> packet) {
        if (!blinking || passEvent) {
            return false;
        }
        Consumer<Packet<?>> action = cancelActions.get(packet.getClass());
        if (action != null) {
            action.accept(packet);
        }
        for (Class<?> clazz : blackList) {
            if (clazz.isAssignableFrom(packet.getClass())) {
                return true;
            }
        }
        Predicate<Packet<?>> predicate = cancelReturnPredicates.get(packet.getClass());
        if (predicate != null && predicate.test(packet)) {
            return true;
        }
        if (!whiteList.isEmpty() && !whiteList.contains(packet.getClass())) {
            return true;
        }
        boolean needAdd = true;
        Predicate<Packet<?>> addPredicate = addReturnPredicates.get(packet.getClass());
        if (addPredicate != null && addPredicate.test(packet)) {
            needAdd = false;
        }

        if (needAdd) {
            packets.add(packet);
        }

        return false;
    }

    public static int getPacketCount() {
        return packets.size();
    }

    public static void setPassEvent(boolean pass) {
        passEvent = pass;
    }
}