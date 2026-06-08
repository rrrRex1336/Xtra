package awa.qwq.ovo.Naven.modules.impl.Combat;

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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;

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
    private boolean wasWKeyPressed = false;

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.level == null) return;

        LivingEntity entity = (LivingEntity) event.getTarget();

        if (entity != null && entity.hurtTime >= hurtTime.getCurrentValue()) {
            String mode = modeValue.getCurrentMode();

            switch (mode) {
                case "LegitFast", "Cancel W" -> tick = 2;

                case "Packet" -> {
                    if (mc.player.isSprinting()) {
                        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    }
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

                    mc.player.setSprinting(true);
                    try {
                        mc.player.getClass().getMethod("setWasSprinting", boolean.class).invoke(mc.player, true);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null) return;

        if (modeValue.getCurrentMode().equals("Cancel W")) {
            if (tick == 2) {
                wasWKeyPressed = mc.options.keyUp.isDown();
                event.setForward(0.0f);
                tick = 1;
            } else if (tick == 1) {
                if (!wasWKeyPressed) {
                    event.setForward(1.0f);
                } else {
                    event.setForward(1.0f);
                }
                tick = 0;
                wasWKeyPressed = false;
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate eventUpdate) {
        setSuffix(modeValue.getCurrentMode());
        if (mc.player == null) return;

        if (modeValue.getCurrentMode().equals("LegitFast")) {
            if (tick == 2) {
                mc.player.setSprinting(false);
                tick = 1;
            } else if (tick == 1) {
                mc.player.setSprinting(true);
                tick = 0;
            }
        }
    }

    @Override
    public void onEnable() {
        tick = 0;
        wasWKeyPressed = false;
    }

    @Override
    public void onDisable() {
        tick = 0;
        if (mc.options != null && mc.options.keyUp != null) {
            if (!wasWKeyPressed) {
                mc.options.keyUp.setDown(true);
            } else {
                mc.options.keyUp.setDown(true);
            }
        }
        wasWKeyPressed = false;
    }
}