package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.movement.LongJump;
import awa.qwq.ovo.Naven.modules.impl.movement.Stuck;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.utils.vector.Vector3d;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.*;
import org.mixin.accessors.ClientboundMoveEntityPacketAccessor;
import org.mixin.accessors.LocalPlayerAccessor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
        name = "Velocity",
        description = "Reduces knockback.",
        category = Category.COMBAT
)
public class Velocity extends Module {

    public static final int mainColor = new Color(150, 45, 45, 255).getRGB();
    private static final float PROGRESS_WIDTH = 100.0F;
    private static final float PROGRESS_HEIGHT = 5.0F;
    private static final float PROGRESS_PLAYER_RANGE = 5.0F;

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Buffer", "Interact Block")
            .setOnUpdate(value -> reset())
            .build()
            .getModeValue();

    private boolean isBufferMode() {
        return this.mode.isCurrentMode("Buffer");
    }

    private boolean isInteractBlockMode() {
        return this.mode.isCurrentMode("Interact Block");
    }

    public final BooleanValue debug = ValueBuilder.create(this, "Verbose Output")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final AddonsValue reduceAddons = ValueBuilder.create(this, "Reduce Addons")
            .setVisibility(() -> isBufferMode())
            .setAddonsModes("Jump reset", "Rotate", "Movement override", "Auto sprint")
            .setDefaultSelectedAddons(false, false, false, false)
            .build()
            .getAddonsValue();

