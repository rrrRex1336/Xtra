package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@ModuleInfo(
   name = "Compass",
   description = "Shows a compass",
   category = Category.VISUAL
)
public class Compass extends Module {
   public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only").setDefaultBooleanValue(true).build().getBooleanValue();
   private boolean hasCompass = false;
   private BlockPos spawnPosition;
   private float renderYaw;
   private double renderX;
   private double renderZ;

   private BlockPos getSpawnPosition(ClientWorld p_117922_) {
      return p_117922_.getDimension().natural() ? p_117922_.getSpawnPos() : null;
   }

   private boolean hasPlayer() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof PlayerEntity) {
            return true;
         }
      }

      return false;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         this.hasCompass = InventoryUtils.hasItem(Items.COMPASS);
         this.spawnPosition = this.getSpawnPosition(mc.world);
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      this.renderX = MathHelper.lerp((double)e.getRenderPartialTicks(), mc.player.lastRenderX, mc.player.getX());
      this.renderZ = MathHelper.lerp((double)e.getRenderPartialTicks(), mc.player.lastRenderZ, mc.player.getZ());
      this.renderYaw = MathHelper.lerp(e.getRenderPartialTicks(), mc.player.prevYaw, mc.player.getYaw());
   }

   @EventTarget(4)
   public void onRender2D(EventRender2D e) {
      this.draw(e.getStack());
   }

   private void draw(MatrixStack stack) {
      if (this.hasCompass || !this.compassOnly.getCurrentValue()) {
         if (!this.hasPlayer() || !this.noPlayerOnly.getCurrentValue()) {
            if (this.spawnPosition != null) {
               float yaw = (float)(
                  Math.toDegrees(Math.atan2((double)this.spawnPosition.getZ() - this.renderZ, (double)this.spawnPosition.getX() - this.renderX))
                     - 90.0
                     - (double)this.renderYaw
               );
               float x = (float)mc.getWindow().getScaledWidth() / 2.0F;
               float y = (float)mc.getWindow().getScaledHeight() / 2.0F;
               stack.push();
               stack.translate(x, y, 0.0F);
               stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
               stack.translate(-x, -y, 0.0F);
               RenderUtils.drawTracer(stack, x, y - 45.0F, 10.0F, 2.0F, 1.0F, -1);
               stack.translate(x, y, 0.0F);
               stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
               stack.translate(-x, -y, 0.0F);
               stack.pop();
            }
         }
      }
   }
}
