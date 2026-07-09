package awa.qwq.ovo.Naven.viaversionfix.items.windcharge;

import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WindChargeItem extends Item {
   private static final float PROJECTILE_SHOOT_POWER = 1.5F;

   public WindChargeItem(Settings properties) {
      super(properties);
   }

   @Override
   public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
      ItemStack stack = player.getStackInHand(hand);
      float pitch = 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F);
      if (level.isClient) {
         level.playSound(player.getX(), player.getY(), player.getZ(), ModSounds.WIND_CHARGE_THROW, SoundCategory.NEUTRAL, 0.5F, pitch, false);
      } else {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WIND_CHARGE_THROW, SoundCategory.NEUTRAL, 0.5F, pitch);
      }

      if (!level.isClient) {
         WindChargeProjectile projectile = new WindChargeProjectile(level, player);
         projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
         level.spawnEntity(projectile);
      }

      player.incrementStat(Stats.USED.getOrCreateStat(this));
      if (!player.getAbilities().creativeMode) {
         stack.decrement(1);
      }

      return TypedActionResult.success(stack, level.isClient());
   }
}
