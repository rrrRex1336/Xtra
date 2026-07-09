package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventStuckInBlock;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "FastCobweb",
   category = Category.MOVEMENT,
   description = "Allows you to walk faster on cobwebs"
)
public class FastCobweb extends Module {
   public final ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Latest Grim")
           .build()
           .getModeValue();
   public final FloatValue groundMultiplier = ValueBuilder.create(this, "Ground Multiplier")
           .setDefaultFloatValue(0.60F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .build()
           .getFloatValue();

   private int playerInWebTick = -1;
   private int lastGroundSneakTick = -100;
   private int inputTick = -1;
   private float inputForward = 0.0F;
   private float inputStrafe = 0.0F;
   private boolean inputJump = false;
   private boolean inputSneak = false;

   @EventTarget
   public void onMoveInput(EventMoveInput event) {
      if (mc.player == null) {
         return;
      }

      int tick = mc.player.age;
      if (tick != this.playerInWebTick) {
         return;
      }

      this.inputTick = tick;
      this.inputForward = event.getForward();
      this.inputStrafe = event.getStrafe();
      this.inputJump = event.isJump();
      this.inputSneak = event.isSneak();

      if (!mc.player.isOnGround()) {
         return;
      }

      if (event.isSneak()) {
         this.lastGroundSneakTick = mc.player.age;
      }

      if (!event.isSneak() && (mc.player.age - this.lastGroundSneakTick) > 1) {
         return;
      }

      event.setJump(false);
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      setSuffix(mode.getCurrentMode());
      if (mc.player == null) {
         return;
      }

      if (e.getType() != EventType.PRE) {
         return;
      }

      int tick = mc.player.age;
      if (tick != this.playerInWebTick) {
         return;
      }

      Vec3d base = mc.player.getVelocity();
      boolean onGround = mc.player.isOnGround();

      float forward = 0.0F;
      float strafe = 0.0F;
      boolean jump = false;
      boolean sneak = false;

      if (mc.player.input != null) {
         forward = mc.player.input.movementForward;
         strafe = mc.player.input.movementSideways;
         jump = mc.player.input.jumping;
         sneak = mc.player.input.sneaking;
      } else if (this.inputTick == tick) {
         forward = this.inputForward;
         strafe = this.inputStrafe;
         jump = this.inputJump;
         sneak = this.inputSneak;
      }

      if (forward != 0 && strafe != 0) {
         forward *= 0.707f;
         strafe *= 0.707f;
      }

      double motionX;
      double motionZ;
      if (onGround) {
         motionX = base.x;
         motionZ = base.z;
      } else {
         motionX = 0.0;
         motionZ = 0.0;
         if (forward != 0 || strafe != 0) {
            double speed = 0.14122;
            float yaw = mc.player.getYaw();
            double radYaw = Math.toRadians(yaw);
            motionX = (-Math.sin(radYaw) * forward + Math.cos(radYaw) * strafe) * speed;
            motionZ = (Math.cos(radYaw) * forward + Math.sin(radYaw) * strafe) * speed;
         }
      }

      double motionY;
      if (onGround) {
         motionY = base.y;
      } else if (jump && sneak) {
         motionY = 0.0;
      } else if (jump) {
         motionY = 0.06222;
      } else if (sneak) {
         motionY = -0.18777F;
      } else {
         motionY = 0.0;
      }

      mc.player.setVelocity(motionX, motionY, motionZ);
      mc.player.fallDistance = 0.0F;
   }

   @EventTarget
   public void onStuck(EventStuckInBlock e) {
      if (mc.player == null) {
         return;
      }
      if (e.getState().getBlock() == Blocks.COBWEB) {
         this.playerInWebTick = mc.player.age;
         Vec3d vanilla = e.getStuckSpeedMultiplier();
         if (mc.player.isOnGround()) {
            double t = this.groundMultiplier.getCurrentValue();
            double x = vanilla.x + (1.0 - vanilla.x) * t;
            double z = vanilla.z + (1.0 - vanilla.z) * t;
            e.setStuckSpeedMultiplier(new Vec3d(x, vanilla.y, z));
         } else {
            e.setStuckSpeedMultiplier(new Vec3d(1.0, 1.0, 1.0));
         }
      }
   }
}