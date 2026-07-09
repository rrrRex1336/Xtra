package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.ProjectileData;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas.BasicProjectileData;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas.EntityArrowData;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas.EntityPotionData;
import awa.qwq.ovo.Naven.utils.RayTraceUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.item.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "Projectiles",
   description = "Renders projectiles",
   category = Category.VISUAL
)
public class Projectile extends Module {
   private final EntityArrowData arrowsColor = new EntityArrowData();
   private final EntityPotionData potionsColor = new EntityPotionData();
   private final BasicProjectileData enderPearlColor = new BasicProjectileData(Collections.singleton(EnderPearlEntity.class), new Color(173, 12, 255));
   private final BasicProjectileData eggColor = new BasicProjectileData(Collections.singleton(EggEntity.class), new Color(255, 238, 154));
   private final BasicProjectileData snowballColor = new BasicProjectileData(Collections.singleton(SnowballEntity.class), new Color(255, 255, 255));
   public BooleanValue showArrows = ValueBuilder.create(this, "Show Arrows").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showPearls = ValueBuilder.create(this, "Show Pearls").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showPotions = ValueBuilder.create(this, "Show Potions").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue showEggs = ValueBuilder.create(this, "Show Eggs").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue showSnowballs = ValueBuilder.create(this, "Show Snowballs").setDefaultBooleanValue(false).build().getBooleanValue();

