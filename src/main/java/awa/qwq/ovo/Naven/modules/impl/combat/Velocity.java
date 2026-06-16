package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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

    private final BooleanValue smart = ValueBuilder.create(this, "Calculate attack amount")
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

    private final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> movePacketQueue = new ConcurrentLinkedQueue<>();
    private final Map<Entity, Vector3d> targets = new HashMap<>();
    private final LinkedBlockingDeque<Packet<ClientGamePacketListener>> interactInbound = new LinkedBlockingDeque<>();

    private boolean isSuspending = false;
    private int suspendTicks = 0;
    private ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket = null;
    private boolean isFlushing = false;
    private boolean shouldFlushMotion = false;
    private Entity attackTarget = null;
    private int attacksRemaining = 0;
    private int totalAttacks = 0;
    public boolean rotateActive = false;
    private int attackCooldown = 0;
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
        double kbX = -clientboundSetEntityMotionPacket.getXa() / 8000.0;
        double kbZ = -clientboundSetEntityMotionPacket.getZa() / 8000.0;
        double kbStrength = Math.sqrt(kbX * kbX + kbZ * kbZ);
        if (mode19Plus.getCurrentValue()) {
            return kbStrength > targetMotion.getCurrentValue() ? 1 : 0;
        }
        int knockbackLevel = EnchantmentHelper.getKnockbackBonus(mc.player);
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
        if (rotateActive) {
            RotationManager.active = false;
            rotateActive = false;
        }
    }

    private void flushPackets() {
        boolean attacked = totalAttacks > 0;

        isFlushing = true;
        targets.clear();
        releasePacket();
        if (clientboundSetEntityMotionPacket != null && mc.getConnection() != null) {
            clientboundSetEntityMotionPacket.handle(mc.getConnection());
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
        attacksRemaining = 0;
        totalAttacks = 0;
        targets.clear();
    }

    private boolean shouldIgnore() {
        if (mc.player == null || mc.level == null) {
            return true;
        }
        if (mc.player.isDeadOrDying() || !mc.player.isAlive() || mc.player.getHealth() <= 0) {
            return true;
        }
        if (mc.player.isSpectator() || mc.player.getAbilities().flying) {
            return true;
        }
        if (ignoreState.isSelected("In Lava") && mc.player.isInLava()) {
            return true;
        }
        if (ignoreState.isSelected("In Water") && mc.player.isInWater()) {
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
        if (mc.player.onClimbable() || mc.player.isSleeping()) {
            return true;
        }
        return mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB);
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
        if (!(mc.hitResult instanceof EntityHitResult hit)) return null;

        Entity entity = hit.getEntity();
        if (entity instanceof LivingEntity && entity != mc.player && entity.isAlive() && !entity.isSpectator()) {
            return entity;
        }
        return null;
    }

    private boolean isValidTarget(Entity target) {
        if (target == null || !target.isAlive()) return false;
        if (target == mc.player) return false;
        if (target instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0)) return false;
        Entity combatTarget = getCombatModuleTarget();
        if (combatTarget != null && combatTarget.equals(target)) return true;
        double distance = getDistanceToEntity(target);
        if (distance <= 3.0) return true;
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        return aura != null && aura.isEnabled() && aura.working && distance <= aura.attackRange.getCurrentValue();
    }

    private boolean isTargetLost() {
        Entity currentCombatTarget = getCombatModuleTarget();
        if (currentCombatTarget == null && attackTarget == null) return true;
        if (multiTarget.getCurrentValue()) {
            return currentCombatTarget == null;
        }
        if (currentCombatTarget != null && attackTarget != null) {
            return !currentCombatTarget.equals(attackTarget);
        }
        return currentCombatTarget == null && attackTarget == null;
    }

    private double getDistanceToEntity(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;

        Vec3 eyePos = mc.player.getEyePosition(1f);
        AABB aabb = entity.getBoundingBox();

        double x = Math.max(aabb.minX, Math.min(eyePos.x, aabb.maxX));
        double y = Math.max(aabb.minY, Math.min(eyePos.y, aabb.maxY));
        double z = Math.max(aabb.minZ, Math.min(eyePos.z, aabb.maxZ));

        return eyePos.distanceTo(new Vec3(x, y, z));
    }

    private void doAttack(Entity target) {
        if (target == null || mc.player == null || mc.gameMode == null) return;
        if (ignoreState.isSelected("No Sprinting") && !mc.player.isSprinting()) {
            log("not sprinting");
            return;
        }
        if (mode19Plus.getCurrentValue()) {
            if (mc.player.getAttackStrengthScale(0.5F) < 1.0F) return;
        }

        boolean wasSprinting = mc.player.isSprinting();

        if (wasSprinting) mc.player.setSprinting(false);
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);

        if (!mode19Plus.getCurrentValue() && wasSprinting) {
            Vec3 vel = mc.player.getDeltaMovement();
            if (EnchantmentHelper.getKnockbackBonus(mc.player) > 0) {
                mc.player.setDeltaMovement(vel.x, vel.y, vel.z);
            } else {
                mc.player.setDeltaMovement(vel.x * 0.6D, vel.y, vel.z * 0.6D);
            }
        }
    }

    private boolean isAllowedPacket(Packet<?> packet) {
        return packet instanceof ClientboundSetEntityMotionPacket
                || packet instanceof ClientboundSetHealthPacket
                || packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundSoundPacket
                || packet instanceof ClientboundPlayerChatPacket
                || packet instanceof ClientboundPlayerCombatKillPacket
                || packet instanceof ClientboundContainerClosePacket
                || packet instanceof ClientboundHurtAnimationPacket
                || packet instanceof ClientboundSetTitleTextPacket
                || packet instanceof ClientboundSetPlayerTeamPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ClientboundDisconnectPacket
                || (packet instanceof ClientboundAnimatePacket
                && ((ClientboundAnimatePacket) packet).getId() != mc.player.getId());
    }

    private void processInteractPackets() {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            this.interactInbound.clear();
            return;
        }

        Packet<ClientGamePacketListener> packet;
        while ((packet = this.interactInbound.poll()) != null) {
            try {
                packet.handle(connection);
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
                || mc.getConnection() == null
                || mc.gameMode == null
                || mc.player.tickCount < 20
                || mc.player.isDeadOrDying()
                || !mc.player.isAlive()
                || mc.player.getHealth() <= 0.0F
                || mc.screen instanceof ProgressScreen
                || mc.screen instanceof DeathScreen
                || Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled();
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() != EventType.RECEIVE) return;
        if (mc.player == null) return;
        Packet<?> packet;
        if (!isBufferMode()) return;
        if (e.getType() != EventType.RECEIVE) return;
        if (shouldIgnore()) {
            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
                log("§4Reset, reason:player invalid");

            }
            return;
        }
        if (isFlushing) return;

        packet = e.getPacket();

        if (isSuspending && packet instanceof ServerboundMovePlayerPacket) {
            movePacketQueue.add(packet);
            e.setCancelled(true);
            return;
        }

        if (packet instanceof ClientboundPlayerPositionPacket) {
            if (ignoreState.isSelected("S08 Cooldown") && s08Cooldown > 0) {
                e.setCancelled(true);
                return;
            }

            if (ignoreState.isSelected("S08 Cooldown")) {
                s08Cooldown = 20;
                log("§cS08 detected");
            }

            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
            }
            clientboundSetEntityMotionPacket = null;
            packetQueue.clear();
            movePacketQueue.clear();
            isFlushing = false;
            shouldFlushMotion = false;
            attackCooldown = 0;
            return;
        }

        if (isSuspending && packet instanceof ClientboundMoveEntityPacket movePacket) {
            e.setCancelled(true);
            Entity entity = movePacket.getEntity(mc.level);
            if (entity != null) {
                Vector3d currentPos = targets.getOrDefault(entity, new Vector3d(entity.getX(), entity.getY(), entity.getZ()));

                if (movePacket.hasPosition()) {
                    double dx = movePacket.getXa() / 4096.0D;
                    double dy = movePacket.getYa() / 4096.0D;
                    double dz = movePacket.getZa() / 4096.0D;

                    targets.put(entity, new Vector3d(currentPos.getX() + dx, currentPos.getY() + dy, currentPos.getZ() + dz));
                }
            }
        }

        if (isSuspending && packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            e.setCancelled(true);
            Entity entity = mc.level.getEntity(teleportPacket.getId());
            if (entity != null) {
                targets.put(entity, new Vector3d(teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ()));
            }
        }

        if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.getId() == mc.player.getId()) {
            if (attackCooldown > 0 && mode19Plus.getCurrentValue()) {
                return;
            }
            e.setCancelled(true);
            double velX = -motion.getXa() / 8000.0;
            double velZ = -motion.getZa() / 8000.0;
            if (Math.abs(velX) <= 0.01 && Math.abs(velZ) <= 0.01) return;

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
                Entity target = getLookTarget();
                if (isValidTarget(target) && mc.player.isSprinting()) {
                    attackTarget = target;
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

        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null || mc.player.isUsingItem()) {
            return;
        }

        if (Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            return;
        }

        if (mc.player.tickCount < 20) {
            resetInteractBlock();
            return;
        }

        if (mc.player.isDeadOrDying() || !mc.player.isAlive() || mc.player.getHealth() <= 0.0F || mc.screen instanceof ProgressScreen || mc.screen instanceof DeathScreen) {
            resetInteractBlock();
            return;
        }

        Packet<?> packet = e.getPacket();
        if (packet instanceof ClientboundLoginPacket) {
            resetInteractBlock();
            return;
        }

        if (this.interactDebugTick > 0 && mc.player.tickCount > 20) {
            if (this.interactStage == InteractStage.BLOCK && packet instanceof ClientboundBlockUpdatePacket blockUpdate && this.interactResult != null && this.interactResult.getBlockPos().equals(blockUpdate.getPos())) {
                processInteractPackets();
                Naven.skipTasks.clear();
                this.interactDebugTick = 0;
                this.interactResult = null;
                return;
            }

            if (!(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ClientboundSetTimePacket)) {
                e.setCancelled(true);
                this.interactInbound.add((Packet<ClientGamePacketListener>) packet);
                return;
            }
        }

        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() != mc.player.getId()) {
                return;
            }

            if (motionPacket.getYa() < 0 || mc.player.getMainHandItem().getItem() instanceof EnderpearlItem) {
                e.setCancelled(false);
                return;
            }

            this.interactGrimTick = mc.player.onGround() ? 2 : 0;
            this.interactDebugTick = (int) packetHoldTime.getCurrentValue();
            this.interactStage = mc.player.onGround() ? InteractStage.TRANSACTION : InteractStage.DELAY_GROUND;
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
                if (!mc.player.onGround()) {
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
                if (!mc.player.onGround()) {
                    this.interactStage = InteractStage.DELAY_GROUND;
                    this.interactAirTicks = 0;
                    return;
                }
                if (blockRayTraceResult != null
                        && !BlockUtils.isAirBlock(blockRayTraceResult.getBlockPos())
                        && mc.player.getBoundingBox().intersects(new AABB(blockRayTraceResult.getBlockPos().above()))) {
                Block targetBlock = mc.level.getBlockState(blockRayTraceResult.getBlockPos()).getBlock();
                if (targetBlock instanceof ChestBlock
                        || targetBlock instanceof CraftingTableBlock
                        || targetBlock instanceof FurnaceBlock
                        || targetBlock instanceof EnchantmentTableBlock
                        || targetBlock instanceof AnvilBlock
                        || targetBlock instanceof BarrelBlock
                        || targetBlock instanceof ShulkerBoxBlock) {
                    return;
                }

                this.interactResult = new BlockHitResult(blockRayTraceResult.getLocation(), blockRayTraceResult.getDirection(), blockRayTraceResult.getBlockPos(), false);
                ((LocalPlayerAccessor) mc.player).setYRotLast(yaw);
                ((LocalPlayerAccessor) mc.player).setXRotLast(pitch);
                RotationManager.setRotations(new Rotation(yaw, pitch).toVec2f());
                if (KillAura.rotation != null) {
                    KillAura.rotation = new Rotation(yaw, pitch).toVec2f();
                }

                processInteractPackets();
                mc.player.connection.send(new Rot(yaw, pitch, mc.player.onGround()));
                mc.player.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, this.interactResult, 0));
                Naven.skipTasks.add(() -> {
                });

                for (int i = 2; i <= 40; i++) {
                    Naven.skipTasks.add(() -> {
                        EventMotion event = new EventMotion(EventType.PRE, mc.player.position().x, mc.player.position().y, mc.player.position().z, yaw, pitch, mc.player.onGround());
                        Naven.getInstance().getRotationManager().onPre(event);
                        if (event.getYaw() != yaw || event.getPitch() != pitch) {
                            mc.player.connection.send(new Rot(event.getYaw(), event.getPitch(), mc.player.onGround()));
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
        targets.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || entry.getKey().isRemoved());
        Stuck stuck = (Stuck) Naven.getInstance().getModuleManager().getModule(Stuck.class);
        //不是这玩意没用吗
        if (stuck.isEnabled() && (stuck.mode.isCurrentMode("Delay") || stuck.mode.isCurrentMode("Packet"))) {
            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
            }
            stuckCooldown = 5;
            return;
        }
        if (stuckCooldown > 0) {
            stuckCooldown--;
            return;
        }
        if (e.type() != EventType.PRE) return;

        if (shouldIgnore()) {
            if (isSuspending) {
                flushPackets();
                endSuspending();
                disableRotate();
                log("§4Reset, reason:player invalid");
            }
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

            boolean onGround = mc.player.onGround();
            boolean movingUp = mc.player.getDeltaMovement().y > 0;
            boolean falling = mc.player.getDeltaMovement().y < 0;
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
                if (reduceAddons.isSelected("Rotate") && clientboundSetEntityMotionPacket != null) {
                    double motionX = -clientboundSetEntityMotionPacket.getXa() / 8000.0;
                    double motionZ = -clientboundSetEntityMotionPacket.getZa() / 8000.0;
                    double motionLength = Math.sqrt(motionX * motionX + motionZ * motionZ);
                    if (motionLength > 0) {
                        float kbYaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
                        RotationManager.setRotations(new Vector2f(kbYaw, mc.player.getXRot()));
                        RotationManager.active = true;
                        rotateActive = true;
                    }
                }
                flushPackets();
                isSuspending = false;
                suspendTicks = 0;
            }
            return;
        }

        if (!isSuspending && attacksRemaining > 0 && attackTarget != null && isValidTarget(attackTarget) && attackCooldown == 0) {
            if (mode19Plus.getCurrentValue() && mc.player.getAttackStrengthScale(0.5F) < 1.0F) {
                attacksRemaining--;
                attackCooldown = mode19Plus.getCurrentValue() ? Math.max(1, (int) (20 / mc.player.getCurrentItemAttackStrengthDelay())) : 0;
                return;
            }

            if (isTargetLost()) {
                log("Hit complete");
                attackTarget = null;
                attacksRemaining = 0;
                disableRotate();
                return;
            }

            if (mc.player.isUsingItem()) {
                return;
            }

            if (attackTarget != null && isValidTarget(attackTarget)) {
                if (attackTarget instanceof Player targetPlayer) {
                    if (AntiBots.isBot(targetPlayer)) {
                        attacksRemaining--;
                        attackCooldown = mode19Plus.getCurrentValue() ? Math.max(1, (int) (20 / mc.player.getCurrentItemAttackStrengthDelay())) : 1;
                        return;
                    }
                }
                doAttack(attackTarget);
                attacksRemaining--;
                attackCooldown = mode19Plus.getCurrentValue() ? Math.max(1, (int) (20 / mc.player.getCurrentItemAttackStrengthDelay())) : 1;
                double decay = 0.6D;
                int kbLevel = EnchantmentHelper.getKnockbackBonus(mc.player);
                if (kbLevel > 0) {
                    decay = Math.max(0.4D, 0.6D - kbLevel * 0.05D);
                }
                log("Reduce(Info:Attack, set motion: " + String.format("%.2f", decay) + (kbLevel > 0 ? " kb:" + kbLevel : "") + ")");

                if (attacksRemaining <= 0) {
                    log("Hit complete");
                    attackTarget = null;
                    disableRotate();
                }
                return;
            }
        }

        if (shouldFlushMotion) {
            releasePacket();
            shouldFlushMotion = false;
        }
    }

    private void releasePacket() {
        while (!movePacketQueue.isEmpty()) {
            Packet<?> p = movePacketQueue.poll();
            if (p != null && mc.getConnection() != null)
                ((Packet<ClientPacketListener>) p).handle(mc.getConnection());
        }
        while (!packetQueue.isEmpty()) {
            Packet<?> p = packetQueue.poll();
            if (p != null && mc.getConnection() != null)
                ((Packet<ClientPacketListener>) p).handle(mc.getConnection());
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput e) {
        if (!isBufferMode()) return;

        if (jump) {
            if (mc.player != null) {
                e.setJump(true);
                ChatUtils.addChatMessage("Reduce(Info:Jump Reset)");
            }
            jump = false;
        }

        if (reduceAddons.isSelected("Auto sprint") && attacksRemaining > 0) {
            e.setForward(1);
            e.setStrafe(0);
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (!isBufferMode()) return;
        if (!isSuspending) return;
        CustomTextRenderer font = Fonts.misans;
        int x = mc.getWindow().getGuiScaledWidth() / 2 - 50;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 15;
        int ticks = suspendTicks;
        String text = "Delay SPacket Ticks : " + ticks;
        double textWidth = font.getWidth(text, true, 0.65);
        font.drawString(e.getStack(), text, mc.getWindow().getGuiScaledWidth() / 2 - textWidth / 2, y - 12,
                new Color(255, 255, 255, 255), true, 0.65);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, 100f, 5f, 2f, Integer.MIN_VALUE);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, Math.min(100f, ticks / 40f * 100f), 5f, 2f, mainColor);
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (!isBufferMode()) return;
        if (!renderServerPos.getCurrentValue() || targets.isEmpty()) return;
        PoseStack poseStack = event.getPMatrixStack();
        for (Map.Entry<Entity, Vector3d> entry : targets.entrySet()) {
            Entity entity = entry.getKey();
            if (!(entity instanceof Player)) continue;
            Vector3d pos = entry.getValue();
            RenderUtils.drawEntitySolidBox(poseStack, pos.getX(), pos.getY(), pos.getZ(),
                    entity.getBbWidth(), entity.getBbHeight(), new Color(0, 200, 0, 60).getRGB());
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
        targets.clear();
        isFlushing = false;
        shouldFlushMotion = false;
        attackCooldown = 0;
        s08Cooldown = 0;
        attackTarget = null;
        attacksRemaining = 0;
        totalAttacks = 0;
        jump = false;
        suspendTicks = 0;
        rotateActive = false;
        resetInteractBlock();
    }

    private enum InteractStage {
        TRANSACTION,
        ROTATION,
        DELAY_GROUND,
        BLOCK,
        IDLE
    }
}
