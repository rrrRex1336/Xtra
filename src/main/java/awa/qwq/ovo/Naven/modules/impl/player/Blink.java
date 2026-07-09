package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.AntiBots;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.ProjectileData;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas.BasicProjectileData;
import awa.qwq.ovo.Naven.modules.impl.visual.projectiles.datas.EntityArrowData;
import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.RayTraceUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Blink",
   category = Category.PLAYER,
   description = "Suspends all movement packets for teleporting!"
)
public class Blink extends Module {
   private final EntityArrowData arrowData = new EntityArrowData();
   private final BasicProjectileData eggData = new BasicProjectileData(Collections.singleton(EggEntity.class), new Color(255, 238, 154));
   private final BasicProjectileData snowballData = new BasicProjectileData(Collections.singleton(SnowballEntity.class), new Color(255, 255, 255));
   public static final int mainColor = new Color(150, 45, 45, 255).getRGB();
   public static final Set<Class<?>> whitelist = new HashSet<Class<?>>() {
      {
         this.add(HandshakeC2SPacket.class);
         this.add(QueryRequestC2SPacket.class);
         this.add(QueryPingC2SPacket.class);
         this.add(LoginHelloC2SPacket.class);
         this.add(LoginKeyC2SPacket.class);
      }
   };
   private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
   public FloatValue releaseOnDamage = ValueBuilder.create(this, "Release Ticks on Damage")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(50.0F)
      .setDefaultFloatValue(20.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue releaseSpeed = ValueBuilder.create(this, "Release Speed (Tick)")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue maxTicks = ValueBuilder.create(this, "Max Ticks")
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(500.0F)
      .setDefaultFloatValue(200.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue playerDistance = ValueBuilder.create(this, "Player Distance")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(4.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue tntDistance = ValueBuilder.create(this, "TNT Distance")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue projectilesExpands = ValueBuilder.create(this, "Fake Player HitBoxes")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(3.0F)
      .setDefaultFloatValue(0.2F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
   private boolean disabling = false;
   private OtherClientPlayerEntity fakePlayer;
   private int shouldReleaseTicks = 0;
   private int releasedTicks = 0;

   private long getBlinkTicks() {
      return this.packets.stream().filter(packet -> packet instanceof PlayerMoveC2SPacket).count();
   }

   private void handleMove(PlayerMoveC2SPacket packet) {
      this.fakePlayer
         .updateTrackedPositionAndAngles(
            packet.getX(this.fakePlayer.getX()),
            packet.getY(this.fakePlayer.getY()),
            packet.getZ(this.fakePlayer.getZ()),
            packet.getYaw(this.fakePlayer.getYaw()),
            packet.getPitch(this.fakePlayer.getPitch()),
            3
         );
      if (packet.changesLook()) {
         this.fakePlayer.setYaw(packet.getYaw(this.fakePlayer.getYaw()));
         this.fakePlayer.setHeadYaw(packet.getYaw(this.fakePlayer.getYaw()));
         this.fakePlayer.setPitch(packet.getPitch(this.fakePlayer.getPitch()));
      }
   }

   private void releaseTick() {
      while (!this.packets.isEmpty()) {
         Packet<?> poll = this.packets.poll();
         NetworkUtils.sendPacketNoEvent(poll);
         if (poll instanceof PlayerMoveC2SPacket) {
            this.releasedTicks++;
            this.handleMove((PlayerMoveC2SPacket)poll);
            break;
         }
      }
   }

   @Override
   public void onEnable() {
      this.packets.clear();
      this.shouldReleaseTicks = 0;
      this.disabling = false;
      this.fakePlayer = new BlinkingPlayer(mc.player);
      this.fakePlayer.setSprinting(mc.player.isSprinting());
      mc.world.addEntity(this.fakePlayer);
   }

   @Override
   public void onDisable() {
      if (this.fakePlayer != null) {
         mc.world.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
         this.fakePlayer = null;
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      int x = mc.getWindow().getScaledWidth() / 2 - 50;
      int y = mc.getWindow().getScaledHeight() / 2 + 15;
      this.progress.update(true);
      RenderUtils.drawRoundedRect(e.getStack(), (float)x, (float)y, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
      RenderUtils.drawRoundedRect(e.getStack(), (float)x, (float)y, this.progress.value, 5.0F, 2.0F, mainColor);
   }

   private boolean isPlayerNear(double distance) {
      long players = mc.world.getPlayers().stream().filter(player -> {
         if (player == mc.player) {
            return false;
         } else if (player instanceof BlinkingPlayer) {
            return false;
         } else if (Teams.isSameTeam(player)) {
            return false;
         } else if (FriendManager.isFriend(player)) {
            return false;
         } else if (AntiBots.isBot(player)) {
            return false;
         } else {
            Vec3d eyePosition = player.getEyePos();
            Vec3d closestPoint = RotationUtils.getClosestPoint(eyePosition, this.fakePlayer.getBoundingBox());
            return eyePosition.distanceTo(closestPoint) < distance;
         }
      }).count();
      return players > 0L;
   }

   private boolean isTNTNear(double distance) {
      Stream<Entity> stream = StreamSupport.stream(mc.world.getEntities().spliterator(), true);
      long tnt = stream.filter(entity -> entity instanceof TntEntity && (double)this.fakePlayer.distanceTo(entity) <= distance).count();
      return tnt > 0L;
   }

   private boolean isArrowNear(double expands) {
      for (Entity entity : mc.world.getEntities()) {
         ProjectileData data;
         if (entity instanceof ArrowEntity) {
            data = this.arrowData;
         } else if (entity instanceof EggEntity) {
            data = this.eggData;
         } else {
            if (!(entity instanceof SnowballEntity)) {
               continue;
            }

            data = this.snowballData;
         }

         if (data != null && this.checkProjectile(entity, data, expands)) {
            return true;
         }
      }

      return false;
   }

   private boolean checkProjectile(Entity entity, ProjectileData projectileInfo, double expands) {
      ClientPlayerEntity thePlayer = mc.player;
      ClientWorld theWorld = mc.world;
      double posX = entity.getX();
      double posY = entity.getY();
      double posZ = entity.getZ();
      double motionX = entity.getVelocity().x;
      double motionY = entity.getVelocity().y;
      double motionZ = entity.getVelocity().z;

      while (true) {
         float data1 = projectileInfo.getData1();
         float data2 = projectileInfo.getData2();
         Box aabb = new Box(posX - (double)data1, posY, posZ - (double)data1, posX + (double)data1, posY + (double)data2, posZ + (double)data1);
         Vec3d vec3 = new Vec3d(posX, posY, posZ);
         Vec3d vec3WithMotion = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
         HitResult movingObj = RayTraceUtils.rayTraceBlocks(vec3, vec3WithMotion, false, entity instanceof ArrowEntity, false, entity);
         List<Entity> entities = theWorld.getOtherEntities(
            thePlayer, aabb.shrink(motionX, motionY, motionZ).stretch(1.0, 1.0, 1.0).expand(expands, expands, expands)
         );
         if (entities.contains(this.fakePlayer)) {
            return true;
         }

         posX += motionX;
         posY += motionY;
         posZ += motionZ;
         if (!movingObj.getType().equals(Type.MISS) || posY < -128.0) {
            return false;
         }

         motionX *= entity.isTouchingWater() ? 0.8 : 0.99;
         double var39 = motionY * (entity.isTouchingWater() ? 0.8 : 0.99);
         motionZ *= entity.isTouchingWater() ? 0.8 : 0.99;
         motionY = var39 - (double)projectileInfo.getGravity();
      }
   }

   private boolean isPlayerInDanger() {
      return this.isTNTNear((double)this.tntDistance.getCurrentValue())
         || this.isPlayerNear((double)this.playerDistance.getCurrentValue())
         || this.isArrowNear((double)this.projectilesExpands.getCurrentValue());
   }

   @Override
   public void setEnabled(boolean enabled) {
      if (mc.player != null) {
         if (enabled) {
            super.setEnabled(true);
         } else if (!this.disabling) {
            this.disabling = true;
         } else if (this.packets.isEmpty()) {
            super.setEnabled(false);
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         this.setSuffix("Delay C03s : " + this.getBlinkTicks());
         this.progress.target = MathHelper.clamp((float) this.getBlinkTicks() / this.maxTicks.getCurrentValue() * 100.0F, 0.0F, 100.0F);
         this.releasedTicks = 0;
         if (mc.player.hurtTime == 10) {
            this.shouldReleaseTicks = this.shouldReleaseTicks + (int)this.releaseOnDamage.getCurrentValue();
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.shouldReleaseTicks > 0 && !this.packets.isEmpty()) {
            this.releaseTick();
            this.shouldReleaseTicks--;
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.isPlayerInDanger() && !this.packets.isEmpty()) {
            this.releaseTick();
         }

         while (
            (float)this.releasedTicks < this.releaseSpeed.getCurrentValue()
               && (float)this.getBlinkTicks() >= this.maxTicks.getCurrentValue()
               && !this.packets.isEmpty()
         ) {
            this.releaseTick();
         }

         if (this.disabling) {
            while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && !this.packets.isEmpty()) {
               this.releaseTick();
            }

            if (this.packets.isEmpty()) {
               this.setEnabled(false);
            }
         }
      }
   }

   @EventTarget(4)
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.SEND && mc.player != null && !e.isCancelled()) {
         if (whitelist.contains(e.getPacket().getClass())) {
            return;
         }

         e.setCancelled(true);
         this.packets.offer(e.getPacket());
      }
   }
}
