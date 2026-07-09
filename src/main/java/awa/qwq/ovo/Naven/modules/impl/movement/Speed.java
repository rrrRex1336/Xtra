package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.player.Blink;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

@ModuleInfo(
        name = "Speed",
        description = "Make you move speed faster",
        category = Category.MOVEMENT
)
public class Speed extends Module {
    private static final float DEFAULT_TICK_SPEED = 1.0F;
    private static final float SPEED_TICK_MULTIPLIER = 1.004f;
    private static final float FRICTION_MULTIPLIER = 1.002F;
    public static final Speed INSTANCE = new Speed();
    private double lastPosX, lastPosZ;
    private long lastUpdateTime;
    @Getter
    private double currentBPS = 0.0;
    private boolean wasJumping = false;

    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Collision")
            .build()
            .getModeValue();

    public final FloatValue grimAddHitBoxes = ValueBuilder.create(this, "Collision Hitbox Count")
            .setVisibility(() -> mode.isCurrentMode("Collision"))
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    public final FloatValue collisionBoost = ValueBuilder.create(this, "Collision Boost Velocity")
            .setVisibility(() -> mode.isCurrentMode("Collision"))
            .setDefaultFloatValue(0.08F)
            .setMinFloatValue(0.01F)
            .setMaxFloatValue(0.5F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    public BooleanValue autoJump = ValueBuilder.create(this, "Auto Jump")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private boolean jump;

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            lastPosX = mc.player.getX();
            lastPosZ = mc.player.getZ();
        }
        lastUpdateTime = System.currentTimeMillis();
        if (autoJump.getCurrentValue())
            jump = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (wasJumping && mc.options.jumpKey != null) {
            KeyBinding.setKeyPressed(mc.options.jumpKey.getDefaultKey(), false);
            wasJumping = false;
        }
    }

    private boolean shouldDisableSpeed() {
        if (Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            try {
                Blink blinkModule = (Blink) Naven.getInstance().getModuleManager().getModule(Blink.class);
                if (blinkModule != null && blinkModule.isEnabled()) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        if (mc.player != null) {
            if (mc.player.isTouchingWater() || mc.player.isInLava()) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (!isEnabled() || event.getType() != EventType.PRE)
            return;
        if (shouldDisableSpeed()) {
            updateBPS();
            this.setSuffix(String.format("%.2f (Disabled)", currentBPS));
            return;
        }
        if (mode.isCurrentMode("Collision")) {
            if (mc.player == null || mc.world == null) return;
            boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() ||
                    mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
            if (!isMoving) return;
            int collisions = 0;
            Box playerBox = mc.player.getBoundingBox();
            double shrinkValue = grimAddHitBoxes.getCurrentValue() / 10.0;
            for (Entity entity : mc.world.getEntities()) {
                if (entity == null || entity == mc.player || !(entity instanceof LivingEntity)) continue;
                Box entityBox = entity.getBoundingBox().contract(shrinkValue, 0.0, shrinkValue);
                if (playerBox.intersects(entityBox)) {
                    collisions++;
                }
            }
            if (collisions > 0) {
                float yaw = mc.player.getYaw();
                double radYaw = Math.toRadians(yaw);
                double moveX = -Math.sin(radYaw) * mc.player.forwardSpeed + Math.cos(radYaw) * mc.player.sidewaysSpeed;
                double moveZ = Math.cos(radYaw) * mc.player.forwardSpeed + Math.sin(radYaw) * mc.player.sidewaysSpeed;
                double angle = Math.atan2(moveX, moveZ);
                float rotationYaw = (float) angle;
                double boost = collisionBoost.getCurrentValue() * collisions;
                mc.player.setVelocity(
                        mc.player.getVelocity().x + Math.sin(rotationYaw) * boost,
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z + Math.cos(rotationYaw) * boost
                );
            }
        }

        updateBPS();
        this.setSuffix(String.format("%.2f", currentBPS));
    }

    private void updateBPS() {
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastUpdateTime;
        if (timeDiff > 0) {
            double deltaX = mc.player.getX() - lastPosX;
            double deltaZ = mc.player.getZ() - lastPosZ;
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            double seconds = timeDiff / 1000.0;
            currentBPS = seconds > 0 ? horizontalDistance / seconds : 0;
            lastPosX = mc.player.getX();
            lastPosZ = mc.player.getZ();
            lastUpdateTime = currentTime;
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (!isEnabled())
            return;
        if (shouldDisableSpeed()) {
            return;
        }

        if (autoJump.getCurrentValue())
            if (jump) {
                if (mc.player != null && mc.player.isOnGround() && MoveUtils.isMoving()) event.setJump(true);
            }
    }

    public int getJumpCooldown() {
        int jumpCooldown = 0;
        return jumpCooldown;
    }
}