    private final BooleanValue mode19Plus = ValueBuilder.create(this, "1.9+ Mode")
            .setVisibility(() -> isBufferMode())
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue smart = ValueBuilder.create(this, "Redefine Motion")
            .setVisibility(() -> isBufferMode() && !mode19Plus.getCurrentValue())
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final FloatValue attack = ValueBuilder.create(this, "Attack amount")
            .setVisibility(() -> isBufferMode() && !smart.getCurrentValue() && !mode19Plus.getCurrentValue())
            .setDefaultFloatValue(5F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();

    private final FloatValue targetMotion = ValueBuilder.create(this, "Target Motion")
            .setVisibility(() -> isBufferMode() && (smart.getCurrentValue() || mode19Plus.getCurrentValue()))
            .setDefaultFloatValue(0.10F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.05F)
            .setMaxFloatValue(0.45F)
            .build()
            .getFloatValue();

    private final AddonsValue ignoreState = ValueBuilder.create(this, "Ignore state")
            .setVisibility(() -> isBufferMode())
            .setDefaultSelectedAddons(false, true, true, false)
            .setAddonsModes("No Sprinting", "In Lava", "In Water", "On Fire", "S08 Cooldown")
            .build()
            .getAddonsValue();

    private final BooleanValue delayTillGround = ValueBuilder.create(this, "Delay till ground")
            .setVisibility(() -> isBufferMode())
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue multiTarget = ValueBuilder.create(this, "Multi target")
            .setVisibility(() -> isBufferMode())
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue renderServerPos = ValueBuilder.create(this, "Render Server Pos")
            .setVisibility(() -> isBufferMode())
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final FloatValue landDelay = ValueBuilder.create(this, "Land Delay")
            .setVisibility(() -> isInteractBlockMode())
            .setDefaultFloatValue(2F)
            .setFloatStep(1F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();

    private final FloatValue packetHoldTime = ValueBuilder.create(this, "Packet Hold")
            .setVisibility(() -> isInteractBlockMode())
            .setDefaultFloatValue(40F)
            .setFloatStep(5F)
            .setMinFloatValue(10F)
            .setMaxFloatValue(80F)
            .build()
            .getFloatValue();

    private final FloatValue progressXOffset = DragManager.createHiddenPositionValue(this, "Progress Drag X", 0.0F);
    private final FloatValue progressYOffset = DragManager.createHiddenPositionValue(this, "Progress Drag Y", 0.0F);
    private final DragManager progressDragManager = new DragManager(this.progressXOffset, this.progressYOffset);
    private final SmoothAnimationTimer progressAnimation = new SmoothAnimationTimer(0.0F, 0.0F, 0.45F);
    private final SmoothAnimationTimer progressAlpha = new SmoothAnimationTimer(1.0F, 0.0F, 0.35F);

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> movePacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<PendingEntityMove> pendingEntityMoves = new ConcurrentLinkedQueue<>();
    private final Queue<PendingEntityTeleport> pendingEntityTeleports = new ConcurrentLinkedQueue<>();
    private final Map<Entity, Vector3d> targets = new HashMap<>();
    private final LinkedBlockingDeque<Packet<ClientPlayPacketListener>> interactInbound = new LinkedBlockingDeque<>();

    private boolean isSuspending = false;
    private int suspendTicks = 0;
    private EntityVelocityUpdateS2CPacket clientboundSetEntityMotionPacket = null;
    private boolean isFlushing = false;
    private boolean shouldFlushMotion = false;
    private volatile boolean pendingScheduledReset = false;
    private Entity attackTarget = null;
    private Entity velocityTarget = null;
    private int attacksRemaining = 0;
    private int totalAttacks = 0;
    public boolean rotateActive = false;
    private int attackCooldown = 0;
    private int releaseRotateTicks = 0;
    private Vector2f velocityRotation = null;
    private boolean jump = false;
    private int stuckCooldown = 0;
    private int s08Cooldown = 0;
    private InteractStage interactStage = InteractStage.IDLE;
    private int interactGrimTick = -1;
    private int interactDebugTick = 10;
    private BlockHitResult interactResult = null;
    private int interactAirTicks = 0;

    private void log(String message) {
        if (this.debug.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    private int calculateSmartAttacks() {
        if (clientboundSetEntityMotionPacket == null) return (int) attack.getCurrentValue();
        double kbX = -clientboundSetEntityMotionPacket.getVelocityX() / 8000.0;
        double kbZ = -clientboundSetEntityMotionPacket.getVelocityZ() / 8000.0;
        double kbStrength = Math.sqrt(kbX * kbX + kbZ * kbZ);
        if (mode19Plus.getCurrentValue()) {
            return kbStrength > targetMotion.getCurrentValue() ? 1 : 0;
        }
        int knockbackLevel = EnchantmentHelper.getKnockback(mc.player);
        boolean hasKnockback = knockbackLevel > 0;
        boolean isSprinting = mc.player.isSprinting();
        double decay = 0.6D;
        if (hasKnockback) {
            decay = 0.6D - (knockbackLevel * 0.05D);
            decay = Math.max(0.4D, decay);
        }
        float threshold = targetMotion.getCurrentValue();
        int maxAttacks = (int) attack.getCurrentValue();
        int minAttacks = 1;
        if (kbStrength <= threshold) return minAttacks;
        double needed = Math.log(threshold / kbStrength) / Math.log(decay);
        int calculated = (int) Math.ceil(needed);
        if (hasKnockback) {
            calculated += knockbackLevel;
        }
        int result = Math.max(minAttacks, Math.min(calculated, maxAttacks));
        return result;
    }

    private void disableRotate() {
        rotateActive = false;
        releaseRotateTicks = 0;
        velocityRotation = null;
    }

    public boolean shouldApplyRotation() {
        return this.isEnabled() && isBufferMode() && rotateActive && velocityRotation != null;
    }

    public Vector2f getVelocityRotation() {
        return velocityRotation;
    }

    private void flushPackets() {
        boolean attacked = totalAttacks > 0;

        isFlushing = true;
        targets.clear();
        releasePacket();
        if (clientboundSetEntityMotionPacket != null && mc.getNetworkHandler() != null) {
            clientboundSetEntityMotionPacket.apply(mc.getNetworkHandler());
            clientboundSetEntityMotionPacket = null;
        }

        shouldFlushMotion = true;
        isFlushing = false;

        StringBuilder sb = new StringBuilder("Sync, ticks used: ").append(suspendTicks);
        if (attacked) {
            sb.append(" | ")
              .append(smart.getCurrentValue() ? "calculated" : "current")
              .append(" attack: ").append(totalAttacks);
        }
        if (jump) {
            sb.append(" | jump: 1");
        }
        if (!attacked && !jump && !mode19Plus.getCurrentValue()) {
            sb.append(" (idle)");
        }
        log(sb.toString());
    }

    private void endSuspending() {
        isSuspending = false;
        suspendTicks = 0;
        attackTarget = null;
        velocityTarget = null;
        attacksRemaining = 0;
        totalAttacks = 0;
        targets.clear();
    }

    private boolean shouldIgnore() {
        if (mc.player == null || mc.world == null) {
            return true;
        }
        if (mc.player.isDead() || !mc.player.isAlive() || mc.player.getHealth() <= 0) {
            return true;
        }
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            return true;
        }
        if (ignoreState.isSelected("In Lava") && mc.player.isInLava()) {
            return true;
        }
        if (ignoreState.isSelected("In Water") && mc.player.isTouchingWater()) {
            return true;
        }
        if (ignoreState.isSelected("On Fire") && mc.player.isOnFire()) {
            return true;
        }
        if (ignoreState.isSelected("No Sprinting") && !mc.player.isSprinting()) {
            return true;
        }
        if (ignoreState.isSelected("S08 Cooldown") && s08Cooldown > 0) {
            return true;
        }
        if (mc.player.isClimbing() || mc.player.isSleeping()) {
            return true;
        }
        return mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.COBWEB);
    }

    private boolean shouldIgnorePacketThread() {
        return mc.player == null
                || mc.getNetworkHandler() == null
                || mc.interactionManager == null
                || mc.player.isUsingItem()
                || mc.player.age < 20
                || mc.player.isDead()
                || !mc.player.isAlive()
                || mc.player.getHealth() <= 0.0F
                || mc.player.isSpectator()
                || mc.player.getAbilities().flying
                || (ignoreState.isSelected("No Sprinting") && !mc.player.isSprinting())
                || mc.currentScreen instanceof ProgressScreen
                || mc.currentScreen instanceof DeathScreen
                || Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled();
    }

    private void scheduleBufferReset(boolean flushQueued, String reason) {
        if (pendingScheduledReset || mc == null) return;
        pendingScheduledReset = true;
        mc.execute(() -> {
            pendingScheduledReset = false;
            if (flushQueued) {
                flushPackets();
            }
            endSuspending();
            disableRotate();
            clientboundSetEntityMotionPacket = null;
            packetQueue.clear();
            movePacketQueue.clear();
            pendingEntityMoves.clear();
            pendingEntityTeleports.clear();
            targets.clear();
            isFlushing = false;
            shouldFlushMotion = false;
            attackCooldown = 0;
            attackTarget = null;
            velocityTarget = null;
            attacksRemaining = 0;
            totalAttacks = 0;
            releaseRotateTicks = 0;
            jump = false;
            if (reason != null) {
                log(reason);
            }
        });
    }

    private void processPendingEntityUpdates() {
        if (mc.world == null) {
            pendingEntityMoves.clear();
            pendingEntityTeleports.clear();
            return;
        }

        PendingEntityMove move;
        while ((move = pendingEntityMoves.poll()) != null) {
            Entity entity = mc.world.getEntityById(move.entityId);
            if (entity == null) continue;
            Vector3d currentPos = targets.getOrDefault(entity, new Vector3d(entity.getX(), entity.getY(), entity.getZ()));
            targets.put(entity, new Vector3d(currentPos.getX() + move.dx, currentPos.getY() + move.dy, currentPos.getZ() + move.dz));
        }

        PendingEntityTeleport teleport;
        while ((teleport = pendingEntityTeleports.poll()) != null) {
            Entity entity = mc.world.getEntityById(teleport.entityId);
            if (entity != null) {
                targets.put(entity, new Vector3d(teleport.x, teleport.y, teleport.z));
            }
        }
    }

    private Entity getCurrentTarget() {
        Entity combatTarget = getCombatModuleTarget();
        if (combatTarget != null) return combatTarget;
        return getLookTarget();
    }

    private Entity getCombatModuleTarget() {
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura != null && aura.isEnabled() && Aura.target != null && Aura.target.isAlive()) {
            return Aura.target;
        }

        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && KillAura.target != null && KillAura.target.isAlive()) {
            return KillAura.target;
        }

        return null;
    }

