package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

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
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
                switch (mode.getCurrentMode()) {
                    case "Packet":
                        ClientboundPlayerPositionPacket newPacket = new ClientboundPlayerPositionPacket(packet.getX(), packet.getY(), packet.getZ(), mc.player.getYRot(), mc.player.getXRot(), packet.getRelativeArguments(), packet.getId());
                        event.setPacket(newPacket);
                        break;

                    case "Edit":
                        this.yaw = packet.getYRot();
                        this.pitch = packet.getXRot();
                        ClientboundPlayerPositionPacket editedPacket = new ClientboundPlayerPositionPacket(packet.getX(), packet.getY(), packet.getZ(), mc.player.getYRot(), mc.player.getXRot(), packet.getRelativeArguments(), packet.getId());
                        event.setPacket(editedPacket);
                        this.teleport = true;
                        break;
                }
            }
        } else if (event.getType() == EventType.SEND) {
            if (mode.isCurrentMode("Edit") && this.teleport &&
                    event.getPacket() instanceof ServerboundMovePlayerPacket.Rot rotPacket) {
                ServerboundMovePlayerPacket.Rot newRotPacket = new ServerboundMovePlayerPacket.Rot(this.yaw, this.pitch, rotPacket.isOnGround());
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