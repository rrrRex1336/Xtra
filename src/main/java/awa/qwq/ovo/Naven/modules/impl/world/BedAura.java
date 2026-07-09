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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
            mc.options.attackKey.setPressed(false);
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
        if (mc.player == null || mc.world == null) return;

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
        BlockPos playerPos = mc.player.getBlockPos();
        double range = breakRange.getCurrentValue();
        double bestDistance = range + 1;
        BlockPos bestBed = null;

        int searchRange = (int) Math.ceil(range) + 2;

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    double realDistance = Math.sqrt(playerPos.getSquaredDistance(pos));
                    if (realDistance > range) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    if (isBed(state) && state.get(BedBlock.PART) == BedPart.FOOT) {
                        Vec3d bedCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        double centerDistance = mc.player.getEyePos().distanceTo(bedCenter);

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
        if (currentBreakPos != null && mc.world.getBlockState(currentBreakPos).isAir()) {
            currentBreakPos = null;
            legitTargetBlock = null;
        }

        if (currentBreakPos == null) {
            evaluateTarget();
        }
    }


    private double calculateBreakTime(BlockPos pos) {
        if (mc.world == null || mc.player == null) return 0.5;

        BlockState state = mc.world.getBlockState(pos);
        float hardness = state.getHardness(mc.world, pos);
        if (hardness <= 0.0f) hardness = 0.0001f;

        float destroySpeed = mc.player.getBlockBreakingSpeed(state);
        float relativeHardness = destroySpeed / hardness / 30f;
        if (relativeHardness <= 0.0f) relativeHardness = 0.0001f;

        return 1.0 / relativeHardness;
    }

    private boolean canHitBedDirectly(BlockPos bedFoot) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        BlockState state = mc.world.getBlockState(bedFoot);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        Direction facing = state.get(BedBlock.FACING);
        boolean isHead = state.get(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? bedFoot.offset(facing.getOpposite()) : bedFoot.offset(facing);
        List<BlockPos> parts = List.of(bedFoot, otherPart);
        for (BlockPos part : parts) {
            for (double dx = 0.0; dx <= 1.0; dx += 0.5) {
                for (double dy = 0.0; dy <= 1.0; dy += 0.5) {
                    for (double dz = 0.0; dz <= 1.0; dz += 0.5) {
                        Vec3d point = new Vec3d(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (hit.getType() == BlockHitResult.Type.BLOCK && hit.getBlockPos().equals(part)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Vec3d getBestHitPoint(BlockPos bedFoot) {
        if (mc.player == null || mc.world == null) return Vec3d.ofCenter(bedFoot);
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        BlockState state = mc.world.getBlockState(bedFoot);
        if (!(state.getBlock() instanceof BedBlock)) return Vec3d.ofCenter(bedFoot);

        Direction facing = state.get(BedBlock.FACING);
        boolean isHead = state.get(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? bedFoot.offset(facing.getOpposite()) : bedFoot.offset(facing);
        List<BlockPos> parts = List.of(bedFoot, otherPart);

        Vec3d bestPoint = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos part : parts) {
            for (double dx = 0.0; dx <= 1.0; dx += 0.5) {
                for (double dy = 0.0; dy <= 1.0; dy += 0.5) {
                    for (double dz = 0.0; dz <= 1.0; dz += 0.5) {
                        Vec3d point = new Vec3d(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (hit.getType() == BlockHitResult.Type.BLOCK && hit.getBlockPos().equals(part)) {
                            double dist = eyePos.squaredDistanceTo(point);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestPoint = point;
                            }
                        }
                    }
                }
            }
        }
        return bestPoint != null ? bestPoint : Vec3d.ofCenter(bedFoot);
    }

    private void evaluateTarget() {
        if (targetBed == null) return;
        if (mc.world == null) return;
        if (canHitBedDirectly(targetBed)) {
            currentBreakPos = targetBed;
            legitTargetBlock = null;
            return;
        }
        BlockState bedState = mc.world.getBlockState(targetBed);
        if (!(bedState.getBlock() instanceof BedBlock)) return;

        Direction facing = bedState.get(BedBlock.FACING);
        boolean isHead = bedState.get(BedBlock.PART) == BedPart.HEAD;
        BlockPos otherPart = isHead ? targetBed.offset(facing.getOpposite()) : targetBed.offset(facing);

        List<BlockPos> bedParts = new ArrayList<>();
        bedParts.add(targetBed);
        bedParts.add(otherPart);
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        boolean hasAir = false;
        List<BlockPos> solidBlocks = new ArrayList<>();

        for (BlockPos bedPart : bedParts) {
            for (Direction dir : directions) {
                BlockPos offsetPos = bedPart.offset(dir);
                BlockState offsetState = mc.world.getBlockState(offsetPos);
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
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getHardness(mc.world, blockPos) < 0) continue;

            double distance = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos));

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
            mc.options.attackKey.setPressed(false);
            if (canBreak()) {
                startPacketBreaking(true);
            }
            return;
        }

        if (breakMode.isCurrentMode("Legit")) {
            if (!isBreaking) {
                isBreaking = true;
                mc.options.attackKey.setPressed(true);
            }
            return;
        }

        mc.options.attackKey.setPressed(false);
        if (canBreak()) {
            startPacketBreaking(false);
        }
    }

    private void stopBreaking() {
        mc.options.attackKey.setPressed(false);
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
            NetworkUtils.sendPacketNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(bedRotations.x, bedRotations.y, mc.player.isOnGround()));
        }
        NetworkUtils.sendPacketNoEvent(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                currentBreakPos,
                direction
        ));
        NetworkUtils.sendPacketNoEvent(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                currentBreakPos,
                direction
        ));
        isBreaking = true;
        lastBreakTime = System.currentTimeMillis();
    }

    private Direction getBreakDirection(BlockPos pos) {
        if (mc.player == null || mc.world == null) return Direction.UP;

        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        Vec3d targetPos = isBed(mc.world.getBlockState(pos))
                ? getBestHitPoint(pos)
                : Vec3d.ofCenter(pos);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eyePos,
                targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getBlockPos().equals(pos) ? hit.getSide() : Direction.UP;
    }

    private void updateTargetRotation() {
        BlockPos target = currentBreakPos != null ? currentBreakPos : targetBed;
        if (target == null) return;

        Vec3d targetCenter;
        if (isBed(mc.world.getBlockState(target))) {
            targetCenter = getBestHitPoint(target);
        } else {
            targetCenter = new Vec3d(target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5);
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
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = Vec3d.ofCenter(pos);
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
        if (mc.player == null || mc.world == null) return false;
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        BlockHitResult result = mc.world.raycast(new RaycastContext(eyePos, blockCenter,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getBlockPos().equals(pos);
    }
}
