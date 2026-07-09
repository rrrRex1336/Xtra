package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.movement.LongJump;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "AntiFireball",
   description = "Prevents fireballs from damaging you",
   category = Category.WORLD
)
public class AntiFireball extends Module {
   private final BooleanValue rotate = ValueBuilder.create(this, "Rotate")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();

   private final FloatValue range = ValueBuilder.create(this, "Range")
      .setDefaultFloatValue(6.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();

   private AbstractFireballEntity pendingFireball = null;
   private Vector2f fireballRotation = null;

   @EventTarget
   public void onRunTicks(EventRunTicks event) {
      if (!canWork() || !this.rotate.getCurrentValue()) {
         resetRotation();
         return;
      }

      if (event.getType() == EventType.POST) {
         if (this.pendingFireball != null && this.fireballRotation != null) {
            RotationManager.setRotations(this.fireballRotation);
            RotationManager.active = true;
            attackFireball(this.pendingFireball);
            resetRotation();
         }
         return;
      }

      if (event.getType() != EventType.PRE) {
         return;
      }

      Optional<AbstractFireballEntity> fireball = findFireball();
      if (fireball.isEmpty()) {
         resetRotation();
         return;
      }

      AbstractFireballEntity entity = fireball.get();
      this.fireballRotation = getRotationToFireball(entity);
      RotationManager.setRotations(this.fireballRotation);
      RotationManager.active = true;
      this.pendingFireball = entity;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() != EventType.PRE || !canWork() || this.rotate.getCurrentValue()) {
         return;
      }

      findFireball().ifPresent(this::attackFireball);
   }

   public boolean shouldApplyRotation() {
      return this.isEnabled() && this.rotate.getCurrentValue() && this.fireballRotation != null;
   }

   public Vector2f getFireballRotation() {
      return this.fireballRotation;
   }

   @Override
   public void onDisable() {
      resetRotation();
      super.onDisable();
   }

   private boolean canWork() {
      return mc.player != null
         && mc.world != null
         && mc.interactionManager != null
         && !Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled();
   }

   private Optional<AbstractFireballEntity> findFireball() {
      Stream<Entity> stream = StreamSupport.stream(mc.world.getEntities().spliterator(), false);
      return stream.filter(entity -> entity instanceof AbstractFireballEntity
            && !entity.isRemoved()
            && mc.player.distanceTo(entity) <= this.range.getCurrentValue())
         .map(entity -> (AbstractFireballEntity)entity)
         .min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)));
   }

   private Vector2f getRotationToFireball(AbstractFireballEntity fireball) {
      return RotationUtils.getRotations(mc.player.getCameraPosVec(1.0F), fireball.getBoundingBox().getCenter()).toVec2f();
   }

   private void attackFireball(AbstractFireballEntity fireball) {
      if (fireball.isRemoved() || mc.player.distanceTo(fireball) > this.range.getCurrentValue()) {
         return;
      }

      if (this.rotate.getCurrentValue() && this.fireballRotation != null) {
         RotationManager.setRotations(this.fireballRotation);
         RotationManager.active = true;
      }

      mc.interactionManager.attackEntity(mc.player, fireball);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void resetRotation() {
      this.pendingFireball = null;
      this.fireballRotation = null;
   }
}
