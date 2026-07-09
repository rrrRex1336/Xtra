package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import awa.qwq.ovo.Naven.utils.EntityWatcher;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.ProjectionUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SharedESPData;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.modules.impl.misc.KillerDetection;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

@ModuleInfo(
   name = "NameTags",
   category = Category.VISUAL,
   description = "Renders name tags"
)
public class NameTags extends Module {
   public BooleanValue mcf = ValueBuilder.create(this, "Middle Click Friend").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showCompassPosition = ValueBuilder.create(this, "Compass Position").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.showCompassPosition.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.showCompassPosition.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue shared = ValueBuilder.create(this, "Shared ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(0.3F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(0.5F)
      .build()
      .getFloatValue();
   private static final int color1 = new Color(0, 0, 0, 40).getRGB();
   private static final int color2 = new Color(0, 0, 0, 80).getRGB();
   private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
   private final List<NameTags.NameTagData> sharedPositions = new CopyOnWriteArrayList<>();
   List<Vector4f> blurMatrices = new ArrayList<>();
   private BlockPos spawnPosition;
   private Vector2f compassPosition;
   private final Map<PlayerEntity, Integer> aimTicks = new ConcurrentHashMap<>();
   private PlayerEntity aimingPlayer;

   private boolean hasPlayer() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof PlayerEntity) {
            return true;
         }
      }

      return false;
   }

   private BlockPos getSpawnPosition(ClientWorld p_117922_) {
      return p_117922_.getDimension().natural() ? p_117922_.getSpawnPos() : null;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (!this.mcf.getCurrentValue()) {
            this.aimingPlayer = null;
         } else {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (!(player instanceof BlinkingPlayer) && player != mc.player) {
                  if (isAiming(player, mc.player.getYaw(), mc.player.getPitch())) {
                     if (this.aimTicks.containsKey(player)) {
                        this.aimTicks.put(player, this.aimTicks.get(player) + 1);
                     } else {
                        this.aimTicks.put(player, 1);
                     }

                     if (this.aimTicks.get(player) >= 10) {
                        this.aimingPlayer = player;
                        break;
                     }
                  } else if (this.aimTicks.containsKey(player) && this.aimTicks.get(player) > 0) {
                     this.aimTicks.put(player, this.aimTicks.get(player) - 1);
                  } else {
                     this.aimTicks.put(player, 0);
                  }
               }
            }

            if (this.aimingPlayer != null && this.aimTicks.containsKey(this.aimingPlayer) && this.aimTicks.get(this.aimingPlayer) <= 0) {
               this.aimingPlayer = null;
            }
         }

         this.spawnPosition = null;
         if (!InventoryUtils.hasItem(Items.COMPASS) && this.compassOnly.getCurrentValue()) {
            return;
         }

         if (this.hasPlayer() && this.noPlayerOnly.getCurrentValue()) {
            return;
         }

         this.spawnPosition = this.getSpawnPosition(mc.world);
      }
   }

   public static boolean isAiming(Entity targetEntity, float yaw, float pitch) {
      Vec3d playerEye = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getStandingEyeHeight(), mc.player.getZ());
      HitResult intercept = RotationUtils.getIntercept(targetEntity.getBoundingBox(), new Vector2f(yaw, pitch), playerEye, 150.0);
      if (intercept == null) {
         return false;
      } else {
         return intercept.getType() != Type.ENTITY ? false : intercept.getPos().distanceTo(playerEye) < 150.0;
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      for (Vector4f blurMatrix : this.blurMatrices) {
         RenderUtils.fill(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
      }
   }

   @EventTarget
   public void update(EventRender e) {
      try {
         this.updatePositions(e.getRenderPartialTicks());
         this.compassPosition = null;
         if (this.spawnPosition != null) {
            this.compassPosition = ProjectionUtils.project(
               (double)this.spawnPosition.getX() + 0.5,
               (double)this.spawnPosition.getY() + 1.75,
               (double)this.spawnPosition.getZ() + 0.5,
               e.getRenderPartialTicks()
            );
         }
      } catch (Exception var3) {
      }
   }

   @EventTarget
   public void onMouseKey(EventMouseClick e) {
      if (e.getKey() == 2 && !e.isState() && this.mcf.getCurrentValue() && this.aimingPlayer != null) {
         if (FriendManager.isFriend(this.aimingPlayer)) {
            Notification notification = new Notification(
               NotificationLevel.ERROR, "Removed " + this.aimingPlayer.getName().getString() + " from friends!", 3000L
            );
            Naven.getInstance().getNotificationManager().addNotification(notification);
            FriendManager.removeFriend(this.aimingPlayer);
         } else {
            Notification notification = new Notification(NotificationLevel.SUCCESS, "Added " + this.aimingPlayer.getName().getString() + " as friends!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
            FriendManager.addFriend(this.aimingPlayer);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      this.blurMatrices.clear();
      if (this.compassPosition != null) {
         Vector2f position = this.compassPosition;
         float scale = Math.max(
               80.0F
                  - MathHelper.sqrt(
                     (float)mc.player
                        .squaredDistanceTo(
                           (double)this.spawnPosition.getX() + 0.5, (double)this.spawnPosition.getY() + 1.75, (double)this.spawnPosition.getZ() + 0.5
                        )
                  ),
               0.0F
            )
            * this.scale.getCurrentValue()
            / 80.0F;
         String text = "Compass";
         float width = Fonts.harmony.getWidth(text, (double)scale);
         double height = Fonts.harmony.getHeight(true, (double)scale);
         this.blurMatrices
            .add(new Vector4f(position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height)));
         StencilUtils.write(false);
         RenderUtils.fill(
            e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height), -1
         );
         StencilUtils.erase(true);
         RenderUtils.fill(
            e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height), color1
         );
         StencilUtils.dispose();
         Fonts.harmony.setAlpha(0.8F);
         Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
      }

      for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
         if (entry.getKey() != mc.player && entry.getKey() instanceof PlayerEntity) {
            PlayerEntity living = (PlayerEntity)entry.getKey();
            e.getStack().push();
            float hp = living.getHealth();
            if (hp > 20.0F) {
               living.setHealth(20.0F);
            }

            Vector2f position = entry.getValue();
            String playerName = living.getName().getString();
            String text = "";
            if (Teams.isSameTeam(living)) {
               text = text + "§aTeam§f | ";
            }

            if (FriendManager.isFriend(living)) {
               text = text + "§aFriend§f | ";
            }

            if (this.aimingPlayer == living) {
               text = text + "§cAiming§f | ";
            }

            // 添加杀手检测
            if (KillerDetection.getDetectedKillers().contains(playerName)) {
               text = text + "§c" + playerName + " (Killer)§f";
            } else {
               text = text + this.formatPlayerName(living);
            }

            text = text + " | §c" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";
            float scale = this.scale.getCurrentValue();
            float width = Fonts.harmony.getWidth(text, (double)scale);
            float delta = 1.0F - living.getHealth() / living.getMaxHealth();
            double height = Fonts.harmony.getHeight(true, (double)scale);
            this.blurMatrices
                    .add(new Vector4f(position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height)));
            RenderUtils.fill(
                    e.getStack(),
                    position.x - width / 2.0F - 2.0F,
                    position.y - 2.0F,
                    position.x + width / 2.0F + 2.0F,
                    (float)((double)position.y + height),
                    color1
            );
            RenderUtils.fill(
                    e.getStack(),
                    position.x - width / 2.0F - 2.0F,
                    position.y - 2.0F,
                    position.x + width / 2.0F + 2.0F - (width + 4.0F) * delta,
                    (float)((double)position.y + height),
                    color2
            );
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
            Fonts.harmony.setAlpha(1.0F);
            e.getStack().pop();
         }
      }

      if (this.shared.getCurrentValue()) {
         for (NameTags.NameTagData data : this.sharedPositions) {
            e.getStack().push();
            Vector2f positionx = data.getRender();
            String textx = "§aShared§f | " + data.getDisplayName();
            String displayName = data.getDisplayName();
            String playerName = extractPlayerNameFromDisplayName(displayName);
            if (playerName != null && KillerDetection.getDetectedKillers().contains(playerName)) {
               textx = "§aShared§f | §c" + playerName + " (Killer)§f";
            }

            float scale = this.scale.getCurrentValue();
            float width = Fonts.harmony.getWidth(textx, (double)scale);
            double delta = 1.0 - data.getHealth() / data.getMaxHealth();
            double height = Fonts.harmony.getHeight(true, (double)scale);
            this.blurMatrices
                    .add(
                            new Vector4f(positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, positionx.x + width / 2.0F + 2.0F, (float)((double)positionx.y + height))
                    );
            RenderUtils.fill(
                    e.getStack(),
                    positionx.x - width / 2.0F - 2.0F,
                    positionx.y - 2.0F,
                    positionx.x + width / 2.0F + 2.0F,
                    (float)((double)positionx.y + height),
                    color1
            );
            RenderUtils.fill(
                    e.getStack(),
                    positionx.x - width / 2.0F - 2.0F,
                    positionx.y - 2.0F,
                    (float)((double)(positionx.x + width / 2.0F + 2.0F) - (double)(width + 4.0F) * delta),
                    (float)((double)positionx.y + height),
                    color2
            );
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(e.getStack(), textx, (double)(positionx.x - width / 2.0F), (double)(positionx.y - 1.0F), Color.WHITE, true, (double)scale);
            Fonts.harmony.setAlpha(1.0F);
            e.getStack().pop();
         }
      }
   }
   private String extractPlayerNameFromDisplayName(String displayName) {
      int pipeIndex = displayName.indexOf("|");
      if (pipeIndex != -1) {
         return displayName.substring(0, pipeIndex).trim();
      }
      return displayName;
   }

   private String formatPlayerName(PlayerEntity player) {
      return player.getName().getString();
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();
      this.sharedPositions.clear();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PlayerEntity && !entity.getName().getString().startsWith("CIT-")) {
            double x = MathUtils.interpolate(renderPartialTicks, entity.prevX, entity.getX());
            double y = MathUtils.interpolate(renderPartialTicks, entity.prevY, entity.getY()) + (double)entity.getHeight() + 0.5;
            double z = MathUtils.interpolate(renderPartialTicks, entity.prevZ, entity.getZ());
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            vector.setY(vector.getY() - 2.0F);
            this.entityPositions.put(entity, vector);
         }
      }

      if (this.shared.getCurrentValue()) {
         Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

         for (SharedESPData value : dataMap.values()) {
            double x = value.getPosX();
            double y = value.getPosY() + (double)mc.player.getHeight() + 0.5;
            double z = value.getPosZ();
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            vector.setY(vector.getY() - 2.0F);
            String displayName = value.getDisplayName();
            displayName = displayName
               + "§f | §c"
               + Math.round(value.getHealth())
               + (value.getAbsorption() > 0.0 ? "+" + Math.round(value.getAbsorption()) : "")
               + "HP";
            this.sharedPositions
               .add(new NameTags.NameTagData(displayName, value.getHealth(), value.getMaxHealth(), value.getAbsorption(), new Vec3d(x, y, z), vector));
         }
      }
   }

   private static class NameTagData {
      private final String displayName;
      private final double health;
      private final double maxHealth;
      private final double absorption;
      private final Vec3d position;
      private final Vector2f render;

      public String getDisplayName() {
         return this.displayName;
      }

      public double getHealth() {
         return this.health;
      }

      public double getMaxHealth() {
         return this.maxHealth;
      }

      public double getAbsorption() {
         return this.absorption;
      }

      public Vec3d getPosition() {
         return this.position;
      }

      public Vector2f getRender() {
         return this.render;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof NameTags.NameTagData other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getHealth(), other.getHealth()) != 0) {
            return false;
         } else if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) {
            return false;
         } else if (Double.compare(this.getAbsorption(), other.getAbsorption()) != 0) {
            return false;
         } else {
            Object this$displayName = this.getDisplayName();
            Object other$displayName = other.getDisplayName();
            if (this$displayName == null ? other$displayName == null : this$displayName.equals(other$displayName)) {
               Object this$position = this.getPosition();
               Object other$position = other.getPosition();
               if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                  Object this$render = this.getRender();
                  Object other$render = other.getRender();
                  return this$render == null ? other$render == null : this$render.equals(other$render);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof NameTags.NameTagData;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $health = Double.doubleToLongBits(this.getHealth());
         result = result * 59 + (int)($health >>> 32 ^ $health);
         long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
         result = result * 59 + (int)($maxHealth >>> 32 ^ $maxHealth);
         long $absorption = Double.doubleToLongBits(this.getAbsorption());
         result = result * 59 + (int)($absorption >>> 32 ^ $absorption);
         Object $displayName = this.getDisplayName();
         result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
         Object $position = this.getPosition();
         result = result * 59 + ($position == null ? 43 : $position.hashCode());
         Object $render = this.getRender();
         return result * 59 + ($render == null ? 43 : $render.hashCode());
      }

      @Override
      public String toString() {
         return "NameTags.NameTagData(displayName="
            + this.getDisplayName()
            + ", health="
            + this.getHealth()
            + ", maxHealth="
            + this.getMaxHealth()
            + ", absorption="
            + this.getAbsorption()
            + ", position="
            + this.getPosition()
            + ", render="
            + this.getRender()
            + ")";
      }

      public NameTagData(String displayName, double health, double maxHealth, double absorption, Vec3d position, Vector2f render) {
         this.displayName = displayName;
         this.health = health;
         this.maxHealth = maxHealth;
         this.absorption = absorption;
         this.position = position;
         this.render = render;
      }
   }
}
