package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.player.InventoryManager;
import awa.qwq.ovo.Naven.utils.MoveUtils;

@ModuleInfo(
        name = "Sprint",
        description = "Automatically sprints",
        category = Category.MOVEMENT
)
public class Sprint extends Module {
    @EventTarget(0)
    public void onMotion(EventMotion e) {

        if (e.getType() == EventType.PRE || MoveUtils.isMoving()) {
            mc.options.keySprint.setDown(true);
            mc.options.toggleSprint().set(false);
            if (InventoryMove.shouldStopSprintForSprintModule() || InventoryManager.shouldStopSprintForSprintModule()) {
                mc.options.keySprint.setDown(false);
                if (mc.player != null && mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    mc.options.keySprint.setDown(false);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        mc.options.keySprint.setDown(false);
    }
}
