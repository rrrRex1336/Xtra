package awa.qwq.ovo.Naven.viaversionfix.items.spear;

import awa.qwq.ovo.Naven.viaversionfix.items.ModSounds;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class SpearItem extends ToolItem {
   private final SpearMaterial material;
   private final Multimap<EntityAttribute, EntityAttributeModifier> defaultModifiers;

   public SpearItem(SpearMaterial material, Settings properties) {
      super(material.tier(), properties);
      this.material = material;

      ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(
         EntityAttributes.GENERIC_ATTACK_DAMAGE,
         new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", material.attackDamageBonus(), EntityAttributeModifier.Operation.ADDITION)
      );
      builder.put(
         EntityAttributes.GENERIC_ATTACK_SPEED,
         new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", material.attackSpeedModifier(), EntityAttributeModifier.Operation.ADDITION)
      );
      this.defaultModifiers = builder.build();
   }

   public SpearMaterial getSpearMaterial() {
      return this.material;
   }

   @Override
   public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getAttributeModifiers(slot);
   }

   @Override
   public UseAction getUseAction(ItemStack stack) {
      return UseAction.SPEAR;
   }

   @Override
   public int getMaxUseTime(ItemStack stack) {
      return 72000;
   }

   @Override
   public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
      ItemStack stack = player.getStackInHand(hand);
      player.setCurrentHand(hand);
      playSound(level, player, this.material.isWood() ? ModSounds.SPEAR_WOOD_USE : ModSounds.SPEAR_USE, 0.8F, 1.0F);
      return TypedActionResult.consume(stack);
   }

   @Override
   public void usageTick(World level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
      int usedTicks = this.getMaxUseTime(stack) - remainingUseDuration;
      SpearLogic.tickKinetic(level, entity, stack, this.material, usedTicks);
   }

   @Override
   public void onStoppedUsing(ItemStack stack, World level, LivingEntity entity, int timeLeft) {
      SpearLogic.release(entity);
   }

   @Override
   public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!attacker.getWorld().isClient) {
         stack.damage(1, attacker, living -> living.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
      }

      SpearLogic.playHitSound(attacker.getWorld(), attacker, this.material);
      return true;
   }

   private static void playSound(World level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      if (level.isClient) {
         level.playSound(entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, volume, pitch, false);
      } else {
         level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
      }
   }
}
