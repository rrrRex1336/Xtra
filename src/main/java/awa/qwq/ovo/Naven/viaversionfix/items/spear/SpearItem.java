package awa.qwq.ovo.Naven.viaversionfix.items.spear;

import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class SpearItem extends TieredItem {
   private final SpearMaterial material;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public SpearItem(SpearMaterial material, Properties properties) {
      super(material.tier(), properties);
      this.material = material;

      ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(
         Attributes.ATTACK_DAMAGE,
         new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", material.attackDamageBonus(), AttributeModifier.Operation.ADDITION)
      );
      builder.put(
         Attributes.ATTACK_SPEED,
         new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", material.attackSpeedModifier(), AttributeModifier.Operation.ADDITION)
      );
      this.defaultModifiers = builder.build();
   }

   public SpearMaterial getSpearMaterial() {
      return this.material;
   }

   @Override
   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.SPEAR;
   }

   @Override
   public int getUseDuration(ItemStack stack) {
      return 72000;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      player.startUsingItem(hand);
      playSound(level, player, this.material.isWood() ? ModSounds.SPEAR_WOOD_USE : ModSounds.SPEAR_USE, 0.8F, 1.0F);
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
      int usedTicks = this.getUseDuration(stack) - remainingUseDuration;
      SpearLogic.tickKinetic(level, entity, stack, this.material, usedTicks);
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      SpearLogic.release(entity);
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!attacker.level().isClientSide) {
         stack.hurtAndBreak(1, attacker, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      SpearLogic.playHitSound(attacker.level(), attacker, this.material);
      return true;
   }

   private static void playSound(Level level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      if (level.isClientSide) {
         level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
      } else {
         level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
      }
   }
}
