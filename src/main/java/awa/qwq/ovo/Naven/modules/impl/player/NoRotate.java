package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@ModuleInfo(
        name = "NoRotate",
        description = "Prevents server from changing your rotation",
        category = Category.MISC
)
public class NoRotate extends Module {
    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Edit", "Packet")
            .build()
            .getModeValue();

    private float yaw, pitch;
    private boolean teleport;

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.RECEIVE) {
            setSuffix(mode.getCurrentMode());
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
                switch (mode.getCurrentMode()) {
                    case "Packet":
                        PlayerPositionLookS2CPacket newPacket = new PlayerPositionLookS2CPacket(packet.getX(), packet.getY(), packet.getZ(), mc.player.getYaw(), mc.player.getPitch(), packet.getFlags(), packet.getTeleportId());
                        event.setPacket(newPacket);
                        break;

                    case "Edit":
                        this.yaw = packet.getYaw();
                        this.pitch = packet.getPitch();
                        PlayerPositionLookS2CPacket editedPacket = new PlayerPositionLookS2CPacket(packet.getX(), packet.getY(), packet.getZ(), mc.player.getYaw(), mc.player.getPitch(), packet.getFlags(), packet.getTeleportId());
                        event.setPacket(editedPacket);
                        this.teleport = true;
                        break;
                }
            }
        } else if (event.getType() == EventType.SEND) {
            if (mode.isCurrentMode("Edit") && this.teleport &&
                    event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround rotPacket) {
                PlayerMoveC2SPacket.LookAndOnGround newRotPacket = new PlayerMoveC2SPacket.LookAndOnGround(this.yaw, this.pitch, rotPacket.isOnGround());
                event.setPacket(newRotPacket);
                this.teleport = false;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.teleport = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.teleport = false;
    }
}