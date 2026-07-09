package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventDispatchPacket;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.player.ContainerStealer;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.PacketUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import org.mixin.accessors.ServerboundMovePlayerPacketAccessor;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;


import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(
        name = "Disabler",
        category = Category.MISC,
        description = "Disables some checks of the anti cheat."
)
public class Disabler extends Module {

   private final BooleanValue logging = ValueBuilder.create(this, "Logging")
           .setDefaultBooleanValue(false).build().getBooleanValue();

   public final BooleanValue aca = ValueBuilder.create(this, "Anti Cheat Addition")
           .setDefaultBooleanValue(false).build().getBooleanValue();

   private final AddonsValue acaAddons = ValueBuilder.create(this, "ACA Addons")
           .setAddonsModes("Aim Step", "Fast Switch", "Perfect Rotation", "Inventory MultiInteraction", "Inventory Frequency")
           .setDefaultSelectedAddons(false, false, false, false, false)
           .setVisibility(aca::getCurrentValue).build().getAddonsValue();

   public final BooleanValue lGrim = ValueBuilder.create(this, "Latest Grim")
           .setDefaultBooleanValue(false).build().getBooleanValue();

   public final AddonsValue grimAddons = ValueBuilder.create(this, "LatestGrim Addons")
           .setAddonsModes("Post", "Duplicate Rot Place", "Aim Modulo 360", "BadPackets A", "BadPackets F", "BadPackets G")
           .setDefaultSelectedAddons(false, false, false, false, false, false)
           .setVisibility(lGrim::getCurrentValue).build().getAddonsValue();

   public final BooleanValue heypixel = ValueBuilder.create(this, "Heypixel Other Check")
           .setDefaultBooleanValue(false).build().getBooleanValue();

   private final AddonsValue heypixelAddons = ValueBuilder.create(this, "Heypixel Addons")
           .setAddonsModes("Exponentially Small")
           .setDefaultSelectedAddons(false)
           .setVisibility(heypixel::getCurrentValue).build().getAddonsValue();

   public final BooleanValue themis = ValueBuilder.create(this, "Themis")
           .setDefaultBooleanValue(false).build().getBooleanValue();

   private final AddonsValue themisAddons = ValueBuilder.create(this, "Themis Addons")
           .setAddonsModes("Blink")
           .setDefaultSelectedAddons(false)
           .setVisibility(themis::getCurrentValue).build().getAddonsValue();

   // ==================== Fields ====================

   private final Random random = new Random();
   private final TimeHelper inventoryTimer = new TimeHelper();
   private final ConcurrentLinkedQueue<PacketData> acaInventoryPackets = new ConcurrentLinkedQueue<>();
   private static final double[] PERFECT_PATTERNS = {0.1, 0.25};

   // Rotation
   private float lastYaw, lastPitch;
   private float lastSentYaw, lastSentPitch;
   private float playerYaw, deltaYaw, lastPlacedDeltaYaw;
   private boolean rotated;

   // Slot
   private int lastSentSlot = -1;

   // BadPackets F/G
   private boolean lastSprinting, lastSneaking;

   // Inventory Frequency
   private boolean inventoryOpen;
   private long inventoryOpenTime;
   private CloseHandledScreenC2SPacket storedClosePacket;
   private long inventoryCloseDelay;

   // Themis Blink
   private long themisBlinkLastSend = System.currentTimeMillis();
   private int themisBlinkCount;

   // ACA Inventory MultiInteraction
   private boolean acaInventoryPass;

   // Grim Post
   private int dispatchEntityActionCount;

   // Grim AimModulo 360
   private int randomCount;

   private record PacketData(Packet<?> packet, long timestamp) {}

   // ==================== Helpers ====================

   private void log(String msg) {
      if (logging.getCurrentValue()) ChatUtils.addChatMessage(msg);
   }

   private boolean isAca(String addon) {
      return aca.getCurrentValue() && acaAddons.isSelected(addon);
   }

   private boolean isGrim(String addon) {
      return lGrim.getCurrentValue() && grimAddons.isSelected(addon);
   }

   private boolean isHeypixel(String addon) {
      return heypixel.getCurrentValue() && heypixelAddons.isSelected(addon);
   }

   private boolean isThemis(String addon) {
      return themis.getCurrentValue() && themisAddons.isSelected(addon);
   }