   @EventTarget
   public void onRender3D(EventRender event) {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity) {
            ProjectileData var8 = this.getProjectileDataByEntity(entity);
            if (var8 != null) {
               MatrixStack stack = event.getPMatrixStack();
               stack.push();
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
               GL11.glEnable(2848);
               RenderSystem.setShader(GameRenderer::getPositionProgram);
               Color color = var8.getColor(entity);
               RenderSystem.setShaderColor((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 1.0F);
               this.render(stack, entity, var8);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               GL11.glDisable(3042);
               GL11.glEnable(2929);
               GL11.glDepthMask(true);
               GL11.glDisable(2848);
               stack.pop();
            }
         }
      }
   }

   @EventTarget
   private void onRender(EventRender event) {
      Projectile.Path pathResult = this.getPath(event.getRenderPartialTicks());
      if (pathResult != null) {
         List<Vec3d> path = pathResult.getPath();
         if (path.size() >= 2) {
            MatrixStack stack = event.getPMatrixStack();
            stack.push();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionProgram);
            Vec3d camPos = path.get(0);
            this.drawLine(stack, path, camPos);
            if (!path.isEmpty()) {
               Vec3d end = path.get(path.size() - 1);
               this.drawEndOfLine(stack, end, camPos, pathResult.result, event.getRenderPartialTicks());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.pop();
         }
      }
   }

   private void drawLine(MatrixStack matrixStack, List<Vec3d> path, Vec3d camPos) {
      Matrix4f matrix = matrixStack.peek().getPositionMatrix();
      BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      bufferBuilder.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);
      float[] colorF = new float[]{1.0F, 1.0F, 1.0F};
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 1.0F);

      for (Vec3d point : path) {
         bufferBuilder.vertex(matrix, (float)(point.x - camPos.x), (float)(point.y - camPos.y), (float)(point.z - camPos.z)).next();
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   private void drawEndOfLine(MatrixStack matrixStack, Vec3d end, Vec3d camPos, HitResult result, float partialTicks) {
      Box bb = new Box(0.15, 0.15, 0.15, 0.35, 0.35, 0.35);
      float[] colorF = new float[]{1.0F, 1.0F, 1.0F};
      if (result != null) {
         if (result.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)result;
            Direction direction = blockHitResult.getSide();
            if (direction == Direction.SOUTH) {
               bb = new Box(0.0, 0.0, 0.0, 0.5, 0.5, 0.1);
            } else if (direction == Direction.NORTH) {
               bb = new Box(0.0, 0.0, 0.4, 0.5, 0.5, 0.5);
            } else if (direction == Direction.EAST) {
               bb = new Box(0.0, 0.0, 0.0, 0.1, 0.5, 0.5);
            } else if (direction == Direction.WEST) {
               bb = new Box(0.4, 0.0, 0.0, 0.5, 0.5, 0.5);
            } else if (direction == Direction.UP) {
               colorF = new float[]{0.0F, 1.0F, 0.0F};
               bb = new Box(0.0, 0.0, 0.0, 0.5, 0.1, 0.5);
            } else if (direction == Direction.DOWN) {
               bb = new Box(0.0, 0.4, 0.0, 0.5, 0.5, 0.5);
            }
         } else if (result.getType() == Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)result;
            colorF = new float[]{1.0F, 0.0F, 0.0F};
            RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
            Entity entity = entityHitResult.getEntity();
            double motionX = entity.getX() - entity.prevX;
            double motionY = entity.getY() - entity.prevY;
            double motionZ = entity.getZ() - entity.prevZ;
            Vec3d cameraPos = RenderUtils.getCameraPos();
            Box move = entity.getBoundingBox()
               .offset(-cameraPos.x, -cameraPos.y, -cameraPos.z)
               .offset(-motionX, -motionY, -motionZ)
               .offset((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ)
               .expand(0.1);
            RenderUtils.drawSolidBox(move, matrixStack);
         }
      }

      double renderX = end.x - camPos.x;
      double renderY = end.y - camPos.y;
      double renderZ = end.z - camPos.z;
      matrixStack.push();
      matrixStack.translate(renderX - 0.25, renderY - 0.25, renderZ - 0.25);
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);
      RenderUtils.drawSolidBox(bb, matrixStack);
      RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.75F);
      RenderUtils.drawOutlinedBox(bb, matrixStack);
      matrixStack.pop();
   }

   private Projectile.Path getPath(float partialTicks) {
      PlayerEntity player = mc.player;
      ArrayList<Vec3d> path = new ArrayList<>();
      ItemStack stack = player.getMainHandStack();
      Item item = stack.getItem();
      if (!stack.isEmpty() && this.isThrowable(item)) {
         double arrowPosX = player.lastRenderX + (player.getX() - player.lastRenderX) * (double)partialTicks;
         double arrowPosY = player.lastRenderY + (player.getY() - player.lastRenderY) * (double)partialTicks + (double)player.getStandingEyeHeight() - 0.1;
         double arrowPosZ = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * (double)partialTicks;
         double arrowMotionFactor = item instanceof RangedWeaponItem ? 1.0 : 0.4;
         double yaw;
         double pitch;
         if (RotationManager.active) {
            if (RotationManager.lastAnimationRotation == null || RotationManager.animationRotation == null) {
               return new Projectile.Path(path, null);
            }

            yaw = Math.toRadians((double)MathHelper.lerp(partialTicks, RotationManager.lastAnimationRotation.x, RotationManager.animationRotation.x));
            pitch = Math.toRadians((double)MathHelper.lerp(partialTicks, RotationManager.lastAnimationRotation.y, RotationManager.animationRotation.y));
         } else {
            yaw = Math.toRadians((double)MathHelper.lerp(partialTicks, player.prevYaw, player.getYaw()));
            pitch = Math.toRadians((double)MathHelper.lerp(partialTicks, player.prevPitch, player.getPitch()));
         }

         double arrowMotionX = -Math.sin(yaw) * Math.cos(pitch) * arrowMotionFactor;
         double arrowMotionY = -Math.sin(pitch) * arrowMotionFactor;
         double arrowMotionZ = Math.cos(yaw) * Math.cos(pitch) * arrowMotionFactor;
         double arrowMotion = Math.sqrt(arrowMotionX * arrowMotionX + arrowMotionY * arrowMotionY + arrowMotionZ * arrowMotionZ);
         arrowMotionX /= arrowMotion;
         arrowMotionY /= arrowMotion;
         arrowMotionZ /= arrowMotion;
         if (item instanceof RangedWeaponItem) {
            float bowPower = (float)(72000 - player.getItemUseTimeLeft()) / 20.0F;
            bowPower = (bowPower * bowPower + bowPower * 2.0F) / 3.0F;
            if (bowPower > 1.0F || bowPower <= 0.1F) {
               bowPower = 1.0F;
            }

            bowPower *= 3.0F;
            arrowMotionX *= (double)bowPower;
            arrowMotionY *= (double)bowPower;
            arrowMotionZ *= (double)bowPower;
         } else {
            arrowMotionX *= 1.5;
            arrowMotionY *= 1.5;
            arrowMotionZ *= 1.5;
         }

         double gravity = this.getProjectileGravity(item);

         for (int i = 0; i < 1000; i++) {
            Vec3d arrowPos = new Vec3d(arrowPosX, arrowPosY, arrowPosZ);
            Vec3d postArrowPos = new Vec3d(arrowPosX + arrowMotionX, arrowPosY + arrowMotionY, arrowPosZ + arrowMotionZ);
            path.add(arrowPos);
            RaycastContext context = new RaycastContext(arrowPos, postArrowPos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
            BlockHitResult clip = mc.world.raycast(context);
            if (clip.getType() != Type.MISS) {
               return new Projectile.Path(path, clip);
            }

            ArrowEntity fakeArrow = new ArrowEntity(mc.world, arrowPosX, arrowPosY, arrowPosZ, new ItemStack(Items.ARROW));
            EntityHitResult entityHitResult = ProjectileUtil.getEntityCollision(
               mc.world,
               fakeArrow,
               arrowPos,
               postArrowPos,
               fakeArrow.getBoundingBox().stretch(new Vec3d(arrowMotionX, arrowMotionY, arrowMotionZ)).expand(1.0),
               entity -> entity != player && entity instanceof LivingEntity
            );
            if (entityHitResult != null && entityHitResult.getType() == Type.ENTITY) {
               return new Projectile.Path(path, entityHitResult);
            }

            arrowPosX += arrowMotionX;
            arrowPosY += arrowMotionY;
            arrowPosZ += arrowMotionZ;
            arrowMotionX *= 0.99;
            arrowMotionY *= 0.99;
            arrowMotionZ *= 0.99;
            arrowMotionY -= gravity;
         }

         return new Projectile.Path(path, null);
      } else {
         return null;
      }
   }

   private double getProjectileGravity(Item item) {
      if (item instanceof BowItem || item instanceof CrossbowItem) {
         return 0.05;
      } else if (item instanceof PotionItem) {
         return 0.4;
      } else if (item instanceof FishingRodItem) {
         return 0.15;
      } else {
         return item instanceof TridentItem ? 0.015 : 0.03;
      }
   }

   private boolean isThrowable(Item item) {
      return item instanceof BowItem
         || item instanceof CrossbowItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof EnderPearlItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem
         || item instanceof FishingRodItem
         || item instanceof TridentItem;
   }

   private void render(MatrixStack matrix, Entity entity, ProjectileData projectileInfo) {
      if (entity != null) {
         ClientPlayerEntity thePlayer = mc.player;
         ClientWorld theWorld = mc.world;
         Color color = projectileInfo.getColor(entity);
         if (color == null) {
            color = new Color(255, 255, 255);
         }

         Tessellator tesselator = Tessellator.getInstance();
         BufferBuilder builder = tesselator.getBuffer();
         builder.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         double posX = entity.getX();
         double posY = entity.getY();
         double posZ = entity.getZ();
         double motionX = entity.getVelocity().x;
         double motionY = entity.getVelocity().y;
         double motionZ = entity.getVelocity().z;
         this.drawVertex(color, builder, matrix, posX, posY, posZ);

         while (true) {
            float data1 = projectileInfo.getData1();
            float data2 = projectileInfo.getData2();
            Box aabb = new Box(posX - (double)data1, posY, posZ - (double)data1, posX + (double)data1, posY + (double)data2, posZ + (double)data1);
            Vec3d vec3 = new Vec3d(posX, posY, posZ);
            Vec3d vec3WithMotion = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
            HitResult movingObj = RayTraceUtils.rayTraceBlocks(vec3, vec3WithMotion, false, entity instanceof ArrowEntity, false, entity);
            if (!movingObj.getType().equals(Type.MISS)) {
               vec3WithMotion = new Vec3d(movingObj.getPos().getX(), movingObj.getPos().getY(), movingObj.getPos().getZ());
            }

            List<Entity> getByAABBEntitys = theWorld.getOtherEntities(thePlayer, aabb.shrink(motionX, motionY, motionZ).stretch(1.0, 1.0, 1.0));
            double lastMinDistance = 0.0;

            for (Entity aabbEntity : getByAABBEntitys) {
               if (aabbEntity instanceof LivingEntity && !(aabbEntity instanceof EndermanEntity) && aabbEntity.isCollidable() && !aabbEntity.equals(thePlayer)) {
                  aabb = aabbEntity.getBoundingBox().stretch(0.3, 0.3, 0.3);
                  EntityHitResult aabbMovingObj = RayTraceUtils.calculateIntercept(aabb, vec3, vec3WithMotion);
                  if (aabbMovingObj != null) {
                     double distance = vec3.distanceTo(aabbMovingObj.getPos());
                     if (distance < lastMinDistance || lastMinDistance == 0.0) {
                        lastMinDistance = distance;
                        movingObj = aabbMovingObj;
                     }
                  }
               }
            }

            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            if (!movingObj.getType().equals(Type.MISS)) {
               posX = movingObj.getPos().getX();
               posY = movingObj.getPos().getY();
               posZ = movingObj.getPos().getZ();
               break;
            }

            if (posY < -128.0) {
               break;
            }

            motionX *= entity.isTouchingWater() ? 0.8 : 0.99;
            double var39 = motionY * (entity.isTouchingWater() ? 0.8 : 0.99);
            motionZ *= entity.isTouchingWater() ? 0.8 : 0.99;
            motionY = var39 - (double)projectileInfo.getGravity();
            this.drawVertex(color, builder, matrix, posX + motionX, posY + motionY, posZ + motionZ);
         }

         tesselator.draw();
      }
   }

   private void drawVertex(Color color, BufferBuilder builder, MatrixStack stack, double x, double y, double z) {
      Entity entity = mc.getCameraEntity();
      double d0 = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * (double)mc.getTickDelta();
      double d1 = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * (double)mc.getTickDelta();
      double d2 = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * (double)mc.getTickDelta();
      builder.vertex(stack.peek().getPositionMatrix(), (float)(x - d0), (float)(y - d1) - 1.5F, (float)(z - d2)).color(color.getRGB()).next();
   }

   private ProjectileData getProjectileDataByEntity(Entity entity) {
      if (entity.isOnGround()) {
         return null;
      } else if (entity.getX() == entity.lastRenderX && entity.getZ() == entity.lastRenderZ) {
         return null;
      } else {
         for (ProjectileData data : this.getProjectileInfos()) {
            if (data.isTargetEntity(entity)) {
               return data;
            }
         }

         return null;
      }
   }

   private List<ProjectileData> getProjectileInfos() {
      ArrayList<ProjectileData> infos = new ArrayList<>();
      if (this.showArrows.getCurrentValue()) {
         infos.add(this.arrowsColor);
      }

      if (this.showPotions.getCurrentValue()) {
         infos.add(this.potionsColor);
      }

      if (this.showPearls.getCurrentValue()) {
         infos.add(this.enderPearlColor);
      }

      if (this.showEggs.getCurrentValue()) {
         infos.add(this.eggColor);
      }

      if (this.showSnowballs.getCurrentValue()) {
         infos.add(this.snowballColor);
      }

      return infos;
   }

   public static class Path {
      private final List<Vec3d> path;
      private final HitResult result;

      public Path(List<Vec3d> path, HitResult result) {
         this.path = path;
         this.result = result;
      }

      public List<Vec3d> getPath() {
         return this.path;
      }

      public HitResult getResult() {
         return this.result;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof Projectile.Path other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$path = this.getPath();
            Object other$path = other.getPath();
            if (this$path == null ? other$path == null : this$path.equals(other$path)) {
               Object this$result = this.getResult();
               Object other$result = other.getResult();
               return this$result == null ? other$result == null : this$result.equals(other$result);
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof Projectile.Path;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         Object $path = this.getPath();
         result = result * 59 + ($path == null ? 43 : $path.hashCode());
         Object $result = this.getResult();
         return result * 59 + ($result == null ? 43 : $result.hashCode());
      }

      @Override
      public String toString() {
         return "Projectile.Path(path=" + this.getPath() + ", result=" + this.getResult() + ")";
      }
   }
}
