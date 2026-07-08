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
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.modules.impl.player.AutoMLG;
import awa.qwq.ovo.Naven.modules.impl.visual.ChestESP;
import awa.qwq.ovo.Naven.utils.ChunkUtils;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "ChestAura", description = "Automatically aims at and opens the nearest unopened chest.", category = Category.WORLD)
public class ChestAura extends Module {
    public FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(4.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(2.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();

    public FloatValue nextDelay = ValueBuilder.create(this, "Next Delay")
            .setDefaultFloatValue(500.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(5000.0F)
            .build()
            .getFloatValue();

    public BlockPos rotations = null;
    public Vector2f chestRotations = null;

    private final TimeHelper delayTimer = new TimeHelper();
    private Target target;
    private int rotateTicks;
    private int waitTicks;

    @Override
    public void onEnable() {
        super.onEnable();
        this.target = null;
        this.rotations = null;
        this.chestRotations = null;
        this.rotateTicks = 0;
        this.waitTicks = 0;
        this.delayTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.clearTarget();
        RotationManager.active = false;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.PRE && this.target != null) {
            this.applyRotation();
        }
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (event.getType() != EventType.PRE || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        if (mc.screen instanceof ContainerScreen) {
            if (this.target != null) {
                this.applyRotation();
                this.startCooldown();
            }
            return;
        }

        if (this.shouldPause()) {
            this.clearTarget();
            return;
        }

        if (this.target != null && this.target.phase == Phase.COOLDOWN) {
            this.applyRotation();
            if (this.delayTimer.delay(this.nextDelay.getCurrentValue())) {
                this.clearTarget();
            }
            return;
        }

        if (this.target == null) {
            this.findTarget().ifPresent(found -> {
                this.target = found;
                this.rotations = found.pos;
                this.chestRotations = found.rotation;
                this.rotateTicks = 0;
                this.waitTicks = 0;
            });
        }

        if (this.target == null) {
            this.rotations = null;
            this.chestRotations = null;
            return;
        }

        this.applyRotation();
        if (this.target.phase == Phase.ROTATING) {
            if (++this.rotateTicks >= 2) {
                this.interact(this.target);
                this.target.phase = Phase.WAITING_SCREEN;
                this.waitTicks = 0;
            }
            return;
        }

        if (this.target.phase == Phase.WAITING_SCREEN && ++this.waitTicks > 10) {
            this.startCooldown();
        }
    }

    private Optional<Target> findTarget() {
        double maxDistanceSq = this.range.getCurrentValue() * this.range.getCurrentValue();
        return ChunkUtils.getLoadedBlockEntities()
                .filter(blockEntity -> blockEntity instanceof ChestBlockEntity)
                .map(BlockEntity::getBlockPos)
                .map(this::normalizeChestPos)
                .distinct()
                .filter(this::isClickableChest)
                .filter(pos -> !this.isOpened(pos))
                .filter(pos -> mc.player.distanceToSqr(center(pos)) <= maxDistanceSq)
                .map(pos -> new Target(pos, this.createHitResult(pos), this.getRotation(pos), Phase.ROTATING))
                .min(Comparator.comparingDouble(target -> mc.player.distanceToSqr(center(target.pos))));
    }

    private void interact(Target target) {
        this.chestRotations = target.rotation;
        NetworkUtils.sendPacketNoEvent(new ServerboundMovePlayerPacket.Rot(
                target.rotation.x,
                target.rotation.y,
                mc.player.onGround()
        ));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, target.hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void applyRotation() {
        if (this.target == null) {
            return;
        }
        this.rotations = this.target.pos;
        this.chestRotations = this.target.rotation;
        RotationManager.setRotations(new Vector2f(this.target.rotation.x, this.target.rotation.y));
        RotationManager.active = true;
    }

    private BlockHitResult createHitResult(BlockPos pos) {
        Direction face = this.getFacingFace(pos);
        Vec3 hitVec = new Vec3(
                pos.getX() + 0.5D + face.getStepX() * 0.5D,
                pos.getY() + 0.5D + face.getStepY() * 0.5D,
                pos.getZ() + 0.5D + face.getStepZ() * 0.5D
        );
        return new BlockHitResult(hitVec, face, pos, false);
    }

    private Vector2f getRotation(BlockPos pos) {
        return RotationUtils.getRotations(this.createHitResult(pos).getLocation());
    }

    private Direction getFacingFace(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 blockCenter = center(pos);
        return Direction.getNearest(eye.x - blockCenter.x, eye.y - blockCenter.y, eye.z - blockCenter.z);
    }

    private boolean isClickableChest(BlockPos pos) {
        if (pos == null || mc.level == null) {
            return false;
        }
        BlockState state = mc.level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock
                && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.LEFT;
    }

    private boolean isOpened(BlockPos pos) {
        ChestESP chestESP = (ChestESP) Naven.getInstance().getModuleManager().getModule(ChestESP.class);
        return chestESP != null && chestESP.openedChests != null && chestESP.openedChests.contains(pos);
    }

    private BlockPos normalizeChestPos(BlockPos pos) {
        if (pos == null || mc.level == null) {
            return pos;
        }
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return pos;
        }
        return state.getValue(ChestBlock.TYPE) == ChestType.LEFT
                ? pos.relative(ChestBlock.getConnectedDirection(state))
                : pos;
    }

    private boolean shouldPause() {
        Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        AutoMLG autoMLG = (AutoMLG) Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
        return scaffold != null && scaffold.isEnabled()
                || killAura != null && killAura.isEnabled()
                || aura != null && aura.isEnabled()
                || autoMLG != null && autoMLG.isEnabled();
    }

    private void startCooldown() {
        if (this.target != null) {
            this.target.phase = Phase.COOLDOWN;
        }
        this.delayTimer.reset();
    }

    private void clearTarget() {
        this.target = null;
        this.rotations = null;
        this.chestRotations = null;
        this.rotateTicks = 0;
        this.waitTicks = 0;
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private enum Phase {
        ROTATING,
        WAITING_SCREEN,
        COOLDOWN
    }

    private static class Target {
        private final BlockPos pos;
        private final BlockHitResult hitResult;
        private final Vector2f rotation;
        private Phase phase;

        private Target(BlockPos pos, BlockHitResult hitResult, Vector2f rotation, Phase phase) {
            this.pos = pos;
            this.hitResult = hitResult;
            this.rotation = rotation;
            this.phase = phase;
        }
    }
}
