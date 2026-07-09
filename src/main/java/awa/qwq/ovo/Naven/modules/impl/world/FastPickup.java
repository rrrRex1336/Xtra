package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import net.minecraft.entity.ItemEntity;
import org.mixin.accessors.ItemEntityAccessor;

@ModuleInfo(name = "FastPickup", description = "Makes your pick fall item than faster.", category = Category.WORLD)
public class FastPickup extends Module {

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.type() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;

        for (ItemEntity itemEntity : mc.world.getNonSpectatingEntities(ItemEntity.class, mc.player.getBoundingBox().expand(5.0))) {
            if (!itemEntity.isAlive()) continue;

            ItemEntityAccessor accessor = (ItemEntityAccessor) itemEntity;
            int currentDelay = accessor.getPickupDelay();
            if (currentDelay > 0) {
                int newDelay = Math.max(1, currentDelay / 2);
                accessor.setPickupDelay(newDelay);
            }
        }
    }
}