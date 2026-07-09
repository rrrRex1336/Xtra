package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@ModuleInfo(
        name = "ExtraKB",
        description = "Make your attack target knock back further.",
        category = Category.COMBAT
)
public class ExtraKB extends Module {
    private final ModeValue modeValue = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(1)
            .setModes("Cancel W", "LegitFast", "Packet")
            .build()
            .getModeValue();

    private final FloatValue hurtTime = ValueBuilder.create(this, "HurtTime")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    public int tick;
    private int sprintTicks;
    private boolean restoreForward;
    private int lastApplyTick = -1;

    @Override
    public void onEnable() {
        this.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.reset();
        super.onDisable();
    }

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (target.hurtTime < this.hurtTime.getCurrentValue() || this.lastApplyTick == mc.player.age) {
            return;
        }

        this.lastApplyTick = mc.player.age;
        switch (this.modeValue.getCurrentMode()) {
            case "Cancel W" -> {
                this.tick = 2;
                this.restoreForward = mc.options.forwardKey.isPressed();
            }
            case "LegitFast" -> this.sprintTicks = 2;
            case "Packet" -> this.sendSprintReset();
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (!this.modeValue.isCurrentMode("Cancel W") || this.tick <= 0) {
            return;
        }

        if (this.tick == 2) {
            event.setForward(0.0F);
        } else if (this.restoreForward) {
            event.setForward(1.0F);
        }
        --this.tick;
        if (this.tick <= 0) {
            this.restoreForward = false;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(this.modeValue.getCurrentMode());
        if (mc.player == null || !this.modeValue.isCurrentMode("LegitFast") || this.sprintTicks <= 0) {
            return;
        }

        if (this.sprintTicks == 2) {
            mc.player.setSprinting(false);
        } else {
            mc.player.setSprinting(true);
        }
        --this.sprintTicks;
    }

    private void sendSprintReset() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (mc.player.isSprinting()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        mc.player.setSprinting(true);
    }

    private void reset() {
        this.tick = 0;
        this.sprintTicks = 0;
        this.restoreForward = false;
        this.lastApplyTick = -1;
    }
}
