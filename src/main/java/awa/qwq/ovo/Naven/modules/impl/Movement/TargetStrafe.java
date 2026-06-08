package awa.qwq.ovo.Naven.modules.impl.Movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Combat.KillAura;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.world.entity.Entity;

@ModuleInfo(
        name = "Target Strafe",
        description = "Automatically moves forward to follow the Aura target(Maybe failed)",
        category = Category.MOVEMENT
)
public class TargetStrafe extends Module {
    public FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(5.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    public FloatValue switchDelay = ValueBuilder.create(this, "Switch Delay")
            .setDefaultFloatValue(1000.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10000.0F)
            .setFloatStep(100.0F)
            .build()
            .getFloatValue();

    private Entity currentTarget = null;
    private long lastTargetSwitchTime = 0;

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura == null || !killAura.isEnabled()) {
            currentTarget = null;
            return;
        }
        float auraAimRange = killAura.aimRange.getCurrentValue();
        Entity auraTarget = KillAura.getTarget();
        if (auraTarget == null) {
            currentTarget = null;
            return;
        }
        float distanceToTarget = mc.player.distanceTo(auraTarget);
        boolean withinAimRange = distanceToTarget <= auraAimRange;

        boolean shouldIgnoreRange = false;
        if (!shouldIgnoreRange && !withinAimRange) {
            currentTarget = null;
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTarget == null || currentTarget != auraTarget) {
            if (currentTime - lastTargetSwitchTime >= switchDelay.getCurrentValue()) {
                currentTarget = auraTarget;
                lastTargetSwitchTime = currentTime;
            } else {
                return;
            }
        }

        event.setForward(1.0F);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
        this.setSuffix(null);
    }
}