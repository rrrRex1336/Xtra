package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventUpdateHeldItem;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.BedAura;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.*;

@ModuleInfo(
        name = "AutoTools",
        description = "Automatically switches to the best tool for mining and combat",
        category = Category.PLAYER
)
public class AutoTools extends Module {
   private final BooleanValue checkSword = ValueBuilder.create(this, "Check Sword").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue switchBack = ValueBuilder.create(this, "Switch Back").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue silent = ValueBuilder.create(this, "Silent")
           .setVisibility(this.switchBack::getCurrentValue)
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   private int originSlot = -1;
   private BedAura bedAura = null;

   private boolean shouldWork() {
      if (this.isEnabled()) return true;
      if (bedAura == null) {
         bedAura = (BedAura) Naven.getInstance().getModuleManager().getModule(BedAura.class);
      }
      return bedAura != null && bedAura.isEnabled() && bedAura.autoTools.getCurrentValue();
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (!shouldWork()) return;
      if (this.switchBack.getCurrentValue() && this.silent.getCurrentValue() && e.getHand() == Hand.MAIN_HAND && this.originSlot != -1) {
         e.setItem(mc.player.getInventory().getStack(this.originSlot));
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (!shouldWork()) return;
      if (e.getType() == EventType.PRE) {
         if (mc.interactionManager.isBreakingBlock()) {
            if (this.checkSword.getCurrentValue()) {
               ItemStack itemStack = mc.player.getMainHandStack();
               if (itemStack.getItem() instanceof SwordItem) {
                  return;
               }
            }

            if (mc.crosshairTarget.getType() == Type.BLOCK) {
               BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
               int bestTool = this.getBestTool(hitResult.getBlockPos());
               if (bestTool != -1 && bestTool != mc.player.getInventory().selectedSlot) {
                  this.originSlot = mc.player.getInventory().selectedSlot;
                  mc.player.getInventory().selectedSlot = bestTool;
               }
            }
         }
      } else if (!mc.interactionManager.isBreakingBlock() && this.switchBack.getCurrentValue() && this.originSlot != -1) {
         mc.player.getInventory().selectedSlot = this.originSlot;
         this.originSlot = -1;
      }
   }

   private int getBestTool(BlockPos pos) {
      BlockState blockState = mc.world.getBlockState(pos);
      Block block = blockState.getBlock();
      int slot = 0;
      float dmg = 1.0F;

      for (int index = 0; index < 9; index++) {
         ItemStack itemStack = mc.player.getInventory().getStack(index);
         if (!InventoryUtils.isGodItem(itemStack)
                 && !itemStack.isEmpty()
                 && !blockState.isAir()
                 && (!(itemStack.getItem() instanceof SwordItem) || block instanceof CobwebBlock)) {
            float strVsBlock = itemStack.getItem().getMiningSpeedMultiplier(itemStack, blockState);
            if (strVsBlock > 1.0F && !(block instanceof ExperienceDroppingBlock) && !(block instanceof RedstoneOreBlock)) {
               int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
               if (i > 0) {
                  strVsBlock += (float) (i * i + 1);
               }
            }

            if (strVsBlock > dmg) {
               slot = index;
               dmg = strVsBlock;
            }
         }
      }

      return dmg > 1.0F ? slot : -1;
   }
}