package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.EntityWatcher;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.ProjectionUtils;
import awa.qwq.ovo.Naven.utils.SharedESPData;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import org.antlr.v4.runtime.misc.OrderedHashSet;

@ModuleInfo(
   name = "ItemTracker",
   description = "Show the player's effect tags.",
   category = Category.MISC
)
public class ItemTracker extends Module {
   private final BooleanValue debug = ValueBuilder.create(this, "Debug").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue shared = ValueBuilder.create(this, "Shared").setDefaultBooleanValue(true).build().getBooleanValue();
   private final List<ItemTracker.TargetInfo> entityPositions = new CopyOnWriteArrayList<>();

   @EventTarget
   public void update(EventRender e) {
      try {
         this.updatePositions(e.getRenderPartialTicks());
      } catch (Exception var3) {
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();

      for (Entity entity : mc.world.getEntities()) {
         if (entity != mc.player && entity instanceof AbstractClientPlayerEntity) {
            double x = MathUtils.interpolate(renderPartialTicks, entity.prevX, entity.getX());
            double y = MathUtils.interpolate(renderPartialTicks, entity.prevY, entity.getY()) + (double)entity.getHeight();
            double z = MathUtils.interpolate(renderPartialTicks, entity.prevZ, entity.getZ());
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            this.entityPositions
               .add(new ItemTracker.TargetInfo((AbstractClientPlayerEntity)entity, vector, EntityWatcher.getEntityTags((AbstractClientPlayerEntity)entity)));
         }
      }

      if (this.shared.getCurrentValue()) {
         Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

         for (SharedESPData value : dataMap.values()) {
            double x = value.getPosX();
            double y = value.getPosY() + (double)mc.player.getHeight();
            double z = value.getPosZ();
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            this.entityPositions.add(new ItemTracker.TargetInfo(null, vector, Set.of(value.getTags())));
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      for (ItemTracker.TargetInfo info : this.entityPositions) {
         e.getStack().push();
         double y = 0.0;

         for (String entityTag : info.getDescription()) {
            Fonts.harmony
               .render(
                  e.getStack(),
                  I18n.translate(entityTag, new Object[0]),
                  (double)(info.getPosition().x + 10.0F),
                  (double)info.getPosition().y + y,
                  Color.RED,
                  true,
                  0.3F
               );
            y += Fonts.harmony.getHeight(true, 0.3F);
         }

         if (this.debug.getCurrentValue() && info.getPlayer() != null) {
            AbstractClientPlayerEntity player = info.getPlayer();
            OrderedHashSet<String> debugInfos = new OrderedHashSet();
            debugInfos.add("X: " + player.getX());
            debugInfos.add("Y: " + player.getY());
            debugInfos.add("Z: " + player.getZ());
            debugInfos.add("Ticks: " + player.age);

            for (String debugInfo : debugInfos) {
               Fonts.harmony.render(e.getStack(), debugInfo, (double)(info.getPosition().x + 10.0F), (double)info.getPosition().y + y, Color.RED, true, 0.35F);
               y += Fonts.harmony.getHeight(true, 0.35F);
            }
         }

         e.getStack().pop();
      }
   }

   private static class TargetInfo {
      AbstractClientPlayerEntity player;
      Vector2f position;
      Set<String> description;

      public AbstractClientPlayerEntity getPlayer() {
         return this.player;
      }

      public Vector2f getPosition() {
         return this.position;
      }

      public Set<String> getDescription() {
         return this.description;
      }

      public void setPlayer(AbstractClientPlayerEntity player) {
         this.player = player;
      }

      public void setPosition(Vector2f position) {
         this.position = position;
      }

      public void setDescription(Set<String> description) {
         this.description = description;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof ItemTracker.TargetInfo other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$player = this.getPlayer();
            Object other$player = other.getPlayer();
            if (this$player == null ? other$player == null : this$player.equals(other$player)) {
               Object this$position = this.getPosition();
               Object other$position = other.getPosition();
               if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                  Object this$description = this.getDescription();
                  Object other$description = other.getDescription();
                  return this$description == null ? other$description == null : this$description.equals(other$description);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof ItemTracker.TargetInfo;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         Object $player = this.getPlayer();
         result = result * 59 + ($player == null ? 43 : $player.hashCode());
         Object $position = this.getPosition();
         result = result * 59 + ($position == null ? 43 : $position.hashCode());
         Object $description = this.getDescription();
         return result * 59 + ($description == null ? 43 : $description.hashCode());
      }

      @Override
      public String toString() {
         return "ItemTracker.TargetInfo(player=" + this.getPlayer() + ", position=" + this.getPosition() + ", description=" + this.getDescription() + ")";
      }

      public TargetInfo(AbstractClientPlayerEntity player, Vector2f position, Set<String> description) {
         this.player = player;
         this.position = position;
         this.description = description;
      }
   }
}