    private Entity getLookTarget() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return null;

        Entity entity = hit.getEntity();
        if (entity instanceof LivingEntity && entity != mc.player && entity.isAlive() && !entity.isSpectator()) {
            return entity;
        }
        return null;
    }

    private boolean isValidTarget(Entity target) {
        if (target == null || !target.isAlive()) return false;
        if (target == mc.player) return false;
        if (target instanceof LivingEntity living && (living.isDead() || living.getHealth() <= 0)) return false;
        Entity combatTarget = getCombatModuleTarget();
        if (combatTarget != null && combatTarget.equals(target)) return true;
        double distance = getDistanceToEntity(target);
        if (distance <= 3.0) return true;
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        return aura != null && aura.isEnabled() && aura.working && distance <= aura.attackRange.getCurrentValue();
    }

    private boolean isTargetLost() {
        Entity currentCombatTarget = getCombatModuleTarget();
        if (attackTarget == null) return true;
        if (multiTarget.getCurrentValue()) {
            if (currentCombatTarget != null) {
                return !currentCombatTarget.equals(attackTarget) && !currentCombatTarget.equals(velocityTarget);
            }
            return !isValidTarget(attackTarget);
        }
        if (currentCombatTarget != null && attackTarget != null) {
            return !currentCombatTarget.equals(attackTarget);
        }
        return !isValidTarget(attackTarget);
    }

    private double getDistanceToEntity(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;

        Vec3d eyePos = mc.player.getCameraPosVec(1f);
        Box aabb = entity.getBoundingBox();

        double x = Math.max(aabb.minX, Math.min(eyePos.x, aabb.maxX));
        double y = Math.max(aabb.minY, Math.min(eyePos.y, aabb.maxY));
        double z = Math.max(aabb.minZ, Math.min(eyePos.z, aabb.maxZ));

        return eyePos.distanceTo(new Vec3d(x, y, z));
    }

    private void doAttack(Entity target) {
        if (target == null || mc.player == null || mc.interactionManager == null) return;
        if (ignoreState.isSelected("No Sprinting") && !mc.player.isSprinting()) {
            log("not sprinting");
            return;
        }
        if (mode19Plus.getCurrentValue()) {
            if (mc.player.getAttackCooldownProgress(0.5F) < 1.0F) return;
        }

        boolean wasSprinting = mc.player.isSprinting();

        if (wasSprinting) mc.player.setSprinting(false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (!mode19Plus.getCurrentValue() && wasSprinting) {
            Vec3d vel = mc.player.getVelocity();
            if (EnchantmentHelper.getKnockback(mc.player) > 0) {
                mc.player.setVelocity(vel.x, vel.y, vel.z);
            } else {
                mc.player.setVelocity(vel.x * 0.6D, vel.y, vel.z * 0.6D);
            }
        }
    }

    private boolean isAllowedPacket(Packet<?> packet) {
        return packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof HealthUpdateS2CPacket
                || packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof PlaySoundS2CPacket
                || packet instanceof ChatMessageS2CPacket
                || packet instanceof DeathMessageS2CPacket
                || packet instanceof CloseScreenS2CPacket
                || packet instanceof DamageTiltS2CPacket
                || packet instanceof TitleS2CPacket
                || packet instanceof TeamS2CPacket
                || packet instanceof GameMessageS2CPacket
                || packet instanceof DisconnectS2CPacket
                || (packet instanceof EntityAnimationS2CPacket
                && ((EntityAnimationS2CPacket) packet).getId() != mc.player.getId());
    }

    private void processInteractPackets() {
        ClientPlayNetworkHandler connection = mc.getNetworkHandler();
        if (connection == null) {
            this.interactInbound.clear();
            return;
        }

        Packet<ClientPlayPacketListener> packet;
        while ((packet = this.interactInbound.poll()) != null) {
            try {
                packet.apply(connection);
            } catch (Exception exception) {
                exception.printStackTrace();
                this.interactInbound.clear();
                break;
            }
        }
    }

    private void resetInteractBlock() {
        this.interactStage = InteractStage.IDLE;
        this.interactGrimTick = -1;
        this.interactDebugTick = 0;
        this.interactResult = null;
        this.interactAirTicks = 0;
        processInteractPackets();
    }

    private boolean isInteractBlockInvalid() {
        return mc.player == null
                || mc.getNetworkHandler() == null
                || mc.interactionManager == null
                || mc.player.age < 20
                || mc.player.isDead()
                || !mc.player.isAlive()
                || mc.player.getHealth() <= 0.0F
                || mc.currentScreen instanceof ProgressScreen
                || mc.currentScreen instanceof DeathScreen
                || Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled();
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() != EventType.RECEIVE) return;
        if (mc.player == null) return;
        Packet<?> packet;
        if (!isBufferMode()) return;
        if (e.getType() != EventType.RECEIVE) return;
        if (shouldIgnorePacketThread()) {
            boolean hasBufferedState = isSuspending
                    || clientboundSetEntityMotionPacket != null
                    || !packetQueue.isEmpty()
                    || !movePacketQueue.isEmpty()
                    || attacksRemaining > 0
                    || attackTarget != null;
            scheduleBufferReset(isSuspending || clientboundSetEntityMotionPacket != null || !packetQueue.isEmpty() || !movePacketQueue.isEmpty(),
                    hasBufferedState ? "§4Reset, reason:player invalid" : null);
            return;
        }
        if (isFlushing) return;

        packet = e.getPacket();

        if (isSuspending && packet instanceof PlayerMoveC2SPacket) {
            movePacketQueue.add(packet);
            e.setCancelled(true);
            return;
        }

        if (packet instanceof PlayerPositionLookS2CPacket) {
            if (ignoreState.isSelected("S08 Cooldown") && s08Cooldown > 0) {
                e.setCancelled(true);
                return;
            }

            if (ignoreState.isSelected("S08 Cooldown")) {
                s08Cooldown = 20;
                log("§cS08 detected");
            }

            if (isSuspending) {
                scheduleBufferReset(true, null);
                return;
            }
            clientboundSetEntityMotionPacket = null;
            packetQueue.clear();
            movePacketQueue.clear();
            isFlushing = false;
            shouldFlushMotion = false;
            attackCooldown = 0;
            attackTarget = null;
            velocityTarget = null;
            attacksRemaining = 0;
            totalAttacks = 0;
            releaseRotateTicks = 0;
            return;
        }

        if (isSuspending && packet instanceof EntityS2CPacket movePacket) {
            e.setCancelled(true);
            ClientboundMoveEntityPacketAccessor accessor = (ClientboundMoveEntityPacketAccessor) movePacket;
            if (accessor.getHasPos()) {
                pendingEntityMoves.offer(new PendingEntityMove(
                        accessor.getEntityId(),
                        accessor.getXa() / 4096.0D,
                        accessor.getYa() / 4096.0D,
                        accessor.getZa() / 4096.0D
                ));
            }
        }

        if (isSuspending && packet instanceof EntityPositionS2CPacket teleportPacket) {
            e.setCancelled(true);
            pendingEntityTeleports.offer(new PendingEntityTeleport(
                    teleportPacket.getId(),
                    teleportPacket.getX(),
                    teleportPacket.getY(),
                    teleportPacket.getZ()
            ));
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket motion && motion.getId() == mc.player.getId()) {
            if (attackCooldown > 0 && mode19Plus.getCurrentValue() && attacksRemaining > 0) {
                return;
            }
            e.setCancelled(true);
            double velX = -motion.getVelocityX() / 8000.0;
            double velZ = -motion.getVelocityZ() / 8000.0;
            if (Math.abs(velX) <= 0.01 && Math.abs(velZ) <= 0.01) return;

            attackTarget = null;
            velocityTarget = null;
            attacksRemaining = 0;
            totalAttacks = 0;
            attackCooldown = 0;
            clientboundSetEntityMotionPacket = motion;
            suspendTicks = 0;

            if (mode19Plus.getCurrentValue()) {
                double velStrength = Math.sqrt(velX * velX + velZ * velZ);
                if (velStrength <= targetMotion.getCurrentValue()) {
                    attacksRemaining = 0;
                    totalAttacks = 0;
                } else {
                    totalAttacks = 1;
                    attacksRemaining = 1;
                }
            } else {
                totalAttacks = smart.getCurrentValue() ? calculateSmartAttacks() : (int) attack.getCurrentValue();
                attacksRemaining = totalAttacks;
            }

            if (reduceAddons.isSelected("Jump reset")) {
                jump = true;
            }

            if (!isValidTarget(attackTarget) || attackTarget == null) {
                Entity target = getCurrentTarget();
                if (isValidTarget(target) && mc.player.isSprinting()) {
                    attackTarget = target;
                    velocityTarget = target;
                }
            }

            if (attackTarget != null && renderServerPos.getCurrentValue()) {
                targets.put(attackTarget, new Vector3d(attackTarget.getX(), attackTarget.getY(), attackTarget.getZ()));
            }

            if (!isSuspending) {
                isSuspending = true;
            }
            return;
        }

        if (isSuspending && !isAllowedPacket(packet)) {
            packetQueue.add(packet);
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onHandlePacket(EventHandlePacket e) {
        if (!isInteractBlockMode()) return;

        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null || mc.player.isUsingItem()) {
            return;
        }

        if (Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            return;
        }

        if (mc.player.age < 20) {
            resetInteractBlock();
            return;
        }

        if (mc.player.isDead() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0F || mc.currentScreen instanceof ProgressScreen || mc.currentScreen instanceof DeathScreen) {
            resetInteractBlock();
            return;
        }

        Packet<?> packet = e.getPacket();
        if (packet instanceof GameJoinS2CPacket) {
            resetInteractBlock();
            return;
        }

        if (this.interactDebugTick > 0 && mc.player.age > 20) {
            if (this.interactStage == InteractStage.BLOCK && packet instanceof BlockUpdateS2CPacket blockUpdate && this.interactResult != null && this.interactResult.getBlockPos().equals(blockUpdate.getPos())) {
                processInteractPackets();
                Naven.skipTasks.clear();
                this.interactDebugTick = 0;
                this.interactResult = null;
                return;
            }

            if (!(packet instanceof GameMessageS2CPacket) && !(packet instanceof WorldTimeUpdateS2CPacket)) {
                e.setCancelled(true);
                this.interactInbound.add((Packet<ClientPlayPacketListener>) packet);
                return;
            }
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket motionPacket) {
            if (motionPacket.getId() != mc.player.getId()) {
                return;
            }

            if (motionPacket.getVelocityY() < 0 || mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) {
                e.setCancelled(false);
                return;
            }

            this.interactGrimTick = mc.player.isOnGround() ? 2 : 0;
            this.interactDebugTick = (int) packetHoldTime.getCurrentValue();
            this.interactStage = mc.player.isOnGround() ? InteractStage.TRANSACTION : InteractStage.DELAY_GROUND;
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onPreTick(EventRunTicks e) {
        if (isInteractBlockMode()) {
            if (e.getType() == EventType.POST) {
                return;
            }

            if (isInteractBlockInvalid()) {
                resetInteractBlock();
                return;
            }

            if (this.interactStage == InteractStage.DELAY_GROUND) {
                this.interactDebugTick = Math.max(this.interactDebugTick, 5);
                this.interactAirTicks++;
                if (!mc.player.isOnGround()) {
                    return;
                }
                if (this.interactAirTicks < 2) {
                    return;
                }
                this.interactStage = InteractStage.TRANSACTION;
                this.interactGrimTick = (int) landDelay.getCurrentValue();
                this.interactDebugTick = Math.max(this.interactDebugTick, 20);
            }

            if (this.interactDebugTick > 0) {
                this.interactDebugTick--;
                if (this.interactDebugTick == 0) {
                    processInteractPackets();
                    this.interactStage = InteractStage.IDLE;
                }
            } else {
                this.interactStage = InteractStage.IDLE;
            }

            if (this.interactGrimTick > 0) {
                this.interactGrimTick--;
            }

            float yaw = RotationManager.rotations.getX();
            float pitch = 89.79F;
            BlockHitResult blockRayTraceResult = (BlockHitResult) PlayerUtils.pickCustom(3.7F, yaw, pitch);
            if (this.interactStage == InteractStage.TRANSACTION
                    && this.interactGrimTick == 0) {
                if (!mc.player.isOnGround()) {
                    this.interactStage = InteractStage.DELAY_GROUND;
                    this.interactAirTicks = 0;
                    return;
                }
                if (blockRayTraceResult != null
                        && !BlockUtils.isAirBlock(blockRayTraceResult.getBlockPos())
                        && mc.player.getBoundingBox().intersects(new Box(blockRayTraceResult.getBlockPos().up()))) {
                Block targetBlock = mc.world.getBlockState(blockRayTraceResult.getBlockPos()).getBlock();
                if (targetBlock instanceof ChestBlock
                        || targetBlock instanceof CraftingTableBlock
                        || targetBlock instanceof FurnaceBlock
                        || targetBlock instanceof EnchantingTableBlock
                        || targetBlock instanceof AnvilBlock
                        || targetBlock instanceof BarrelBlock
                        || targetBlock instanceof ShulkerBoxBlock) {
                    return;
                }

                this.interactResult = new BlockHitResult(blockRayTraceResult.getPos(), blockRayTraceResult.getSide(), blockRayTraceResult.getBlockPos(), false);
                ((LocalPlayerAccessor) mc.player).setYRotLast(yaw);
                ((LocalPlayerAccessor) mc.player).setXRotLast(pitch);
                RotationManager.setRotations(new Rotation(yaw, pitch).toVec2f());
                if (KillAura.rotation != null) {
                    KillAura.rotation = new Rotation(yaw, pitch).toVec2f();
                }

                processInteractPackets();
                mc.player.networkHandler.sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, this.interactResult, 0));
                Naven.skipTasks.add(() -> {
                });

                for (int i = 2; i <= 40; i++) {
                    Naven.skipTasks.add(() -> {
                        EventMotion event = new EventMotion(EventType.PRE, mc.player.getPos().x, mc.player.getPos().y, mc.player.getPos().z, yaw, pitch, mc.player.isOnGround());
                        Naven.getInstance().getRotationManager().onPre(event);
                        if (event.getYaw() != yaw || event.getPitch() != pitch) {
                            mc.player.networkHandler.sendPacket(new LookAndOnGround(event.getYaw(), event.getPitch(), mc.player.isOnGround()));
                        }
                    });
                }

                this.interactDebugTick = 20;
                this.interactStage = InteractStage.BLOCK;
                this.interactGrimTick = 0;
                }
            }
            return;
        }
        if (!isBufferMode()) return;

        if (s08Cooldown > 0) {
            s08Cooldown--;
        }
        processPendingEntityUpdates();
        targets.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || entry.getKey().isRemoved());
        Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
        //不是这玩意没用吗
        if (stuck.isEnabled() && (stuck.mode.isCurrentMode("Delay") || stuck.mode.isCurrentMode("Packet"))) {
            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
            }
            attackTarget = null;
            velocityTarget = null;
            attacksRemaining = 0;
            totalAttacks = 0;
            attackCooldown = 0;
            stuckCooldown = 5;
            return;
        }
        if (stuckCooldown > 0) {
            stuckCooldown--;
            return;
        }
        if (e.type() != EventType.PRE) return;

        if (releaseRotateTicks > 0 && !jump) {
            releaseRotateTicks--;
            if (releaseRotateTicks <= 0) {
                disableRotate();
            }
        }

        if (shouldIgnore()) {
            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
                log("§4Reset, reason:player invalid");
            }
            attackTarget = null;
            velocityTarget = null;
            attacksRemaining = 0;
            totalAttacks = 0;
            attackCooldown = 0;
            return;
        }

        if (attackCooldown > 0) attackCooldown--;

        if (isSuspending) {
            suspendTicks++;
            if (attackTarget != null && (!attackTarget.isAlive() || attackTarget.isRemoved())) {
                log("§4Reset, reason:target invalid");
                flushPackets();
                endSuspending();
                disableRotate();
                return;
            }

            if (multiTarget.getCurrentValue()) {
                Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
                KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
                boolean canSwitch = (killAura != null && killAura.switchSize.getCurrentValue() >= 2)
                        || (aura != null && aura.targetTrack.isCurrentMode("Switch"));

                if (canSwitch) {
                    Entity newTarget = getCurrentTarget();
                    if (newTarget != null && !newTarget.equals(attackTarget)) {
                        attackTarget = newTarget;
                        if (renderServerPos.getCurrentValue() && !targets.containsKey(attackTarget)) {
                            targets.put(attackTarget, new Vector3d(attackTarget.getX(), attackTarget.getY(), attackTarget.getZ()));
                        }
                    }
                } else {
                    log("Aura/KillAura not in Switch mode");
                }
            }

            boolean onGround = mc.player.isOnGround();
            boolean movingUp = mc.player.getVelocity().y > 0;
            boolean falling = mc.player.getVelocity().y < 0;
            boolean timeout = suspendTicks >= 20;
            boolean canRelease = delayTillGround.getCurrentValue() ? onGround : onGround || movingUp || falling;
            boolean shouldRelease = canRelease && isValidTarget(attackTarget) && mc.player.isSprinting();

            if (onGround) {
                if (!isValidTarget(attackTarget)) {
                    flushPackets();
                    endSuspending();
                    disableRotate();
                    return;
                }
                if (!mc.player.isSprinting()) {
                    flushPackets();
                    endSuspending();
                    disableRotate();
                    return;
                }
            }

            if (isTargetLost() && attacksRemaining < totalAttacks) {
                log("Hit complete");
                flushPackets();
                endSuspending();
                disableRotate();
                return;
            }

            if (timeout) {
                if (attacksRemaining < totalAttacks && attacksRemaining > 0) {
                    log("Hit complete");
                } else {
                    flushPackets();
                    log("§4Reset, reason:timeout");
                }
                endSuspending();
                disableRotate();
                return;
            }

            if (shouldRelease) {
                targets.clear();
                Entity rotateTarget = isValidTarget(velocityTarget) ? velocityTarget : attackTarget;
                if (reduceAddons.isSelected("Rotate") && onGround && isValidTarget(rotateTarget)) {
                    velocityRotation = RotationUtils.getRotations(mc.player.getCameraPosVec(1.0F), rotateTarget.getBoundingBox().getCenter()).toVec2f();
                    rotateActive = true;
                    releaseRotateTicks = 1;
                }
                flushPackets();
                isSuspending = false;
                suspendTicks = 0;
            }
            return;
        }

        if (!isSuspending && attacksRemaining > 0) {
            if (attackTarget == null || !isValidTarget(attackTarget)) {
                log("Hit complete");
                attackTarget = null;
                velocityTarget = null;
                attacksRemaining = 0;
                totalAttacks = 0;
                attackCooldown = 0;
                disableRotate();
                return;
            }

            if (attackCooldown > 0) {
                return;
            }

            if (reduceAddons.isSelected("Auto sprint") && !mc.player.isSprinting()) {
                mc.options.sprintKey.setPressed(true);
                mc.options.getSprintToggled().setValue(false);
                mc.player.setSprinting(true);
            }

            if (mode19Plus.getCurrentValue() && mc.player.getAttackCooldownProgress(0.5F) < 1.0F) {
                log("Hit complete");
                attackTarget = null;
                velocityTarget = null;
                attacksRemaining = 0;
                totalAttacks = 0;
                attackCooldown = 0;
                disableRotate();
                return;
            }

            if (isTargetLost()) {
                log("Hit complete");
                attackTarget = null;
                velocityTarget = null;
                attacksRemaining = 0;
                totalAttacks = 0;
                attackCooldown = 0;
                disableRotate();
                return;
            }

            if (mc.player.isUsingItem()) {
                log("Hit complete");
                attackTarget = null;
                velocityTarget = null;
                attacksRemaining = 0;
                totalAttacks = 0;
                attackCooldown = 0;
                disableRotate();
                return;
            }

            if (attackTarget instanceof PlayerEntity targetPlayer) {
                if (AntiBots.isBot(targetPlayer)) {
                    log("Hit complete");
                    attackTarget = null;
                    velocityTarget = null;
                    attacksRemaining = 0;
                    totalAttacks = 0;
                    attackCooldown = 0;
                    disableRotate();
                    return;
                }
            }

            doAttack(attackTarget);
            attacksRemaining--;
            attackCooldown = mode19Plus.getCurrentValue() ? Math.max(1, (int) (20 / mc.player.getAttackCooldownProgressPerTick())) : 1;
            log("Reduce(Info: Attack Reduce)");

            if (attacksRemaining <= 0) {
                log("Hit complete");
                attackTarget = null;
                velocityTarget = null;
                totalAttacks = 0;
                attackCooldown = 0;
                disableRotate();
            }
            return;
        }

        if (shouldFlushMotion) {
            releasePacket();
            shouldFlushMotion = false;
        }
    }

    private void releasePacket() {
        while (!movePacketQueue.isEmpty()) {
            Packet<?> p = movePacketQueue.poll();
            if (p != null && mc.getNetworkHandler() != null)
                ((Packet<ClientPlayNetworkHandler>) p).apply(mc.getNetworkHandler());
        }
        while (!packetQueue.isEmpty()) {
            Packet<?> p = packetQueue.poll();
            if (p != null && mc.getNetworkHandler() != null)
                ((Packet<ClientPlayNetworkHandler>) p).apply(mc.getNetworkHandler());
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput e) {
        if (!isBufferMode()) return;

        if (jump) {
            if (mc.player != null) {
                e.setJump(true);
                ChatUtils.addChatMessage("Reduce(Info: Jump Reset)");
            }
            jump = false;
            if (releaseRotateTicks > 0) {
                releaseRotateTicks = 0;
                disableRotate();
            }
        }

        Entity moveTarget = isValidTarget(velocityTarget) ? velocityTarget : attackTarget;
        boolean hasMoveTarget = isValidTarget(moveTarget);
        if (!isSuspending && attacksRemaining > 0 && hasMoveTarget) {
            if (reduceAddons.isSelected("Auto sprint") && !mc.player.isSprinting() && MoveUtils.isMoving()) {
                mc.options.sprintKey.setPressed(true);
                mc.options.getSprintToggled().setValue(false);
                mc.player.setSprinting(true);
            }
        }

        if (reduceAddons.isSelected("Movement override") && hasMoveTarget && (isSuspending || attacksRemaining > 0)) {
            e.setForward(1.0F);
            e.setStrafe(0.0F);
            Vector2f rotations = RotationUtils.getRotations(mc.player.getCameraPosVec(1.0F), moveTarget.getBoundingBox().getCenter()).toVec2f();
            MoveUtils.correctionMovement(e, rotations.x);
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!isBufferMode()) return;
        boolean preview = DragManager.isHudEditorActive();
        boolean shouldShow = preview || isSuspending || hasNearbyPlayerForProgress();

        this.progressAlpha.update(shouldShow);
        this.progressAnimation.target = isSuspending
                ? Math.min(100.0F, suspendTicks / 20.0F * 100.0F)
                : 0.0F;
        this.progressAnimation.update(true);

        float alpha = this.progressAlpha.value;
        if (alpha <= 0.01F && this.progressAnimation.value <= 0.01F && !preview) return;

        CustomTextRenderer font = Fonts.misans;
        float baseX = mc.getWindow().getScaledWidth() / 2.0F - PROGRESS_WIDTH / 2.0F;
        float baseY = mc.getWindow().getScaledHeight() / 2.0F + 15.0F;
        int ticks = preview ? 20 : suspendTicks;
        String text = isSuspending ? "Delaying SPacket Ticks : " + ticks : "Velocity Progress";
        double textWidth = font.getWidth(text, true, 0.65);
        float dragWidth = Math.max(PROGRESS_WIDTH, (float) textWidth);
        this.progressDragManager.update(baseX, baseY - 12.0F, dragWidth, 17.0F);
        float x = this.progressDragManager.getX(baseX);
        float y = this.progressDragManager.getY(baseY);
        font.setAlpha(alpha);
        font.drawString(e.getStack(), text, x + PROGRESS_WIDTH / 2.0F - textWidth / 2.0F, y - 12.0F,
                new Color(255, 255, 255, 255), true, 0.65);
        font.setAlpha(1.0F);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, PROGRESS_WIDTH, PROGRESS_HEIGHT, 2f, alphaColor(0x80000000, alpha));
        RenderUtils.drawRoundedRect(e.getStack(), x, y, Math.min(PROGRESS_WIDTH, this.progressAnimation.value / 100.0F * PROGRESS_WIDTH), PROGRESS_HEIGHT, 2f, alphaColor(mainColor, alpha));
    }

    private boolean hasNearbyPlayerForProgress() {
        if (mc.player == null || mc.world == null) return false;
        double rangeSq = PROGRESS_PLAYER_RANGE * PROGRESS_PLAYER_RANGE;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || player == mc.player) continue;
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) continue;
            if (mc.player.squaredDistanceTo(player) <= rangeSq) return true;
        }
        return false;
    }

    private int alphaColor(int color, float alpha) {
        float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int baseAlpha = color >>> 24;
        return (color & 0x00FFFFFF) | (Math.round(baseAlpha * clampedAlpha) << 24);
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (!isBufferMode()) return;
        if (!renderServerPos.getCurrentValue() || targets.isEmpty()) return;
        MatrixStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : targets.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof PlayerEntity)) continue;
            Vector3d pos = entry.getValue();
            RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                    entity.getWidth(), entity.getHeight(), new Color(0, 200, 0, 60).getRGB());
        }
    }

    @EventTarget
    public void onPlayerRespawn(EventRespawn e) {
        reset();
    }

    @EventTarget
    public void onDisconnect(EventDisconnect e) {
        reset();
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        if (isSuspending) {
            flushPackets();
        }
        releasePacket();
        reset();
        disableRotate();
    }

    private void reset() {
        if (isSuspending) {
            flushPackets();
        }
        endSuspending();
        disableRotate();
        clientboundSetEntityMotionPacket = null;
        packetQueue.clear();
        movePacketQueue.clear();
        pendingEntityMoves.clear();
        pendingEntityTeleports.clear();
        targets.clear();
        isFlushing = false;
        shouldFlushMotion = false;
        pendingScheduledReset = false;
        attackCooldown = 0;
        s08Cooldown = 0;
        attackTarget = null;
        velocityTarget = null;
        attacksRemaining = 0;
        totalAttacks = 0;
        releaseRotateTicks = 0;
        jump = false;
        suspendTicks = 0;
        rotateActive = false;
        progressAnimation.value = 0.0F;
        progressAnimation.target = 0.0F;
        progressAlpha.value = 0.0F;
        resetInteractBlock();
    }

    private record PendingEntityMove(int entityId, double dx, double dy, double dz) {
    }

    private record PendingEntityTeleport(int entityId, double x, double y, double z) {
    }

    private enum InteractStage {
        TRANSACTION,
        ROTATION,
        DELAY_GROUND,
        BLOCK,
        IDLE
    }
}
