package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class MixinLocalPlayer extends AbstractClientPlayerEntity {
   @Shadow
   private boolean lastSprinting;
   @Shadow
   @Final
   public ClientPlayNetworkHandler networkHandler;
   @Shadow
   private boolean lastSneaking;
   @Shadow
   private double lastX;
   @Shadow
   private double lastBaseY;
   @Shadow
   private double lastZ;
   @Shadow
   private float lastYaw;
   @Shadow
   private float lastPitch;
   @Shadow
   private int ticksSinceLastPositionPacketSent;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private boolean autoJumpEnabled;
   @Shadow
   @Final
   protected MinecraftClient client;

   @Shadow
   protected abstract boolean isCamera();

   @Shadow
   protected abstract void sendSprintingPacket();

   public MixinLocalPlayer(ClientWorld pClientLevel, GameProfile pGameProfile) {
      super(pClientLevel, pGameProfile);
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   public void injectUpdateEvent(CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventUpdate());
   }

   @Inject(
           method = {"tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
                   shift = Shift.BEFORE
           )}
   )
   public void onTick(CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventSprint());
   }

   /**
    * @author b
    * @reason b
    */
   @Overwrite
   private void sendMovementPackets() {
      EventMotion eventPre = new EventMotion(EventType.PRE, this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), this.isOnGround());
      Naven.getInstance().getEventManager().call(eventPre);
      if (eventPre.isCancelled()) {
         Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
      } else {
         this.sendSprintingPacket();
         boolean flag3 = this.isSneaking();
         if (flag3 != this.lastSneaking) {
            Mode serverboundplayercommandpacket$action1 = flag3 ? Mode.PRESS_SHIFT_KEY : Mode.RELEASE_SHIFT_KEY;
            this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, serverboundplayercommandpacket$action1));
            this.lastSneaking = flag3;
         }

         if (this.isCamera()) {
            double d4 = eventPre.getX() - this.lastX;
            double d0 = eventPre.getY() - this.lastBaseY;
            double d1 = eventPre.getZ() - this.lastZ;
            double d2 = (double) (eventPre.getYaw() - this.lastYaw);
            double d3 = (double) (eventPre.getPitch() - this.lastPitch);
            this.ticksSinceLastPositionPacketSent++;
            boolean flag1 = MathHelper.squaredMagnitude(d4, d0, d1) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean flag2 = d2 != 0.0 || d3 != 0.0;
            if (this.hasVehicle()) {
               Vec3d vec3 = this.getVelocity();
               this.networkHandler.sendPacket(new Full(vec3.x, -999.0, vec3.z, eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
               flag1 = false;
            } else if (flag1 && flag2) {
               this.networkHandler
                       .sendPacket(new Full(eventPre.getX(), eventPre.getY(), eventPre.getZ(), eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
            } else if (flag1) {
               this.networkHandler.sendPacket(new PositionAndOnGround(eventPre.getX(), eventPre.getY(), eventPre.getZ(), eventPre.isOnGround()));
            } else if (flag2) {
               this.networkHandler.sendPacket(new LookAndOnGround(eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround()));
            } else if (this.lastOnGround != eventPre.isOnGround()) {
               this.networkHandler.sendPacket(new OnGroundOnly(eventPre.isOnGround()));
            }

            if (flag1) {
               this.lastX = eventPre.getX();
               this.lastBaseY = eventPre.getY();
               this.lastZ = eventPre.getZ();
               this.ticksSinceLastPositionPacketSent = 0;
            }

            if (flag2) {
               this.lastYaw = eventPre.getYaw();
               this.lastPitch = eventPre.getPitch();
            }

            this.lastOnGround = eventPre.isOnGround();
            this.autoJumpEnabled = (Boolean) this.client.options.getAutoJump().getValue();
         }

         Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
      }
   }

   @Redirect(
           method = {"tickMovement"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
                   ordinal = 0
           )
   )
   public boolean onSlowdown(ClientPlayerEntity localPlayer) {
      EventSlowdown event = new EventSlowdown(localPlayer.isUsingItem());
      Naven.getInstance().getEventManager().call(event);
      return event.isSlowdown();
   }

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void hookTick(CallbackInfo ci) {
      if (SkipTicks.tick()) {
         this.prevX = this.getX();
         this.prevY = this.getY();
         this.prevZ = this.getZ();
         ci.cancel();
      }
   }
}
