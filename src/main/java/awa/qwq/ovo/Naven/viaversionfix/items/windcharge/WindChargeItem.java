package awa.qwq.ovo.Naven.viaversionfix.items.windcharge;

import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WindChargeItem extends Item {
   private static final float PROJECTILE_SHOOT_POWER = 1.5F;

   public WindChargeItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      float pitch = 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F);
      if (level.isClientSide) {
         level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5F, pitch, false);
      } else {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5F, pitch);
      }

      if (!level.isClientSide) {
         WindChargeProjectile projectile = new WindChargeProjectile(level, player);
         projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
         level.addFreshEntity(projectile);
      }

      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }
}
