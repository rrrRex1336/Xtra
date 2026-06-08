package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventDestroyBlock;
import awa.qwq.ovo.Naven.events.impl.EventPositionItem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public class MixinMultiPlayerGameMode {
   @Redirect(
           method = {"useItem"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
                   ordinal = 0
           )
   )
   public void onSendPacket(ClientPacketListener instance, Packet<?> pPacket) {
      EventPositionItem event = new EventPositionItem(pPacket);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.send(event.getPacket());
      }
   }

   @Inject(
           method = {"startDestroyBlock"},
           at = {@At("HEAD")}
   )
   public void onStartDestroyBlock(BlockPos pLoc, Direction pFace, CallbackInfoReturnable<Boolean> cir) {
      Naven.getInstance().getEventManager().call(new EventDestroyBlock(pLoc, pFace));
   }

   @Inject(method = {"attack"}, at = {@At("HEAD")}, cancellable = true)
   private void onAttackPre(Player player, Entity entity, CallbackInfo ci) {
      EventAttack event = new EventAttack(false, entity);  //  entity（目标）
      Naven.getInstance().getEventManager().call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = {"attack"}, at = {@At("RETURN")})
   private void onAttackPost(Player player, Entity entity, CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventAttack(true, entity));  // 已经是 entity，保持
   }
}
