package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMove;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "StopMove", description = "Stops movement", category = Category.MOVEMENT)
public class StopMove extends Module {
    private Vec3 lockedVelocity = Vec3.ZERO;

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.lockedVelocity = mc.player.getDeltaMovement();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.lockedVelocity = Vec3.ZERO;
        super.onDisable();
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            mc.player.setDeltaMovement(this.lockedVelocity);
        }
    }

    @EventTarget
    public void onMove(EventMove event) {
        event.setX(this.lockedVelocity.x);
        event.setY(this.lockedVelocity.y);
        event.setZ(this.lockedVelocity.z);
    }
}
