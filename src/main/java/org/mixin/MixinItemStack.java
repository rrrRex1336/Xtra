package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.MaceLogic;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import com.google.common.collect.Multimap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {
   @Inject(method = "getTooltipLines", at = @At("RETURN"))
   private void localizeMappedMaceTooltip(Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
      if (!ViaVersionFix.isHighVersionItemFixEnabled() || !InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         return;
      }

      List<Component> tooltip = cir.getReturnValue();
      for (int i = 0; i < tooltip.size(); i++) {
         Component line = tooltip.get(i);
         String localized = localizeHighVersionEnchantments(line.getString());
         if (!localized.equals(line.getString())) {
            tooltip.set(i, Component.literal(localized).withStyle(line.getStyle()));
         }
      }
   }

   @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceHoverName(CallbackInfoReturnable<Component> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(Component.translatable("item.naven-modern.mace"));
      }
   }

   @Inject(method = "hasCustomHoverName", at = @At("HEAD"), cancellable = true)
   private void mappedMaceHasNoCustomHoverName(CallbackInfoReturnable<Boolean> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceRarity(CallbackInfoReturnable<Rarity> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(Rarity.RARE);
      }
   }

   @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
   private void recognizeMappedMace(Item item, CallbackInfoReturnable<Boolean> cir) {
      if (item == ModItems.MACE && ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
   private void useMappedMaceOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.useOn(context));
      }
   }

   @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.getDestroySpeed((ItemStack)(Object)this, state));
      }
   }

   @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
   private void mineBlockWithMappedMace(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(stack)) {
         if (ModItems.MACE.mineBlock(stack, level, state, pos, player)) {
            player.awardStat(Stats.ITEM_USED.get(ModItems.MACE));
         }

         ci.cancel();
      }
   }

   @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
   private void isMappedMaceCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.isCorrectToolForDrops(state));
      }
   }

   @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceAttributes(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.getDefaultAttributeModifiers(slot));
      }
   }

   @Inject(method = "hurtEnemy", at = @At("RETURN"))
   private void hurtEnemyWithMappedMace(LivingEntity target, Player attacker, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(stack)) {
         MaceLogic.handlePostHit(attacker.level(), target, attacker, stack);
      }
   }

   private static String localizeHighVersionEnchantments(String text) {
      return text.replace("Wind Burst", Component.translatable("enchantment.minecraft.wind_burst").getString())
         .replace("Breach", Component.translatable("enchantment.minecraft.breach").getString())
         .replace("Density", Component.translatable("enchantment.minecraft.density").getString());
   }
}
