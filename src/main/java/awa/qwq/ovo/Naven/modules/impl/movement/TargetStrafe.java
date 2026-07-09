package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "Target Strafe",
        description = "Automatically circles around your Aura target",
        category = Category.MOVEMENT
)
public class TargetStrafe extends Module {

    public static TargetStrafe INSTANCE;
    public final FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(1.5F)
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(3.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public final FloatValue switchDelay = ValueBuilder.create(this, "Switch Delay")
            .setDefaultFloatValue(1000.0F)
            .setMinFloatValue(100.0F)
            .setMaxFloatValue(5000.0F)
            .setFloatStep(100.0F)
            .build()
            .getFloatValue();
    public final BooleanValue jumpKeyOnly = ValueBuilder.create(this, "Jump Key Only")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private int strafeDirection = 1;
    private Entity strafeTarget;

    private final TimeHelper switchTimer = new TimeHelper();
    private final TimeHelper collisionTimer = new TimeHelper();

    public TargetStrafe() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        strafeDirection = 1;
        strafeTarget = null;
        switchTimer.reset();
        collisionTimer.reset();
    }

    @Override
    public void onDisable() {
        strafeTarget = null;
        if (mc.player != null) {
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura == null || !aura.isEnabled()) {
            strafeTarget = null;
            return;
        }
        Entity auraTarget = Aura.target;
        if (auraTarget == null) {
            strafeTarget = null;
            return;
        }
        double distanceToTarget = mc.player.distanceTo(auraTarget);
        if (distanceToTarget > aura.attackRange.getCurrentValue() + 2.0F) {
            strafeTarget = null;
            return;
        }
        if (switchTimer.delay(switchDelay.getCurrentValue()) || strafeTarget == null) {
            strafeTarget = auraTarget;
            switchTimer.reset();
        }
        if (mc.player == null) return;

        Box playerBox = mc.player.getBoundingBox();
        boolean aboveVoid = isAboveVoid(playerBox.minX, playerBox.minY, playerBox.minZ) || isAboveVoid(playerBox.minX, playerBox.minY, playerBox.maxZ) || isAboveVoid(playerBox.maxX, playerBox.minY, playerBox.minZ) || isAboveVoid(playerBox.maxX, playerBox.minY, playerBox.maxZ);
        if ((aboveVoid || mc.player.horizontalCollision) && collisionTimer.delay(500)) {
            strafeDirection *= -1;
            collisionTimer.reset();
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null || strafeTarget == null) {
            return;
        }
        if (jumpKeyOnly.getCurrentValue() && !mc.options.jumpKey.isPressed()) {
            return;
        }
        boolean isMoving = MoveUtils.isMoving();
        if (!isMoving) {
            return;
        }
        Vec3d targetPos = strafeTarget.getPos();
        Vec3d playerPos = mc.player.getPos();
        double dx = playerPos.x - targetPos.x;
        double dz = playerPos.z - targetPos.z;
        double angle = Math.atan2(dz, dx);
        double strafeAngle = angle + (strafeDirection * Math.PI / 2);
        double targetMoveX = Math.cos(strafeAngle);
        double targetMoveZ = Math.sin(strafeAngle);
        double targetX = targetPos.x + targetMoveX * range.getCurrentValue();
        double targetZ = targetPos.z + targetMoveZ * range.getCurrentValue();
        double moveAngle = Math.atan2(targetZ - playerPos.z, targetX - playerPos.x);
        float requiredYaw = (float) Math.toDegrees(moveAngle);
        MoveUtils.correctionMovement(event, requiredYaw);
        if (event.getForward() == 0 && event.getStrafe() == 0) {
            event.setForward(1.0F);
        }

        float maxSpeed = 0.2873F;
        if (Math.abs(event.getForward()) > maxSpeed) {
            event.setForward(maxSpeed);
        }
        if (Math.abs(event.getStrafe()) > maxSpeed) {
            event.setStrafe(maxSpeed);
        }
    }

    private boolean isAboveVoid(double x, double y, double z) {
        if (mc.world == null) return false;
        for (int i = 1; i <= 5; i++) {
            if (!mc.world.getStatesInBox(new Box(x - 0.1, y - i, z - 0.1, x + 0.1, y - i + 0.5, z + 0.1)).iterator().hasNext()) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static int getStrafeDirection() {
        return INSTANCE != null ? INSTANCE.strafeDirection : 1;
    }

    public static Entity getStrafeTarget() {
        return INSTANCE != null ? INSTANCE.strafeTarget : null;
    }
}