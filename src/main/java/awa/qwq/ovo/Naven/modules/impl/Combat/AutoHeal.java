package awa.qwq.ovo.Naven.modules.impl.Combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.PacketUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "AutoHeal",
   description = "Automatically heals you when you're low on health.",
   category = Category.COMBAT
)
public class AutoHeal extends Module {
   private final TimeHelper timer = new TimeHelper();
   private final FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(500.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(300.0F)
      .setMaxFloatValue(1000.0F)
      .build()
      .getFloatValue();
   private final FloatValue health = ValueBuilder.create(this, "Health Percent")
      .setDefaultFloatValue(0.5F)
      .setFloatStep(0.05F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
   private final ModeValue mode = ValueBuilder.create(this, "Mode").setModes("Soup", "Head").setDefaultModeIndex(0).build().getModeValue();
   private boolean switchBack = true;
   private boolean useItem = true;
   private boolean throwItem = true;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (this.useItem) {
            PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
            this.useItem = false;
            return;
         }

         if (this.throwItem) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            this.throwItem = false;
            return;
         }

         if (this.switchBack) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
            this.switchBack = false;
            return;
         }

         if (!this.timer.delay((double)this.delay.getCurrentValue())) {
            return;
         }

         if (mc.player.getHealth() / mc.player.getMaxHealth() < this.health.getCurrentValue()) {
            if (this.mode.isCurrentMode("Soup")) {
               for (int i = 0; i < 9; i++) {
                  ItemStack stack = (ItemStack)mc.player.getInventory().items.get(i);
                  if (stack.getItem() == Items.MUSHROOM_STEW) {
                     this.switchUseItem(i, true);
                     this.switchBack = true;
                     break;
                  }
               }
            } else if (this.mode.isCurrentMode("Head")) {
               for (int ix = 0; ix < 9; ix++) {
                  ItemStack stack = (ItemStack)mc.player.getInventory().items.get(ix);
                  if (InventoryUtils.isGoldenHead(stack)) {
                     this.switchUseItem(ix, false);
                     this.switchBack = true;
                     break;
                  }
               }
            }
         }
      }
   }

   private void switchUseItem(int slot, boolean throwItem) {
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      this.throwItem = throwItem;
      this.useItem = true;
   }
}
