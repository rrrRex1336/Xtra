package awa.qwq.ovo.Naven.modules.impl.Combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Misc.KillSay;
import awa.qwq.ovo.Naven.modules.impl.Misc.Teams;
import awa.qwq.ovo.Naven.modules.impl.Player.Blink;
import awa.qwq.ovo.Naven.modules.impl.Movement.Stuck;
import awa.qwq.ovo.Naven.modules.impl.Visual.WaterMark;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
        name = "KillAura",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class KillAura extends Module {
   private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
   private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
   public static Entity target;
   public static Entity aimingTarget;
   public static List<Entity> targets = new ArrayList<>();
   public static Vector2f rotation;
   BooleanValue targetHud = ValueBuilder.create(this, "Target Interface").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue movementCorrection = ValueBuilder.create(this, "Movement Fix")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(6.0F)
           .build()
           .getFloatValue();
   FloatValue minAPS = ValueBuilder.create(this, "Min APS(Attack Per Second)")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .build()
           .getFloatValue();
   FloatValue maxAPS = ValueBuilder.create(this, "Max APS(Attack Per Second)")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .build()
           .getFloatValue();
   public FloatValue rotateMinSpeed = ValueBuilder.create(this, "Min Rotation Speed")
           .setDefaultFloatValue(180.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(180.0F)
           .build()
           .getFloatValue();
   public FloatValue rotateMaxSpeed = ValueBuilder.create(this, "Max Rotation Speed")
           .setDefaultFloatValue(180.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(180.0F)
           .build()
           .getFloatValue();
   public FloatValue randomYawOffset = ValueBuilder.create(this, "Random Yaw Offset")
           .setDefaultFloatValue(12.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(180.0F)
           .build()
           .getFloatValue();
   public FloatValue randomPitchOffset = ValueBuilder.create(this, "Random Pitch Offset")
           .setDefaultFloatValue(84.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(180.0F)
           .build()
           .getFloatValue();
   FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(5.0F)
           .setVisibility(() -> !this.infSwitch.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
           .setDefaultFloatValue(360.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(10.0F)
           .setMaxFloatValue(360.0F)
           .build()
           .getFloatValue();
   FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
   {
      minAPS.linkAsMin(maxAPS);
      maxAPS.linkAsMax(minAPS);
   }                                         {
      rotateMinSpeed.linkAsMin(rotateMaxSpeed);
      rotateMaxSpeed.linkAsMax(rotateMinSpeed);
   }
   RotationUtils.Data lastRotationData;
   RotationUtils.Data rotationData;
   int attackTimes = 0;
   float attacks = 0.0F;
   private int index;
   private Vector4f blurMatrix;
   private final Random random = new Random();

   @EventTarget
   public void onShader(EventShader e) {
      if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
         RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      this.blurMatrix = null;
      if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
         LivingEntity living = (LivingEntity)target;
         e.getStack().pushPose();
         float x = mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
         float y = mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
         String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
         float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
         this.blurMatrix = new Vector4f(x, y, width, 30.0F);
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(e.getStack(), x, y, width, 30.0F, 5.0F, WaterMark.headerColor);
         StencilUtils.erase(true);
         RenderUtils.fillBound(e.getStack(), x, y, width, 30.0F, WaterMark.bodyColor);
         RenderUtils.fillBound(e.getStack(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, WaterMark.headerColor);
         StencilUtils.dispose();
         Fonts.harmony.render(e.getStack(), targetName, x + 5.0F, y + 6.0F, Color.WHITE, true, 0.35F);
         Fonts.harmony
                 .render(
                         e.getStack(),
                         "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                         x + 5.0F,
                         y + 17.0F,
                         Color.WHITE,
                         true,
                         0.35F
                 );
         e.getStack().popPose();
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      if (this.targetEsp.getCurrentValue()) {
         PoseStack stack = e.getPMatrixStack();
         float partialTicks = e.getRenderPartialTicks();
         stack.pushPose();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glEnable(2848);
         RenderSystem.setShader(GameRenderer::getPositionShader);
         RenderUtils.applyRegionalRenderOffset(stack);

         for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
               float[] color = target == living ? targetColorRed : targetColorGreen;
               stack.pushPose();
               RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
               double motionX = entity.getX() - entity.xo;
               double motionY = entity.getY() - entity.yo;
               double motionZ = entity.getZ() - entity.zo;
               AABB boundingBox = entity.getBoundingBox()
                       .move(-motionX, -motionY, -motionZ)
                       .move(partialTicks * motionX, partialTicks * motionY, partialTicks * motionZ);
               RenderUtils.drawSolidBox(boundingBox, stack);
               stack.popPose();
            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(3042);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDisable(2848);
         stack.popPose();
      }
   }

   @Override
   public void onEnable() {
      this.rotation = null;
      this.index = 0;
      target = null;
      aimingTarget = null;
      targets.clear();
   }

   @Override
   public void onDisable() {
      target = null;
      aimingTarget = null;
      super.onDisable();
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      target = null;
      aimingTarget = null;
      this.toggle();
   }

   @EventTarget
   public void onMotion(EventRunTicks event) {
      if (event.getType() == EventType.PRE && mc.player != null) {
         if (mc.screen instanceof AbstractContainerScreen
                 || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                 || InventoryUtils.shouldDisableFeatures()) {
            target = null;
            aimingTarget = null;
            this.rotationData = null;
            this.rotation = null;
            this.lastRotationData = null;
            targets.clear();
            return;
         }

         boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
         this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
         this.updateAttackTargets();
         aimingTarget = this.shouldPreAim();
         this.lastRotationData = this.rotationData;
         this.rotationData = null;
         if (aimingTarget != null) {
            this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
            if (this.rotationData.getRotation() != null) {
               this.rotation = this.rotationData.getRotation();
            } else {
               this.rotation = null;
            }
         }

         if (targets.isEmpty()) {
            target = null;
            return;
         }

         if (this.index > targets.size() - 1) {
            this.index = 0;
         }

         if (targets.size() > 1
                 && (this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
            this.attackTimes = 0;

            for (int i = 0; i < targets.size(); i++) {
               this.index++;
               if (this.index > targets.size() - 1) {
                  this.index = 0;
               }

               Entity nextTarget = targets.get(this.index);
               RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
               if (data.getDistance() < 3.0) {
                  break;
               }
            }
         }

         if (this.index > targets.size() - 1 || !isSwitch) {
            this.index = 0;
         }

         target = targets.get(this.index);
         float randomAPS = this.minAPS.getCurrentValue() + (this.random.nextFloat() * (this.maxAPS.getCurrentValue() - this.minAPS.getCurrentValue()));
         this.attacks = this.attacks + randomAPS / 20.0F;
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player.getUseItem().isEmpty()
              && mc.screen == null
              && !NetworkUtils.isServerLag()
              && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
         while (this.attacks >= 1.0F) {
            this.doAttack();
            this.attacks--;
         }
      }
   }

   public Entity shouldPreAim() {
      Entity target = KillAura.target;
      if (target == null) {
         List<Entity> aimTargets = this.getTargets();
         if (!aimTargets.isEmpty()) {
            target = aimTargets.get(0);
         }
      }

      return target;
   }

   public void doAttack() {
      if (!targets.isEmpty()) {
         HitResult hitResult = mc.hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            if (AntiBots.isBot(result.getEntity())) {
               ChatUtils.addChatMessage("Attacking Bot!");
               return;
            }
         }

         if (this.multi.getCurrentValue()) {
            int attacked = 0;

            for (Entity entity : targets) {
               if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                  this.attackEntity(entity);
                  if (++attacked >= 2) {
                     break;
                  }
               }
            }
         } else if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            this.attackEntity(result.getEntity());
         }
      }
   }

   public void updateAttackTargets() {
      targets = this.getTargets();
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
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !(entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                          && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
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
      } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         return closestPoint.distanceTo(mc.player.getEyePosition()) > this.aimRange.getCurrentValue()
                 ? false
                 : RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
      }
   }

   public void attackEntity(Entity entity) {
      this.attackTimes++;
      float currentYaw = mc.player.getYRot();
      float currentPitch = mc.player.getXRot();
      mc.player.setYRot(RotationManager.rotations.x);
      mc.player.setXRot(RotationManager.rotations.y);
      if (entity instanceof Player && !AntiBots.isBot(entity)) {
         KillSay.attackedPlayers.add(entity.getName().getString());
      }
      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);
      if (this.moreParticles.getCurrentValue()) {
         mc.player.magicCrit(entity);
         mc.player.crit(entity);
      }

      mc.player.setYRot(currentYaw);
      mc.player.setXRot(currentPitch);
   }

   public static Entity getTarget() {
      KillAura instance = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
      if (instance != null && instance.isEnabled()) {
         return target;
      }
      return null;
   }

   private List<Entity> getTargets() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
              .filter(entity -> entity instanceof Entity)
              .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
                 Comparator.comparingDouble(o -> RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? living.getHealth() : 0.0));
      }

      if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
         possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
      }

      possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
      return this.infSwitch.getCurrentValue()
              ? possibleTargets
              : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
   }
}