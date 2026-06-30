package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventDestroyBlock;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "BedAura",
        category = Category.WORLD,
        description = "Automatically finds and breaks nearby beds"
)
public class BedAura extends Module {

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Legit", "Swap")
            .build()
            .getModeValue();

    public final BooleanValue autoTools = ValueBuilder.create(this, "Auto Tools")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue breakRange = ValueBuilder.create(this, "Break Range")
            .setDefaultFloatValue(4.5f)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(5.0f)
            .build()
            .getFloatValue();

    public final BooleanValue allowKillAura = ValueBuilder.create(this, "Allow Kill Aura(Danger)")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue breakDelay = ValueBuilder.create(this, "Break Delay")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMaxFloatValue(20.0F)
            .setMinFloatValue(1.0F)
            .build()
            .getFloatValue();

    public final ModeValue rotationMode = ValueBuilder.create(this, "Rotation Mode")
            .setVisibility(() -> mode.isCurrentMode("Swap"))
            .setDefaultModeIndex(0)
            .setModes("Break Tick", "Always Tick", "Break Tick/End Tick")
            .build()
            .getModeValue();

    public final ModeValue breakMode = ValueBuilder.create(this, "Break Mode")
            .setDefaultModeIndex(0)
            .setModes("Legit", "Packet")
            .build()
            .getModeValue();

    public final ModeValue swapMode = ValueBuilder.create(this, "Swap Mode")
            .setVisibility(() -> mode.isCurrentMode("Swap"))
            .setDefaultModeIndex(0)
            .setModes("Direct", "All Layer", "Hypixel", "Heypixel")
            .build()
            .getModeValue();

