package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import net.minecraft.client.util.InputUtil;

@ModuleInfo(
   name = "SafeWalk",
   description = "Prevents you from falling off blocks",
   category = Category.MOVEMENT
)
public class SafeWalk extends Module {
   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.world
         .getCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, -0.5, 0.0).expand((double)(-sensitivity), 0.0, (double)(-sensitivity)))
         .iterator()
         .hasNext();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.options.sneakKey.setPressed(mc.player.isOnGround() && isOnBlockEdge(0.3F));
      }
   }

   @Override
   public void onDisable() {
      boolean isHoldingShift = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode());
      mc.options.sneakKey.setPressed(isHoldingShift);
   }
}
