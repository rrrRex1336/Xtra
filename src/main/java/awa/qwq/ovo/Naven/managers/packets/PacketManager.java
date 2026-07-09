package awa.qwq.ovo.Naven.managers.packets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class PacketManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void sendC0BPacket() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            ClientCommandC2SPacket packet = new ClientCommandC2SPacket(
                    mc.player,
                    ClientCommandC2SPacket.Mode.STOP_SPRINTING
            );
            mc.getNetworkHandler().sendPacket(packet);
        }

    }
    public void sendC09Packet() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            int current = mc.player.getInventory().selectedSlot;
            int next = (current + 1) % 9;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(next));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(current));
        }
    }
}