   private float normalizeYaw(float yaw) {
      while (yaw > 180F) yaw -= 360F;
      while (yaw < -180F) yaw += 360F;
      return yaw;
   }

   // ==================== Rotation Logic ====================

   private boolean shouldModifyRotation(float yaw, float pitch) {
      if (lastYaw == 0F && lastPitch == 0F) return false;
      double yawDelta = Math.abs(normalizeYaw(yaw - lastYaw));
      double pitchDelta = Math.abs(pitch - lastPitch);
      return (yawDelta < 1E-5 && pitchDelta > 1.0) || (pitchDelta < 1E-5 && yawDelta > 1.0);
   }

   private float[] getModifiedRotation(float yaw, float pitch) {
      double yawDelta = Math.abs(normalizeYaw(yaw - lastYaw));
      double pitchDelta = Math.abs(pitch - lastPitch);
      if (yawDelta < 1E-5 && pitchDelta > 1.0) yaw = lastYaw + (float) (random.nextGaussian() * 0.001);
      if (pitchDelta < 1E-5 && yawDelta > 1.0) pitch = lastPitch + (float) (random.nextGaussian() * 0.001);
      return new float[]{yaw, pitch};
   }

   private float[] getAntiPerfectRotation(float yaw, float pitch) {
      if (lastYaw == 0F && lastPitch == 0F) return new float[]{yaw, pitch};
      double yawDelta = Math.abs(normalizeYaw(yaw - lastYaw));
      double pitchDelta = Math.abs(pitch - lastPitch);
      if (!isNoRotation(yawDelta) && isPerfectPattern(yawDelta))
         yaw += (float) (random.nextGaussian() * 0.005);
      if (!isNoRotation(pitchDelta) && isPerfectPattern(pitchDelta))
         pitch += (float) (random.nextGaussian() * 0.005);
      return new float[]{yaw, pitch};
   }

   private boolean isNoRotation(double r) {
      return Math.abs(r) <= 1E-10 || isIntegerMultiple(360.0, r);
   }

   private boolean isPerfectPattern(double r) {
      if (Double.isInfinite(r) || Double.isNaN(r)) return false;
      for (double p : PERFECT_PATTERNS) if (isIntegerMultiple(p, r)) return true;
      return false;
   }

   private boolean isIntegerMultiple(double ref, double val) {
      if (ref == 0.0) return Math.abs(val) <= 1E-10;
      return Math.abs(val / ref - Math.round(val / ref)) <= 1E-10;
   }

   // ==================== Fast Switch ====================

   private void sendIntermediateSlots(int from, int to) {
      int step = to > from ? 1 : -1;
      for (int i = from + step; i != to + step; i += step) {
         PacketUtils.sendPacketNoEvent(new UpdateSelectedSlotC2SPacket(i));
      }
   }

   // ==================== Events ====================

