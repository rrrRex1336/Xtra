package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.utils.GetC03StatusUtil;
import awa.qwq.ovo.Naven.utils.MovementUtils;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import org.mixin.accessors.LocalPlayerAccessor;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.StewItem;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(
        name = "Stuck",
        description = "Stuck in air!",
        category = Category.MOVEMENT
)
public class Stuck extends Module {

   private int stuckState = 0;
   private Packet<?> capturedPacket;
   private float savedYaw;
   private float savedPitch;
   private boolean pendingDisable = false;
   private final Queue<CommonPongC2SPacket> pongQueue = new ConcurrentLinkedQueue<>();
   public static final ConcurrentLinkedQueue<Runnable> delayPackets = new ConcurrentLinkedQueue<>();

   public ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Delay", "Packet", "Skip Ticks", "Cancel Move")
           .build()
           .getModeValue();

   public FloatValue skipTicks = ValueBuilder.create(this, "Skip Ticks")
           .setVisibility(()-> this.mode.isCurrentMode("Skip Ticks"))
           .setDefaultFloatValue(19.0f)
           .setMinFloatValue(1.0f)
           .setMaxFloatValue(19.0f)
           .setFloatStep(1f)
           .build()
           .getFloatValue();

   @Override
   public void onEnable() {
      this.stuckState = 0;
      this.capturedPacket = null;
      this.savedYaw = RotationManager.rotations.x;
      this.savedPitch = RotationManager.rotations.y;
      this.pendingDisable = false;
      if (mode.isCurrentMode("Skip Ticks")) {
         SkipTicks.skipTicks(skipTicks.getCurrentValue());
      } else if (mode.isCurrentMode("Cancel Move")) {
         MovementUtils.cancelMove();
      }
   }

   @Override
   public void setEnabled(boolean enable) {
      if (mc.player == null) {
         return;
      }
      if (enable) {
         super.setEnabled(true);
      } else if (this.mode.isCurrentMode("Delay")) {
         if (this.stuckState == 3) {
            super.setEnabled(false);
         } else {
            this.pendingDisable = true;
         }
      } else {
         super.setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      SkipTicks.dispatch();
      if (this.mode.isCurrentMode("Cancel Move")) {
         MovementUtils.resetMove();
         if (mc.player != null) {
            ((LocalPlayerAccessor) mc.player).setPositionReminder(GetC03StatusUtil.noMovePackets);
         }
      }
      super.onDisable();
   }

   @EventTarget
   public void onTick(EventRunTicks e) {
      if (this.mode.isCurrentMode("Cancel Move")) {
         return;
      }
      if (!this.mode.isCurrentMode("Packet")) return;
      Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold.isEnabled()) {
         scaffold.setEnabled(false);
         return;
      }
      if (mc.player == null) {
         return;
      }
      NetworkUtils.sendPacketNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (this.mode.isCurrentMode("Skip Ticks") || this.mode.isCurrentMode("Cancel Move")) return;
      Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold.isEnabled()) {
         scaffold.setEnabled(false);
         return;
      }
      if (mc.player == null) {
         return;
      }
      if (e.getType() == EventType.POST) {
         mc.player.setVelocity(0.0, 0.0, 0.0);
         if (this.stuckState == 1) {
            this.stuckState = 2;
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            if (this.shouldSendCapturedPacket() && (this.savedYaw != currentYaw || this.savedPitch != currentPitch)) {
               NetworkUtils.sendPacketNoEvent(new LookAndOnGround(currentYaw, currentPitch, mc.player.isOnGround()));
               while (!this.pongQueue.isEmpty()) {
                  NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
               }
               this.savedYaw = currentYaw;
               this.savedPitch = currentPitch;
            }
            NetworkUtils.sendPacketNoEvent((Packet<ServerPlayPacketListener>) this.capturedPacket);
         } else if (this.mode.isCurrentMode("Packet") && mc.player.age % 10 == 0) {
            while (!this.pongQueue.isEmpty()) {
               NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
            }
         }
         if (this.pendingDisable) {
            if (this.mode.isCurrentMode("Delay")) {
               NetworkUtils.sendPacketNoEvent(new PositionAndOnGround(mc.player.getX() + 1337.0, mc.player.getY(), mc.player.getZ() + 1337.0, mc.player.isOnGround()));
            } else {
               NetworkUtils.sendPacketNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
            while (!this.pongQueue.isEmpty()) {
               NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
            }
            if (this.mode.isCurrentMode("Packet")) {
               for (int i = 1; i <= 4; ++i) {
                  delayPackets.add(() -> {});
               }
            }
            this.stuckState = 3;
            this.pendingDisable = false;
         }
      }
   }

   @EventTarget
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && this.mode.isCurrentMode("Cancel Move")) {
         ((LocalPlayerAccessor) mc.player).setPositionReminder(0);
      }
   }

   private boolean shouldSendCapturedPacket() {
      if (this.capturedPacket instanceof PlayerInteractItemC2SPacket useItemPacket) {
         ItemStack heldStack = mc.player.getStackInHand(useItemPacket.getHand());
         return !(heldStack.getItem() instanceof StewItem) && !(heldStack.getItem() instanceof BowItem);
      }
      if (this.capturedPacket instanceof PlayerActionC2SPacket actionPacket) {
         return actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && mc.player.getActiveItem().getItem() instanceof BowItem;
      }
      return false;
   }

   @EventTarget
   public void onMoveInput(EventMoveInput e) {
      if (this.mode.isCurrentMode("Skip Ticks") || this.mode.isCurrentMode("Cancel Move")) return;
      e.setForward(0.0F);
      e.setStrafe(0.0F);
      e.setJump(false);
      e.setSneak(false);
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.stuckState = 3;
      this.capturedPacket = null;
      this.setEnabled(false);
   }

   @EventTarget(value=1)
   public void onPacket(EventPacket e) {
      if (this.mode.isCurrentMode("Skip Ticks")) return;
      if (mc.player == null) {
         return;
      }

      if (this.mode.isCurrentMode("Cancel Move")) {
         if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.setEnabled(false);
         } else if (e.getType() == EventType.SEND && e.getPacket() instanceof PlayerMoveC2SPacket.OnGroundOnly) {
            e.setCancelled(true);
         }
         return;
      }

      Object rawPacket = e.getPacket();
      if (rawPacket instanceof PlayerMoveC2SPacket) {
         if (this.stuckState != 1 && this.mode.isCurrentMode("Packet")) {
            Rotation jitterRotation = new Rotation(mc.player.getYaw() + (float)(Math.random() - 0.5), mc.player.getPitch());
            ((ServerboundMovePlayerPacketAccessor)mc.player).setXRot(jitterRotation.getPitch());
            ((ServerboundMovePlayerPacketAccessor)mc.player).setYRot(jitterRotation.getYaw());
         }
         e.setCancelled(true);
      } else if (e.getPacket() instanceof CommonPongC2SPacket) {
         this.pongQueue.offer((CommonPongC2SPacket)e.getPacket());
         e.setCancelled(true);
      } else if (e.getPacket() instanceof PlayerInteractItemC2SPacket || e.getPacket() instanceof PlayerActionC2SPacket) {
         this.capturedPacket = e.getPacket();
         this.stuckState = 1;
         e.setCancelled(true);
      } else if (e.getPacket() instanceof PlayerPositionLookS2CPacket && this.mode.isCurrentMode("Delay")) {
         while (!this.pongQueue.isEmpty()) {
            NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
         }
         this.stuckState = 3;
         this.setEnabled(false);
      }
   }
}
