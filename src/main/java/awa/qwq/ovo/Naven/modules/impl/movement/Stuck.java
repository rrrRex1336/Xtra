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
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import org.mixin.accessors.LocalPlayerAccessor;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;

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
   private final Queue<ServerboundPongPacket> pongQueue = new ConcurrentLinkedQueue<>();
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
      NetworkUtils.sendPacketNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
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
         mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         if (this.stuckState == 1) {
            this.stuckState = 2;
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            if (this.shouldSendCapturedPacket() && (this.savedYaw != currentYaw || this.savedPitch != currentPitch)) {
               NetworkUtils.sendPacketNoEvent(new Rot(currentYaw, currentPitch, mc.player.onGround()));
               while (!this.pongQueue.isEmpty()) {
                  NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
               }
               this.savedYaw = currentYaw;
               this.savedPitch = currentPitch;
            }
            NetworkUtils.sendPacketNoEvent((Packet<ServerGamePacketListener>) this.capturedPacket);
         } else if (this.mode.isCurrentMode("Packet") && mc.player.tickCount % 10 == 0) {
            while (!this.pongQueue.isEmpty()) {
               NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
            }
         }
         if (this.pendingDisable) {
            if (this.mode.isCurrentMode("Delay")) {
               NetworkUtils.sendPacketNoEvent(new Pos(mc.player.getX() + 1337.0, mc.player.getY(), mc.player.getZ() + 1337.0, mc.player.onGround()));
            } else {
               NetworkUtils.sendPacketNoEvent(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
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
      if (this.capturedPacket instanceof ServerboundUseItemPacket useItemPacket) {
         ItemStack heldStack = mc.player.getItemInHand(useItemPacket.getHand());
         return !(heldStack.getItem() instanceof BowlFoodItem) && !(heldStack.getItem() instanceof BowItem);
      }
      if (this.capturedPacket instanceof ServerboundPlayerActionPacket actionPacket) {
         return actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && mc.player.getUseItem().getItem() instanceof BowItem;
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
         if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.setEnabled(false);
         } else if (e.getType() == EventType.SEND && e.getPacket() instanceof ServerboundMovePlayerPacket.StatusOnly) {
            e.setCancelled(true);
         }
         return;
      }

      Object rawPacket = e.getPacket();
      if (rawPacket instanceof ServerboundMovePlayerPacket) {
         if (this.stuckState != 1 && this.mode.isCurrentMode("Packet")) {
            Rotation jitterRotation = new Rotation(mc.player.getYRot() + (float)(Math.random() - 0.5), mc.player.getXRot());
            ((ServerboundMovePlayerPacketAccessor)mc.player).setXRot(jitterRotation.getPitch());
            ((ServerboundMovePlayerPacketAccessor)mc.player).setYRot(jitterRotation.getYaw());
         }
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ServerboundPongPacket) {
         this.pongQueue.offer((ServerboundPongPacket)e.getPacket());
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ServerboundUseItemPacket || e.getPacket() instanceof ServerboundPlayerActionPacket) {
         this.capturedPacket = e.getPacket();
         this.stuckState = 1;
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ClientboundPlayerPositionPacket && this.mode.isCurrentMode("Delay")) {
         while (!this.pongQueue.isEmpty()) {
            NetworkUtils.sendPacketNoEvent(this.pongQueue.poll());
         }
         this.stuckState = 3;
         this.setEnabled(false);
      }
   }
}