    public BlockPos targetBed = null;
    public Vector2f bedRotations = null;
    private BlockPos currentBreakPos = null;
    private boolean isBreaking = false;
    private boolean working = false;
    private long lastBreakTime = 0;
    private BlockPos legitTargetBlock = null;
    private float currentDamage = 0.0f;
    private double bestDistance = Double.MAX_VALUE;

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (isBreaking) {
            mc.options.keyAttack.setDown(false);
        }
        reset();
        RotationManager.active = false;
    }

    private void reset() {
        targetBed = null;
        bedRotations = null;
        currentBreakPos = null;
        legitTargetBlock = null;
        isBreaking = false;
        working = false;
        currentDamage = 0.0f;
    }

    @EventTarget(3)
    public void onPreTick(EventRunTicks event) {
        if (event.type() != EventType.PRE) return;
        if (mc.player == null || mc.level == null) return;

        findNearestBed();
        updateWorkingStatus();

        boolean combatAuraActive = isCombatAuraActive();
        if (combatAuraActive && !allowKillAura.getCurrentValue()) {
            pauseForCombatAura();
            bedRotations = null;
            if (RotationManager.active && isUsingBedAuraRotation()) {
                RotationManager.active = false;
            }
            return;
        }

        if (working && currentBreakPos != null) {
            performBreakTick(combatAuraActive);
        }

        if (isBreaking && (!working || currentBreakPos == null)) {
            stopBreaking();
        }

        updateRotation();

        if (targetBed == null && currentBreakPos == null) {
            if (RotationManager.active) {
                RotationManager.active = false;
            }
            bedRotations = null;
        }
    }

    private void findNearestBed() {
        BlockPos playerPos = mc.player.blockPosition();
        double range = breakRange.getCurrentValue();
        double bestDistance = range + 1;
        BlockPos bestBed = null;

        int searchRange = (int) Math.ceil(range) + 2;

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    double realDistance = Math.sqrt(playerPos.distSqr(pos));
                    if (realDistance > range) continue;

                    BlockState state = mc.level.getBlockState(pos);
                    if (isBed(state) && state.getValue(BedBlock.PART) == BedPart.FOOT) {
                        Vec3 bedCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        double centerDistance = mc.player.getEyePosition().distanceTo(bedCenter);

                        if (centerDistance < bestDistance) {
                            bestDistance = centerDistance;
                            bestBed = pos;
                        }
                    }
                }
            }
        }

        targetBed = bestBed;
    }

    private void updateWorkingStatus() {
        if (targetBed == null) {
            working = false;
            currentBreakPos = null;
            legitTargetBlock = null;
            return;
        }

        if (!canReach(targetBed)) {
            working = false;
            currentBreakPos = null;
            legitTargetBlock = null;
            return;
        }

        working = true;
        if (currentBreakPos != null && mc.level.getBlockState(currentBreakPos).isAir()) {
            currentBreakPos = null;
            legitTargetBlock = null;
        }

        if (currentBreakPos == null) {
            evaluateTarget();
        }
    }


    private double calculateBreakTime(BlockPos pos) {
        if (mc.level == null || mc.player == null) return 0.5;

        BlockState state = mc.level.getBlockState(pos);
        float hardness = state.getDestroySpeed(mc.level, pos);
        if (hardness <= 0.0f) hardness = 0.0001f;

        float destroySpeed = mc.player.getDestroySpeed(state);
        float relativeHardness = destroySpeed / hardness / 30f;
        if (relativeHardness <= 0.0f) relativeHardness = 0.0001f;

        return 1.0 / relativeHardness;
    }

    private boolean canHitBedDirectly(BlockPos bedFoot) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        BlockState state = mc.level.getBlockState(bedFoot);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        Direction facing = state.getValue(BedBlock.FACING);
        boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? bedFoot.relative(facing.getOpposite()) : bedFoot.relative(facing);
        List<BlockPos> parts = List.of(bedFoot, otherPart);
        for (BlockPos part : parts) {
            for (double dx = 0.0; dx <= 1.0; dx += 0.5) {
                for (double dy = 0.0; dy <= 1.0; dy += 0.5) {
                    for (double dz = 0.0; dz <= 1.0; dz += 0.5) {
                        Vec3 point = new Vec3(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                        BlockHitResult hit = mc.level.clip(new ClipContext(eyePos, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                        if (hit.getType() == BlockHitResult.Type.BLOCK && hit.getBlockPos().equals(part)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Vec3 getBestHitPoint(BlockPos bedFoot) {
        if (mc.player == null || mc.level == null) return Vec3.atCenterOf(bedFoot);
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        BlockState state = mc.level.getBlockState(bedFoot);
        if (!(state.getBlock() instanceof BedBlock)) return Vec3.atCenterOf(bedFoot);

        Direction facing = state.getValue(BedBlock.FACING);
        boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? bedFoot.relative(facing.getOpposite()) : bedFoot.relative(facing);
        List<BlockPos> parts = List.of(bedFoot, otherPart);

        Vec3 bestPoint = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos part : parts) {
            for (double dx = 0.0; dx <= 1.0; dx += 0.5) {
                for (double dy = 0.0; dy <= 1.0; dy += 0.5) {
                    for (double dz = 0.0; dz <= 1.0; dz += 0.5) {
                        Vec3 point = new Vec3(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                        BlockHitResult hit = mc.level.clip(new ClipContext(eyePos, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                        if (hit.getType() == BlockHitResult.Type.BLOCK && hit.getBlockPos().equals(part)) {
                            double dist = eyePos.distanceToSqr(point);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestPoint = point;
                            }
                        }
                    }
                }
            }
        }
        return bestPoint != null ? bestPoint : Vec3.atCenterOf(bedFoot);
    }

    private void evaluateTarget() {
        if (targetBed == null) return;
        if (mc.level == null) return;
        if (canHitBedDirectly(targetBed)) {
            currentBreakPos = targetBed;
            legitTargetBlock = null;
            return;
        }
        BlockState bedState = mc.level.getBlockState(targetBed);
        if (!(bedState.getBlock() instanceof BedBlock)) return;

        Direction facing = bedState.getValue(BedBlock.FACING);
        boolean isHead = bedState.getValue(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? targetBed.relative(facing.getOpposite()) : targetBed.relative(facing);

        List<BlockPos> bedParts = new ArrayList<>();
        bedParts.add(targetBed);
        bedParts.add(otherPart);
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        boolean hasAir = false;
        List<BlockPos> solidBlocks = new ArrayList<>();

        for (BlockPos bedPart : bedParts) {
            for (Direction dir : directions) {
                BlockPos offsetPos = bedPart.relative(dir);
                BlockState offsetState = mc.level.getBlockState(offsetPos);
                if (offsetState.isAir()) {
                    hasAir = true;
                    break;
                }
                if (!(offsetState.getBlock() instanceof BedBlock)) {
                    solidBlocks.add(offsetPos);
                }
            }
            if (hasAir) break;
        }

        if (hasAir) {
            currentBreakPos = targetBed;
            legitTargetBlock = null;
            return;
        }

        if (solidBlocks.isEmpty()) {
            currentBreakPos = targetBed;
            legitTargetBlock = null;
            return;
        }

        boolean isPacketMode = breakMode.isCurrentMode("Packet");
        double bestScore = Double.MAX_VALUE;
        BlockPos bestBlock = null;

        for (BlockPos blockPos : solidBlocks) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            if (blockState.getDestroySpeed(mc.level, blockPos) < 0) continue;

            double distance = mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos));

            if (isPacketMode) {
                double time = calculateBreakTime(blockPos);
                if (time < bestScore - 0.00001 || (Math.abs(time - bestScore) < 0.00001 && distance < bestDistance)) {
                    bestScore = time;
                    bestDistance = distance;
                    bestBlock = blockPos;
                }
            } else {
                if (distance < bestScore) {
                    bestScore = distance;
                    bestBlock = blockPos;
                }
            }
        }

        if (bestBlock != null) {
            legitTargetBlock = bestBlock;
            currentBreakPos = bestBlock;
        } else {
            currentBreakPos = targetBed;
            legitTargetBlock = null;
        }
    }

    private boolean canBreak() {
        if (breakDelay.getCurrentValue() <= 0) return true;
        long delayMs = (long) (1000.0 / breakDelay.getCurrentValue());
        return System.currentTimeMillis() - lastBreakTime >= delayMs;
    }

    private void performBreakTick(boolean combatAuraActive) {
        updateTargetRotation();

        if (combatAuraActive && allowKillAura.getCurrentValue()) {
            mc.options.keyAttack.setDown(false);
            if (canBreak()) {
                startPacketBreaking(true);
            }
            return;
        }

        if (breakMode.isCurrentMode("Legit")) {
            if (!isBreaking) {
                isBreaking = true;
                mc.options.keyAttack.setDown(true);
            }
            return;
        }

        mc.options.keyAttack.setDown(false);
        if (canBreak()) {
            startPacketBreaking(false);
        }
    }

    private void stopBreaking() {
        mc.options.keyAttack.setDown(false);
        isBreaking = false;
        lastBreakTime = System.currentTimeMillis();
        currentBreakPos = null;
    }

    private void pauseForCombatAura() {
        if (isBreaking) {
            stopBreaking();
        }
        currentBreakPos = null;
        legitTargetBlock = null;
    }

    private void startPacketBreaking(boolean sendRotationPacket) {
        if (mc.player == null || currentBreakPos == null || bedRotations == null) return;
        Direction direction = getBreakDirection(currentBreakPos);
        if (sendRotationPacket) {
            NetworkUtils.sendPacketNoEvent(new ServerboundMovePlayerPacket.Rot(bedRotations.x, bedRotations.y, mc.player.onGround()));
        }
        NetworkUtils.sendPacketNoEvent(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                currentBreakPos,
                direction
        ));
        NetworkUtils.sendPacketNoEvent(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                currentBreakPos,
                direction
        ));
        isBreaking = true;
        lastBreakTime = System.currentTimeMillis();
    }

    private Direction getBreakDirection(BlockPos pos) {
        if (mc.player == null || mc.level == null) return Direction.UP;

        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 targetPos = isBed(mc.level.getBlockState(pos))
                ? getBestHitPoint(pos)
                : Vec3.atCenterOf(pos);
        BlockHitResult hit = mc.level.clip(new ClipContext(
                eyePos,
                targetPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return hit.getBlockPos().equals(pos) ? hit.getDirection() : Direction.UP;
    }

    private void updateTargetRotation() {
        BlockPos target = currentBreakPos != null ? currentBreakPos : targetBed;
        if (target == null) return;

        Vec3 targetCenter;
        if (isBed(mc.level.getBlockState(target))) {
            targetCenter = getBestHitPoint(target);
        } else {
            targetCenter = new Vec3(target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5);
        }

        Vector2f rotations = RotationUtils.getRotations(targetCenter);
        if (rotations != null) {
            bedRotations = rotations;
            if (!isCombatAuraActive() || !allowKillAura.getCurrentValue()) {
                RotationManager.setRotations(bedRotations);
                RotationManager.active = true;
            }
        }
    }

    private void updateRotation() {
        if (!working) return;
        updateTargetRotation();
    }

    @EventTarget
    public void onDestroy(EventDestroyBlock e) {
        BlockPos destroyedPos = e.getPos();
        if (currentBreakPos != null && currentBreakPos.equals(destroyedPos)) {
            lastBreakTime = System.currentTimeMillis();
            currentDamage = 0.0f;
            currentBreakPos = null;
            legitTargetBlock = null;
        }
        if (targetBed != null && targetBed.equals(destroyedPos)) {
            stopBreaking();
            targetBed = null;
            currentBreakPos = null;
            legitTargetBlock = null;
        }
    }

    private boolean canReach(BlockPos pos) {
        if (mc.player == null) return false;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = Vec3.atCenterOf(pos);
        double distance = eyePos.distanceTo(targetCenter);
        return distance <= breakRange.getCurrentValue();
    }

    public boolean shouldYieldToCombatAura() {
        return isEnabled() && bedRotations != null && !allowKillAura.getCurrentValue() && isCombatAuraActive();
    }

    private boolean isUsingBedAuraRotation() {
        return RotationManager.rotations != null
                && bedRotations != null
                && RotationManager.rotations.x == bedRotations.x
                && RotationManager.rotations.y == bedRotations.y;
    }

    private boolean isCombatAuraActive() {
        KillAura killAura = (KillAura) Naven.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && (KillAura.target != null || !KillAura.targets.isEmpty())) {
            return true;
        }

        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        return aura != null && aura.isEnabled() && (Aura.target != null || !Aura.targets.isEmpty());
    }

    private boolean isBed(BlockState state) {
        return state.getBlock() instanceof BedBlock ||
                state.getBlock() == Blocks.RED_BED ||
                state.getBlock() == Blocks.BLACK_BED ||
                state.getBlock() == Blocks.BLUE_BED ||
                state.getBlock() == Blocks.BROWN_BED ||
                state.getBlock() == Blocks.CYAN_BED ||
                state.getBlock() == Blocks.GRAY_BED ||
                state.getBlock() == Blocks.GREEN_BED ||
                state.getBlock() == Blocks.LIGHT_BLUE_BED ||
                state.getBlock() == Blocks.LIGHT_GRAY_BED ||
                state.getBlock() == Blocks.LIME_BED ||
                state.getBlock() == Blocks.MAGENTA_BED ||
                state.getBlock() == Blocks.ORANGE_BED ||
                state.getBlock() == Blocks.PINK_BED ||
                state.getBlock() == Blocks.PURPLE_BED ||
                state.getBlock() == Blocks.WHITE_BED ||
                state.getBlock() == Blocks.YELLOW_BED;
    }

    private boolean canSeeBlock(BlockPos pos) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        BlockHitResult result = mc.level.clip(new ClipContext(eyePos, blockCenter,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getBlockPos().equals(pos);
    }
}
