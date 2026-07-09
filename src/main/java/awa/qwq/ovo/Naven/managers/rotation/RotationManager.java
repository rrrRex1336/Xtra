package awa.qwq.ovo.Naven.managers.rotation;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventAttackYaw;
import awa.qwq.ovo.Naven.events.impl.EventFallFlying;
import awa.qwq.ovo.Naven.events.impl.EventJump;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventPositionItem;
import awa.qwq.ovo.Naven.events.impl.EventRayTrace;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.events.impl.EventRotationAnimation;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventStrafe;
import awa.qwq.ovo.Naven.events.impl.EventUseItemRayTrace;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.impl.combat.*;
import awa.qwq.ovo.Naven.modules.impl.misc.Helper;
import awa.qwq.ovo.Naven.modules.impl.player.AutoMLG;
import awa.qwq.ovo.Naven.modules.impl.movement.LongJump;
import awa.qwq.ovo.Naven.modules.impl.world.AntiFireball;
import awa.qwq.ovo.Naven.modules.impl.world.BedAura;
import awa.qwq.ovo.Naven.modules.impl.world.ChestAura;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.modules.impl.world.Surround;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RotationManager {
   private static final Logger log = LogManager.getLogger(RotationManager.class);
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static Vector2f rotations;
   public static Vector2f lastRotations;
   public static Vector2f animationRotation;
   public static Vector2f lastAnimationRotation;
   public static boolean active = false;

   public static void setRotations(Vector2f rotations) {
      RotationManager.rotations = rotations;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      lastRotations = null;
      rotations = null;
   }

   @EventTarget(4)
   public void updateGlobalYaw(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         AutoMLG autoMLG = (AutoMLG) Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
         KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
         Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
         Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         Helper helper = (Helper) Naven.getInstance().getModuleManager().getModule(Helper.class);
         CrystalAura crystalAura = (CrystalAura) Naven.getInstance().getModuleManager().getModule(CrystalAura.class);
         AimAssist aimAssist = (AimAssist) Naven.getInstance().getModuleManager().getModule(AimAssist.class);
         LongJump longJump = (LongJump) Naven.getInstance().getModuleManager().getModule(LongJump.class);
         BedAura bedAura = (BedAura) Naven.getInstance().getModuleManager().getModule(BedAura.class);
         ChestAura chestAura = (ChestAura) Naven.getInstance().getModuleManager().getModule(ChestAura.class);
         Surround surround = (Surround) Naven.getInstance().getModuleManager().getModule(Surround.class);
         AutoThrow autoThrow = (AutoThrow) Naven.getInstance().getModuleManager().getModule(AutoThrow.class);
         Velocity velocity = (Velocity) Naven.getInstance().getModuleManager().getModule(Velocity.class);
         AntiFireball antiFireball = (AntiFireball) Naven.getInstance().getModuleManager().getModule(AntiFireball.class);

         active = true;
         boolean killAuraTargeting = isKillAuraTargeting(killAura);
         boolean auraTargeting = isAuraTargeting(aura);
         boolean killAuraReady = killAuraTargeting && killAura.rotation != null;
         boolean auraReady = aura != null && aura.isEnabled() && aura.working && aura.targetRotation != null;
         boolean bedAuraReady = bedAura != null && bedAura.isEnabled() && bedAura.bedRotations != null && !bedAura.shouldYieldToCombatAura();
         boolean bedAuraYieldingToCombat = bedAuraReady && bedAura.allowKillAura.getCurrentValue() && (killAuraTargeting || auraTargeting);

         if (autoMLG.isEnabled() && autoMLG.rotation) {
            if (autoMLG.getTargetRotation() != null) {
               setRotations(autoMLG.getTargetRotation());
            } else if (autoMLG.above != null && autoMLG.isCollectingWater()) {
               Vector2f lookAtRotation = autoMLG.calculateLookAt(autoMLG.above);
               setRotations(lookAtRotation);
            } else {
               setRotations(new Vector2f(mc.player.getYaw(), 90.0F));
            }
         } else if (autoThrow != null && autoThrow.isEnabled() && autoThrow.rotationSet > 0 && autoThrow.targetRotations != null) {
            setRotations(autoThrow.targetRotations);
            active = true;
         } else if (longJump.isEnabled() && LongJump.rotation != null) {
            setRotations(LongJump.rotation.toVec2f());
         } else if (antiFireball != null && antiFireball.shouldApplyRotation()) {
            setRotations(antiFireball.getFireballRotation());
            active = true;
         } else if (chestAura != null && chestAura.isEnabled() && chestAura.rotations != null && chestAura.chestRotations != null) {
            Vector2f rot = chestAura.chestRotations;
            setRotations(rot);
            active = true;
         } else if (helper != null && helper.isEnabled() && helper.needRotate && helper.helperRotation != null) {
            setRotations(helper.helperRotation);
            active = true;
         } else if (crystalAura.isEnabled() && CrystalAura.rotations != null) {
            setRotations(new Vector2f(CrystalAura.rotations.x, CrystalAura.rotations.y));
         } else if (surround != null && surround.isEnabled() && surround.rots != null) {
            setRotations(new Vector2f(surround.rots.x, surround.rots.y));
         } else if (scaffold.isEnabled() && scaffold.rots != null) {
            setRotations(new Vector2f(scaffold.rots.x, scaffold.rots.y));
         } else if (velocity != null && velocity.shouldApplyRotation()) {
            setRotations(velocity.getVelocityRotation());
            active = true;
         } else if (bedAuraYieldingToCombat && killAuraReady) {
            applyKillAuraRotation(killAura, autoThrow);
         } else if (bedAuraYieldingToCombat && auraReady) {
            applyAuraRotation(aura, autoThrow);
         } else if (bedAuraReady && !bedAuraYieldingToCombat) {
            setRotations(bedAura.bedRotations);
            active = true;
         } else if (killAuraReady) {
            applyKillAuraRotation(killAura, autoThrow);
         } else if (auraReady) {
            applyAuraRotation(aura, autoThrow);
         } else if (aimAssist.isEnabled() && aimAssist.working) {
            if (aimAssist.slientaim) {
               setRotations(new Vector2f(aimAssist.targetRotation.x, aimAssist.targetRotation.y));
            } else {
               active = false;
            }
         } else {
            active = false;
         }
      }
   }

   private static boolean isKillAuraTargeting(KillAura killAura) {
      return killAura != null && killAura.isEnabled() && (KillAura.target != null || !KillAura.targets.isEmpty());
   }

   private static boolean isAuraTargeting(Aura aura) {
      return aura != null && aura.isEnabled() && (Aura.target != null || !Aura.targets.isEmpty());
   }

   private static void applyKillAuraRotation(KillAura killAura, AutoThrow autoThrow) {
      float minSpeed = killAura.rotateMinSpeed.getCurrentValue();
      float maxSpeed = killAura.rotateMaxSpeed.getCurrentValue();
      float randomSpeed = minSpeed + (float) (Math.random() * (maxSpeed - minSpeed));

      Vector2f targetRot = new Vector2f(killAura.rotation.x, killAura.rotation.y);
      Vector2f currentRot = new Vector2f(mc.player.getYaw(), mc.player.getPitch());

      float yawOffset = (float) ((Math.random() - 0.5) * killAura.randomYawOffset.getCurrentValue());
      float pitchOffset = (float) ((Math.random() - 0.5) * killAura.randomPitchOffset.getCurrentValue());
      targetRot.x += yawOffset;
      targetRot.y += pitchOffset;

      float yawDelta = RotationUtils.getAngleDifference(targetRot.x, currentRot.x);
      if (Math.abs(yawDelta) > randomSpeed) {
         targetRot.x = currentRot.x + randomSpeed * Math.signum(yawDelta);
      }

      float pitchDelta = targetRot.y - currentRot.y;
      if (Math.abs(pitchDelta) > randomSpeed) {
         targetRot.y = currentRot.y + randomSpeed * Math.signum(pitchDelta);
      }

      setRotations(targetRot);
      clearAutoThrowRotation(autoThrow);
   }

   private static void applyAuraRotation(Aura aura, AutoThrow autoThrow) {
      setRotations(new Vector2f(aura.targetRotation.getX(), aura.targetRotation.getY()));
      clearAutoThrowRotation(autoThrow);
   }

   private static void clearAutoThrowRotation(AutoThrow autoThrow) {
      if (autoThrow != null && autoThrow.isEnabled()) {
         autoThrow.targetRotations = null;
         autoThrow.rotationSet = 0;
      }
   }

   @EventTarget
   public void onAnimation(EventRotationAnimation e) {
      if (animationRotation != null && lastAnimationRotation != null) {
         e.setYaw(animationRotation.x);
         e.setLastYaw(lastAnimationRotation.x);
         e.setPitch(animationRotation.y);
         e.setLastPitch(lastAnimationRotation.y);
      }
   }

   @EventTarget(4)
   public void onPre(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (rotations == null || lastRotations == null) {
            rotations = lastRotations = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
         }

         lastAnimationRotation = animationRotation;
         float yaw = rotations.x;
         float pitch = rotations.y;
         if (!Float.isNaN(yaw) && !Float.isNaN(pitch) && active) {
            e.setYaw(yaw);
            e.setPitch(pitch);
         }
         Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         animationRotation = scaffold.correctRotation;
      } else {
         animationRotation = new Vector2f(e.getYaw(), e.getPitch());

      }
      lastRotations = new Vector2f(e.getYaw(), e.getPitch());

   }

   private static boolean controlMovementCorrection() {
      AutoThrow autoThrow = (AutoThrow) Naven.getInstance().getModuleManager().getModule(AutoThrow.class);
      if (autoThrow != null && autoThrow.isEnabled() && autoThrow.rotationSet > 0 && autoThrow.targetRotations != null) {
         return true;
      }

      ChestAura chestAura = (ChestAura) Naven.getInstance().getModuleManager().getModule(ChestAura.class);
      if (chestAura != null && chestAura.isEnabled() && chestAura.rotations != null && chestAura.chestRotations != null) {
         return true;
      }

      Helper helper = (Helper) Naven.getInstance().getModuleManager().getModule(Helper.class);
      if (helper != null && helper.isEnabled() && helper.needRotate && helper.helperRotation != null) {
         return true;
      }

      Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold != null && scaffold.isEnabled() && scaffold.rots != null) {
         return true;
      }

      KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
      if (killAura != null && killAura.isEnabled() && KillAura.target != null) {
         return killAura.movementCorrection.getCurrentValue();
      }

      Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
      if (aura != null && aura.isEnabled() && aura.working) {
         return true;
      }

      return true;
   }

   @EventTarget
   public void onMove(EventMoveInput event) {
      if (active && rotations != null && controlMovementCorrection()) {
         float yaw = rotations.x;
         MoveUtils.correctionMovement(event, yaw);
      }
   }

   @EventTarget
   public void onMove(EventRayTrace event) {
      if (rotations != null && event.entity == mc.player && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onItemRayTrace(EventUseItemRayTrace event) {
      if (rotations != null && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onStrafe(EventStrafe event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget
   public void onJump(EventJump event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget(0)
   public void onPositionItem(EventPositionItem e) {
      if (active && rotations != null) {
         Full packet = (Full)e.getPacket();
         Full newPacket = new Full(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getX(), rotations.getY(), packet.isOnGround());
         e.setPacket(newPacket);
      }
   }

   @EventTarget
   public void onFallFlying(EventFallFlying e) {
      if (rotations != null) {
         e.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onAttack(EventAttackYaw e) {
      if (rotations != null) {
         e.setYaw(rotations.x);
      }
   }
}
