package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.PacketUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "CrystalAura",
   category = Category.COMBAT,
   description = "Automatically attacks end crystals"
)
public class CrystalAura extends Module {
   public static Vector2f rotations;
   private Entity entity;
   BooleanValue packet = ValueBuilder.create(this, "Attack on Packet (Danger)").setDefaultBooleanValue(false).build().getBooleanValue();

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof EntitySpawnS2CPacket && this.packet.getCurrentValue()) {
         EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket)e.getPacket();
         if (packet.getEntityType() == EntityType.END_CRYSTAL) {
            EndCrystalEntity pTarget = new EndCrystalEntity(mc.world, packet.getX(), packet.getY(), packet.getZ());
            pTarget.setId(packet.getId());
            if (mc.player.distanceTo(pTarget) <= 4.0F) {
               Vector2f rotations = RotationUtils.getRotations(pTarget);
               mc.getNetworkHandler()
                  .sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotations.getX(), rotations.getY(), mc.player.isOnGround()));
               PacketUtils.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id));
               float currentYaw = mc.player.getYaw();
               float currentPitch = mc.player.getPitch();
               mc.player.setYaw(RotationManager.rotations.x);
               mc.player.setPitch(RotationManager.rotations.y);
               mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(pTarget, false));
               mc.player.swingHand(Hand.MAIN_HAND);
               mc.player.setYaw(currentYaw);
               mc.player.setPitch(currentPitch);
            }
         }
      }
   }

   @EventTarget
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null && mc.world != null) {
         Optional<Entity> any = StreamSupport.<Entity>stream(mc.world.getEntities().spliterator(), true)
            .filter(entityx -> entityx instanceof EndCrystalEntity)
            .findAny();
         rotations = null;
         if (any.isPresent()) {
            Entity entity = any.get();
            Vector2f rots = RotationUtils.getRotations(entity);
            double minDistance = RotationUtils.getMinDistance(entity, rots);
            if (minDistance <= 3.0) {
               rotations = rots;
               this.entity = entity;
            }
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (this.entity != null) {
         float currentYaw = mc.player.getYaw();
         float currentPitch = mc.player.getPitch();
         mc.player.setYaw(rotations.x);
         mc.player.setPitch(rotations.y);
         mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(this.entity, false));
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.player.setYaw(currentYaw);
         mc.player.setPitch(currentPitch);
         this.entity = null;
      }
   }
}
