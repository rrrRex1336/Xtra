package awa.qwq.ovo.Naven.viaversionfix.items.mace;

import awa.qwq.ovo.Naven.viaversionfix.MaceLogic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MaceItem extends MiningToolItem {
   public static final float SMASH_MIN_FALL_DISTANCE = 1.5F;
   public static final float DESTROY_SPEED = 6.0F;

   public MaceItem(Settings properties) {
      super(7.0F, -2.4F, MaceTier.INSTANCE, BlockTags.PICKAXE_MINEABLE, properties);
   }

   @Override
   public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
      return DESTROY_SPEED;
   }

   @Override
   public boolean isSuitableFor(BlockState state) {
      return !state.isIn(BlockTags.NEEDS_IRON_TOOL) && !state.isIn(BlockTags.NEEDS_DIAMOND_TOOL);
   }

   @Override
   public boolean postMine(ItemStack stack, World level, BlockState state, BlockPos pos, LivingEntity entity) {
      if (!level.isClient && state.getHardness(level, pos) != 0.0F) {
         stack.damage(1, entity, living -> living.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   @Override
   public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.damage(2, attacker, living -> living.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
      MaceLogic.handlePostHit(attacker.getWorld(), target, attacker, stack);
      return true;
   }
}
