package awa.qwq.ovo.Naven.viaversionfix.items;

import awa.qwq.ovo.Naven.viaversionfix.MaceLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MaceItem extends DiggerItem {
   public static final float SMASH_MIN_FALL_DISTANCE = 1.5F;
   public static final float DESTROY_SPEED = 6.0F;

   public MaceItem(Properties properties) {
      super(7.0F, -2.4F, MaceTier.INSTANCE, BlockTags.MINEABLE_WITH_PICKAXE, properties);
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      return DESTROY_SPEED;
   }

   @Override
   public boolean isCorrectToolForDrops(BlockState state) {
      return !state.is(BlockTags.NEEDS_IRON_TOOL) && !state.is(BlockTags.NEEDS_DIAMOND_TOOL);
   }

   @Override
   public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
      if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
         stack.hurtAndBreak(1, entity, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(2, attacker, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      MaceLogic.handlePostHit(attacker.level(), target, attacker, stack);
      return true;
   }
}
