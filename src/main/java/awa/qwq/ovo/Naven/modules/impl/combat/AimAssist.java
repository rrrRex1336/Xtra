package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AimAssist",
   description = "Automatically aims at the nearest entity",
   category = Category.COMBAT
)
public class AimAssist extends Module {
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue clickonly = ValueBuilder.create(this, "Click Only").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue slient = ValueBuilder.create(this, "Slient Aim").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue rotateSpeed = ValueBuilder.create(this, "Rotation Speed")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(90.0F)
      .build()
      .getFloatValue();
   FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
   public Vector2f targetRotation = new Vector2f();
   public boolean working = false;
   public boolean slientaim = this.slient.currentValue;

   @EventTarget
   public void onMotion(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         if (this.clickonly.currentValue && !mc.options.attackKey.isPressed()) {
            this.working = false;
            return;
         }

         Entity target = this.getTarget();
         float targetYaw;
         float targetPitch;
         if (target != null) {
            Vector2f rotations = RotationUtils.getRotations(target);
            targetYaw = mc.player.getYaw() + RotationUtils.getAngleDifference(rotations.getX(), mc.player.getYaw());
            targetPitch = rotations.getY();
            this.working = true;
         } else {
            targetYaw = mc.player.getYaw();
            targetPitch = mc.player.getPitch();
            if (this.targetRotation.getX() % 360.0F == targetYaw % 360.0F) {
               this.working = false;
            }
         }

         if (this.working) {
            this.targetRotation.setX(RotationUtils.rotateToYaw(this.rotateSpeed.getCurrentValue(), this.targetRotation.getX(), targetYaw));
            this.targetRotation.setY(targetPitch);
         } else {
            this.targetRotation.setX(targetYaw);
            this.targetRotation.setY(targetPitch);
         }
      }
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDead() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStandEntity) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof PlayerEntity && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof PlayerEntity) || !((double)entity.getWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof MobEntity || entity instanceof SlimeEntity || entity instanceof BatEntity || entity instanceof GolemEntity)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof AnimalEntity || entity instanceof SquidEntity) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof VillagerEntity && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof PlayerEntity) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else {
         Vec3d closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePos(), entity.getBoundingBox());
         if (closestPoint.distanceTo(mc.player.getEyePos()) > (double)this.aimRange.getCurrentValue()) {
            return false;
         } else {
            boolean b = RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
            if (entity.getName().getString().equals("Standing")) {
               System.out.println(b);
            }

            return b;
         }
      }
   }

   private Entity getTarget() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.world.getEntities().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      return possibleTargets.isEmpty() ? null : possibleTargets.get(0);
   }
}
