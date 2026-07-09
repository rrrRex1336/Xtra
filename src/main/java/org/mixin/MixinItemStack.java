package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.MaceLogic;
import awa.qwq.ovo.Naven.viaversionfix.items.ModItems;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearItem;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearLogic;
import awa.qwq.ovo.Naven.viaversionfix.items.spear.SpearMaterial;
import com.google.common.collect.Multimap;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {
   @Inject(method = "getTooltip", at = @At("RETURN"))
   private void localizeMappedMaceTooltip(PlayerEntity player, TooltipContext tooltipFlag, CallbackInfoReturnable<List<Text>> cir) {
      if (!ViaVersionFix.isHighVersionItemFixEnabled()
         || (!InventoryUtils.isServerMace((ItemStack)(Object)this) && !InventoryUtils.isServerSpear((ItemStack)(Object)this))) {
         return;
      }

      List<Text> tooltip = cir.getReturnValue();
      for (int i = 0; i < tooltip.size(); i++) {
         Text line = tooltip.get(i);
         String localized = localizeHighVersionEnchantments(line.getString());
         if (!localized.equals(line.getString())) {
            tooltip.set(i, Text.literal(localized).fillStyle(line.getStyle()));
         }
      }
   }

   @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceHoverName(CallbackInfoReturnable<Text> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(Text.translatable("item.naven-modern.mace"));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerWindCharge((ItemStack)(Object)this)) {
         cir.setReturnValue(Text.translatable("item.naven-modern.wind_charge"));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearMaterial material = InventoryUtils.getServerSpearMaterial((ItemStack)(Object)this);
         if (material != null) {
            cir.setReturnValue(Text.translatable(material.translationKey()));
         }
      }
   }

   @Inject(method = "hasCustomName", at = @At("HEAD"), cancellable = true)
   private void mappedMaceHasNoCustomHoverName(CallbackInfoReturnable<Boolean> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled()
         && (InventoryUtils.isServerMace((ItemStack)(Object)this)
            || InventoryUtils.isServerWindCharge((ItemStack)(Object)this)
            || InventoryUtils.isServerSpear((ItemStack)(Object)this))) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceRarity(CallbackInfoReturnable<Rarity> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(Rarity.RARE);
      } else if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerWindCharge((ItemStack)(Object)this)) {
         cir.setReturnValue(Rarity.COMMON);
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearMaterial material = InventoryUtils.getServerSpearMaterial((ItemStack)(Object)this);
         if (material != null) {
            cir.setReturnValue(material.rarity());
         }
      }
   }

   @Inject(method = "isOf(Lnet/minecraft/item/Item;)Z", at = @At("HEAD"), cancellable = true)
   private void recognizeMappedMace(Item item, CallbackInfoReturnable<Boolean> cir) {
      if (!ViaVersionFix.isHighVersionItemFixEnabled()) {
         return;
      }

      ItemStack stack = (ItemStack)(Object)this;
      if (item == ModItems.MACE) {
         if (InventoryUtils.isServerMace(stack)) {
            cir.setReturnValue(true);
         }
         return;
      }

      if (item == ModItems.WIND_CHARGE) {
         if (InventoryUtils.isServerWindCharge(stack)) {
            cir.setReturnValue(true);
         }
         return;
      }

      SpearMaterial queriedMaterial = ModItems.getSpearMaterial(item);
      if (queriedMaterial == null) {
         return;
      }

      SpearMaterial stackMaterial = InventoryUtils.getServerSpearMaterial(stack);
      if (stackMaterial == queriedMaterial) {
         cir.setReturnValue(true);
      }
   }

   @Inject(method = "use", at = @At("HEAD"), cancellable = true)
   private void useMappedWindCharge(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerWindCharge((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.WIND_CHARGE.use(level, player, hand));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearItem spear = getMappedSpear((ItemStack)(Object)this);
         if (spear != null) {
            cir.setReturnValue(spear.use(level, player, hand));
         }
      }
   }

   @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
   private void useMappedMaceOn(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.useOnBlock(context));
      }
   }

   @Inject(method = "getMiningSpeedMultiplier", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.getMiningSpeedMultiplier((ItemStack)(Object)this, state));
      }
   }

   @Inject(method = "postMine", at = @At("HEAD"), cancellable = true)
   private void mineBlockWithMappedMace(World level, BlockState state, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(stack)) {
         if (ModItems.MACE.postMine(stack, level, state, pos, player)) {
            player.incrementStat(Stats.USED.getOrCreateStat(ModItems.MACE));
         }

         ci.cancel();
      }
   }

   @Inject(method = "isSuitableFor", at = @At("HEAD"), cancellable = true)
   private void isMappedMaceCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.isSuitableFor(state));
      }
   }

   @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
   private void getMappedMaceAttributes(EquipmentSlot slot, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace((ItemStack)(Object)this)) {
         cir.setReturnValue(ModItems.MACE.getAttributeModifiers(slot));
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearItem spear = getMappedSpear((ItemStack)(Object)this);
         if (spear != null) {
            cir.setReturnValue(spear.getAttributeModifiers(slot));
         }
      }
   }

   @Inject(method = "postHit", at = @At("RETURN"))
   private void hurtEnemyWithMappedMace(LivingEntity target, PlayerEntity attacker, CallbackInfo ci) {
      ItemStack stack = (ItemStack)(Object)this;
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(stack)) {
         MaceLogic.handlePostHit(attacker.getWorld(), target, attacker, stack);
      } else if (ViaVersionFix.isHighVersionItemFixEnabled()) {
         SpearMaterial material = InventoryUtils.getServerSpearMaterial(stack);
         if (material != null) {
            SpearLogic.playHitSound(attacker.getWorld(), attacker, material);
         }
      }
   }

   @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
   private void getMappedSpearUseAnimation(CallbackInfoReturnable<UseAction> cir) {
      SpearItem spear = getMappedSpear((ItemStack)(Object)this);
      if (spear != null) {
         cir.setReturnValue(spear.getUseAction((ItemStack)(Object)this));
      }
   }

   @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
   private void getMappedSpearUseDuration(CallbackInfoReturnable<Integer> cir) {
      SpearItem spear = getMappedSpear((ItemStack)(Object)this);
      if (spear != null) {
         cir.setReturnValue(spear.getMaxUseTime((ItemStack)(Object)this));
      }
   }

   @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
   private void onMappedSpearUseTick(World level, LivingEntity entity, int remainingUseDuration, CallbackInfo ci) {
      SpearItem spear = getMappedSpear((ItemStack)(Object)this);
      if (spear != null) {
         spear.usageTick(level, entity, (ItemStack)(Object)this, remainingUseDuration);
         ci.cancel();
      }
   }

   @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
   private void releaseMappedSpearUsing(World level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
      SpearItem spear = getMappedSpear((ItemStack)(Object)this);
      if (spear != null) {
         spear.onStoppedUsing((ItemStack)(Object)this, level, entity, timeLeft);
         ci.cancel();
      }
   }

   private static String localizeHighVersionEnchantments(String text) {
      return text.replace("Wind Burst", Text.translatable("enchantment.minecraft.wind_burst").getString())
         .replace("Breach", Text.translatable("enchantment.minecraft.breach").getString())
         .replace("Density", Text.translatable("enchantment.minecraft.density").getString());
   }

   private static SpearItem getMappedSpear(ItemStack stack) {
      if (!ViaVersionFix.isHighVersionItemFixEnabled()) {
         return null;
      }

      SpearMaterial material = InventoryUtils.getServerSpearMaterial(stack);
      return material == null ? null : ModItems.getSpear(material);
   }
}
