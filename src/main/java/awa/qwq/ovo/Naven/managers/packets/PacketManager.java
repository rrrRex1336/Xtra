package awa.qwq.ovo.Naven.managers.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class PacketManager {
    private static final Minecraft mc = Minecraft.getInstance();

    public void sendC0BPacket() {
        if (mc.getConnection() != null && mc.player != null) {
            ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(
                    mc.player,
                    ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
            );
            mc.getConnection().send(packet);
        }

    }
    public void sendC09Packet() {
        if (mc.player != null && mc.getConnection() != null) {
            int current = mc.player.getInventory().selected;
            int next = (current + 1) % 9;
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(next));
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(current));
        }
    }
}