   @EventTarget
   public void badPacketUDetector(EventDispatchPacket e) {
      if (e.getPacket() instanceof PlayerMoveC2SPacket) {
         dispatchEntityActionCount = 0;
      }
      if (isGrim("Post") && e.getPacket() instanceof ClientCommandC2SPacket packet) {
         if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING
                 || packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (dispatchEntityActionCount > 0) log("You may just flagged BadPacketU!");
            dispatchEntityActionCount++;
         }
      }
   }

   @EventTarget(3)
   public void duplicateRotPlaceDisabler(EventPacket e) {
      if (e.getType() != EventType.SEND || e.isCancelled() || mc.player == null) return;

      if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
         ServerboundMovePlayerPacketAccessor acc = (ServerboundMovePlayerPacketAccessor) packet;
         float yaw = acc.getYRot(), pitch = acc.getXRot();

         // Heypixel Exponentially Small
         if (isHeypixel("Exponentially Small")) {
            float dYaw = Math.abs(yaw - lastYaw), dPitch = Math.abs(pitch - lastPitch);
            if (dYaw > 0F && dYaw < 0.005F) { yaw = lastYaw; log("Fixed small yaw: " + dYaw); }
            if (dPitch > 0F && dPitch < 0.005F) { pitch = lastPitch; log("Fixed small pitch: " + dPitch); }
         }

         // Grim AimModulo 360
         if (isGrim("Aim Modulo 360")) {
            yaw += randomCount * 360F;
            if ((randomCount += 5) >= 100) randomCount = 0;
         }

         if (yaw != acc.getYRot()) {
            e.setPacket(packet.changesPosition()
                    ? new PlayerMoveC2SPacket.Full(acc.getX(), acc.getY(), acc.getZ(), yaw, pitch, packet.isOnGround())
                    : new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, packet.isOnGround()));
         }

         // Duplicate Rot Place
         if (isGrim("Duplicate Rot Place")) {
            if (packet.changesLook()) {
               if (acc.getYRot() < 360F && acc.getYRot() > -360F) {
                  e.setPacket(packet.changesPosition()
                          ? new PlayerMoveC2SPacket.Full(acc.getX(), acc.getY(), acc.getZ(), acc.getYRot() + 720F, acc.getXRot(), packet.isOnGround())
                          : new PlayerMoveC2SPacket.LookAndOnGround(acc.getYRot() + 720F, acc.getXRot(), packet.isOnGround()));
               }
               updatePlaceDelta(acc.getYRot());
               if (deltaYaw > 2F && Math.abs(deltaYaw - lastPlacedDeltaYaw) < 1E-4) {
                  log("Disabling DuplicateRotPlace!");
                  e.setPacket(packet.changesPosition()
                          ? new PlayerMoveC2SPacket.Full(acc.getX(), acc.getY(), acc.getZ(), acc.getYRot() + 0.002F, acc.getXRot(), packet.isOnGround())
                          : new PlayerMoveC2SPacket.LookAndOnGround(acc.getYRot() + 0.002F, acc.getXRot(), packet.isOnGround()));
               }
            }
         } else {
            updatePlaceDelta(acc.getYRot());
            if (deltaYaw > 2F && Math.abs(deltaYaw - lastPlacedDeltaYaw) < 1E-4) {
               e.setPacket(packet.changesPosition()
                       ? new PlayerMoveC2SPacket.Full(acc.getX(), acc.getY(), acc.getZ(), acc.getYRot() + 0.002F, acc.getXRot(), packet.isOnGround())
                       : new PlayerMoveC2SPacket.LookAndOnGround(acc.getYRot() + 0.002F, acc.getXRot(), packet.isOnGround()));
            }
         }

         lastSentYaw = yaw;
         lastSentPitch = pitch;

      } else if (e.getPacket() instanceof PlayerInteractBlockC2SPacket && rotated) {
         lastPlacedDeltaYaw = deltaYaw;
         rotated = false;
      }

      // ACA Aim Step / Perfect Rotation
      if (e.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
         ServerboundMovePlayerPacketAccessor acc = (ServerboundMovePlayerPacketAccessor) movePacket;
         float yaw = acc.getYRot(), pitch = acc.getXRot();
         boolean mod = false;

         if (isAca("Aim Step") && shouldModifyRotation(yaw, pitch)) {
            float[] r = getModifiedRotation(yaw, pitch);
            yaw = r[0]; pitch = r[1]; mod = true;
         }
         if (isAca("Perfect Rotation")) {
            float[] r = getAntiPerfectRotation(yaw, pitch);
            if (r[0] != yaw || r[1] != pitch) { yaw = r[0]; pitch = r[1]; mod = true; }
         }
         if (mod) {
            acc.setYRot(yaw);
            acc.setXRot(MathUtils.clampPitch_To90(pitch));
         }
         lastYaw = acc.getYRot();
         lastPitch = acc.getXRot();
      }
   }

   private void updatePlaceDelta(float yaw) {
      float prev = playerYaw;
      playerYaw = yaw;
      deltaYaw = Math.abs(playerYaw - prev);
      rotated = true;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      // reserved
   }

   @EventTarget
   public void onTick(EventRunTicks e) {
      if (isAca("Inventory MultiInteraction") && mc.player != null && mc.world != null) {
         ContainerStealer stealer = (ContainerStealer) Naven.getInstance().getModuleManager().getModule(ContainerStealer.class);
         if (stealer.isEnabled() && stealer.instant.getCurrentValue()) {
            long now = System.currentTimeMillis();
            while (!acaInventoryPackets.isEmpty() && now - acaInventoryPackets.peek().timestamp >= 2500L) {
               PacketData data = acaInventoryPackets.poll();
               if (data != null) processACAPacket(data.packet);
            }
         }
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      Packet<?> packet = event.getPacket();

      // === ACA Inventory MultiInteraction ===
      if (event.getType() == EventType.RECEIVE && isAca("Inventory MultiInteraction")
              && mc.player != null && mc.world != null && !acaInventoryPass) {
         ContainerStealer stealer = (ContainerStealer) Naven.getInstance().getModuleManager().getModule(ContainerStealer.class);
         if (stealer.isEnabled() && stealer.instant.getCurrentValue() && packet instanceof KeepAliveS2CPacket) {
            acaInventoryPackets.add(new PacketData(packet, System.currentTimeMillis()));
            event.setCancelled(true);
         }
      }

      // === Inventory Frequency ===
      if (isAca("Inventory Frequency")) {
         if (storedClosePacket != null && inventoryTimer.delay(inventoryCloseDelay)) {
            PacketUtils.sendPacketNoEvent(storedClosePacket);
            storedClosePacket = null;
         }
         if (packet instanceof OpenScreenS2CPacket) {
            inventoryOpenTime = System.currentTimeMillis();
            inventoryOpen = true;
         }
         if (packet instanceof CloseHandledScreenC2SPacket closePacket) {
            if (inventoryOpen) {
               long duration = System.currentTimeMillis() - inventoryOpenTime;
               if (duration <= 150L) {
                  event.setCancelled(true);
                  storedClosePacket = closePacket;
                  inventoryCloseDelay = 151L - duration;
                  inventoryTimer.reset();
                  inventoryOpen = false;
                  return;
               }
               inventoryOpen = false;
            }
         }
      }

      if (event.getType() != EventType.SEND || event.isCancelled() || mc.player == null) return;
      if (isThemis("Blink")) {
         if (System.currentTimeMillis() - themisBlinkLastSend > 200L) {
            if (themisBlinkCount == 0) {
               if (mc.player != null && mc.player.networkHandler != null) {
                  CommonPongC2SPacket keepAlive = new CommonPongC2SPacket(0);
                  mc.player.networkHandler.sendPacket(keepAlive);
               }
            }
            themisBlinkLastSend = System.currentTimeMillis();
            themisBlinkCount = 0;
         }
         if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly) {
            themisBlinkCount++;
         }
      }

      // === Slot handling ===
      if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
         int slot = slotPacket.getSelectedSlot();

         if (isGrim("BadPackets A") && slot == lastSentSlot && slot != -1) {
            event.setCancelled(true);
            return;
         }

         if (isAca("Fast Switch") && lastSentSlot != -1 && slot != lastSentSlot && mc.getNetworkHandler() != null) {
            event.setCancelled(true);
            sendIntermediateSlots(lastSentSlot, slot);
            lastSentSlot = slot;
            return;
         }

         lastSentSlot = slot;
      }

      // === Grim BadPackets F ===
      if (isGrim("BadPackets F") && packet instanceof ClientCommandC2SPacket pkt) {
         if (pkt.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            if (lastSprinting) event.setCancelled(true);
            lastSprinting = true;
         } else if (pkt.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (!lastSprinting) event.setCancelled(true);
            lastSprinting = false;
         }
      }

      // === Grim BadPackets G ===
      if (isGrim("BadPackets G") && packet instanceof ClientCommandC2SPacket pkt) {
         if (pkt.getMode() == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
            if (lastSneaking) { event.setCancelled(true); return; }
            lastSneaking = true;
         } else if (pkt.getMode() == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
            if (!lastSneaking) { event.setCancelled(true); return; }
            lastSneaking = false;
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void processACAPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() == null) return;
      acaInventoryPass = true;
      try {
         ((Packet<ClientPlayPacketListener>) packet).apply(mc.getNetworkHandler());
      } catch (Exception ignored) {
      } finally {
         acaInventoryPass = false;
      }
   }

   private void reset() {
      acaInventoryPackets.clear();
      lastYaw = lastPitch = 0F;
      lastSentYaw = lastSentPitch = 0F;
      playerYaw = deltaYaw = lastPlacedDeltaYaw = 0F;
      rotated = false;
      lastSentSlot = -1;
      lastSprinting = lastSneaking = false;
      inventoryOpen = false;
      inventoryOpenTime = 0L;
      storedClosePacket = null;
      inventoryCloseDelay = 0L;
      themisBlinkLastSend = System.currentTimeMillis();
      themisBlinkCount = 0;
      dispatchEntityActionCount = 0;
      randomCount = 0;
      acaInventoryPass = false;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      reset();
      if (mc.player != null) lastSentSlot = mc.player.getInventory().selectedSlot;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      reset();
   }
}