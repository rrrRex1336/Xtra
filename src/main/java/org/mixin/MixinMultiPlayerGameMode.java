package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventDestroyBlock;
import awa.qwq.ovo.Naven.events.impl.EventPositionItem;
import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.viaversionfix.MaceLogic;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class MixinMultiPlayerGameMode {
   @Redirect(
           method = {"interactItem"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
                   ordinal = 0
           )
   )
   public void onSendPacket(ClientPlayNetworkHandler instance, Packet<?> pPacket) {
      EventPositionItem event = new EventPositionItem(pPacket);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.sendPacket(event.getPacket());
      }
   }

   @Inject(
           method = {"attackBlock"},
           at = {@At("HEAD")}
   )
   public void onStartDestroyBlock(BlockPos pLoc, Direction pFace, CallbackInfoReturnable<Boolean> cir) {
      Naven.getInstance().getEventManager().call(new EventDestroyBlock(pLoc, pFace));
   }

   @Inject(method = {"attackEntity"}, at = {@At("HEAD")}, cancellable = true)
   private void onAttackPre(PlayerEntity player, Entity entity, CallbackInfo ci) {
      EventAttack event = new EventAttack(false, entity);
      Naven.getInstance().getEventManager().call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = {"attackEntity"}, at = {@At("RETURN")})
   private void onAttackPost(PlayerEntity player, Entity entity, CallbackInfo ci) {
      if (ViaVersionFix.isHighVersionItemFixEnabled() && InventoryUtils.isServerMace(player.getMainHandStack())) {
         MaceLogic.playClientSmashSound(player.getWorld(), entity, player);
      }

      Naven.getInstance().getEventManager().call(new EventAttack(true, entity));
   }
}
