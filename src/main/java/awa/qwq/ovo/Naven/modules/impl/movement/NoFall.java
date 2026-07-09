package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.PacketUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@ModuleInfo(name = "NoFall", category = Category.MOVEMENT, description = "Prevent fall damage")
public class NoFall extends Module {

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Elytra", "LagPos")
            .build()
            .getModeValue();

    private final FloatValue fallDistance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(3.0f)
            .setMaxFloatValue(15.0f)
            .build()
            .getFloatValue();

    private final FloatValue lagTick = ValueBuilder.create(this, "Lag Tick")
            .setDefaultFloatValue(5.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(21.0f)
            .build()
            .getFloatValue();

    private final FloatValue lagOffset = ValueBuilder.create(this, "Lag Offset")
            .setDefaultFloatValue(1000.0f)
            .setFloatStep(100.0f)
            .setMinFloatValue(100.0f)
            .setMaxFloatValue(10000.0f)
            .build()
            .getFloatValue();

    @Getter
    private double previousFallDistance;
    private boolean isLagged = false;
    private boolean handleFall = false;
    private boolean sendLagPacket = false;
    private boolean jump = false;
    private int boostTick = 0;
    private double originalX, originalY, originalZ;  // 用于 LagPos 模式

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        isLagged = false;
        handleFall = false;
        sendLagPacket = false;
        jump = false;
        boostTick = 0;
        originalX = originalY = originalZ = 0;
    }

    private boolean shouldBlockJump() {
        return jump;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() == EventType.POST || mc.player == null) {
            return;
        }

        if (jump) {
            mc.options.jumpKey.setPressed(false);
        }

        previousFallDistance = mc.player.isOnGround() ? 0.0 : mc.player.fallDistance;

        if (isLagged && handleFall) {
            if (boostTick < (int) (float) lagTick.getCurrentValue()) {
                boostTick++;
            } else {
                jump = true;
                handleFall = false;
                isLagged = false;
                boostTick = 0;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(EventUpdate event) {
        if (shouldBlockJump() && mc.options != null) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {
        if (mc.player.isOnGround() && jump) {
            mc.player.jump();
            jump = false;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (jump) {
            event.setJump(true);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.POST) {
            return;
        }

        if (!handleFall && mc.player.fallDistance > fallDistance.getCurrentValue() && !event.isOnGround()) {
            handleFall = true;
            isLagged = false;
            sendLagPacket = false;
            boostTick = 0;
            originalX = mc.player.getX();
            originalY = mc.player.getY();
            originalZ = mc.player.getZ();
        }

        if (handleFall && mc.player.fallDistance < 3.0f) {
            event.setOnGround(false);

            if (!sendLagPacket) {
                String currentMode = mode.getCurrentMode();

                if (currentMode.equals("Elytra")) {
                    PacketUtils.sendQueued(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    PacketUtils.sendQueued(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                } else if (currentMode.equals("LagPos")) {
                    double offset = lagOffset.getCurrentValue();
                    PacketUtils.sendQueued(new PlayerMoveC2SPacket.PositionAndOnGround(
                            originalX + offset,
                            originalY,
                            originalZ,
                            false
                    ));
                    PacketUtils.sendQueued(new PlayerMoveC2SPacket.PositionAndOnGround(
                            originalX,
                            originalY,
                            originalZ,
                            false
                    ));
                }

                sendLagPacket = true;
                isLagged = true;
                boostTick = 0;
                jump = false;
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND) {
            if (handleFall && sendLagPacket && isLagged && event.getPacket() instanceof PlayerMoveC2SPacket) {
                event.setCancelled(true);
            }
        } else if (event.getType() == EventType.RECEIVE) {
            if (handleFall && isLagged && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                if (boostTick < (int) lagTick.getCurrentValue()) {
                    boostTick = (int) lagTick.getCurrentValue();
                }
            }
        }
    }
}