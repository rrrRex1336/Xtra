package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.combat.Aura;
import awa.qwq.ovo.Naven.modules.impl.combat.KillAura;
import awa.qwq.ovo.Naven.modules.impl.player.AutoMLG;
import awa.qwq.ovo.Naven.modules.impl.visual.ChestESP;
import awa.qwq.ovo.Naven.utils.ChunkUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

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
    private boolean isOpening = false;
    private boolean hasOpened = false;
    private final TimeHelper delayTimer = new TimeHelper();
    private final List<BlockPos> openedChests = new ArrayList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        rotations = null;
        isOpening = false;
        hasOpened = false;
        openedChests.clear();
        delayTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        rotations = null;
        chestRotations = null;
        isOpening = false;
        hasOpened = false;
        RotationManager.active = false;
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundBlockEventPacket packet) {
            if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getB0() == 1 && packet.getB1() == 1) {
                if (!openedChests.contains(packet.getPos())) {
                    openedChests.add(packet.getPos());
                }
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) return;

        if (mc.player == null || mc.level == null) return;
        AutoMLG autoMLG = (AutoMLG) Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
        boolean mlgActive = autoMLG != null;
        boolean inChest = mc.screen instanceof ContainerScreen;
        if (hasOpened && !inChest && rotations != null) {
            if (delayTimer.delay(nextDelay.getCurrentValue())) {
                rotations = null;
                isOpening = false;
                hasOpened = false;
                delayTimer.reset();
            }
        }

        if (!mlgActive && hasOpened && !inChest && rotations != null && !delayTimer.delay(nextDelay.getCurrentValue())) {
            lookAtBlock(rotations);
            return;
        }
        if (inChest && rotations != null) {
            lookAtBlock(rotations);
            return;
        }
        if (rotations == null || (!isOpening && !hasOpened)) {
            BlockPos nearestChest = findNearestUnopenedChest();
            if (nearestChest != null) {
                rotations = nearestChest;
                lookAtBlock(rotations);
            } else {
                rotations = null;
            }
            return;
        }
        if (rotations != null) {
            lookAtBlock(rotations);
        }
    }

    @EventTarget
    public void onRunTicks(EventRunTicks e) {
        if (e.getType() != EventType.PRE) return;

        if (mc.player == null || mc.level == null || rotations == null) return;
        if (isOpening || hasOpened) return;
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 chestPos = new Vec3(rotations.getX() + 0.5, rotations.getY() + 0.5, rotations.getZ() + 0.5);
        double distance = eyePos.distanceTo(chestPos);

        if (distance <= range.getCurrentValue()) {
            Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
            Module killAura = Naven.getInstance().getModuleManager().getModule(KillAura.class);
            Module aura = Naven.getInstance().getModuleManager().getModule(Aura.class);
            AutoMLG autoMLG = (AutoMLG) Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
            boolean mlgActive = autoMLG != null;
            if ((scaffold == null || !scaffold.isEnabled()) && (killAura == null || !killAura.isEnabled()) && (aura == null || !aura.isEnabled()) || !mlgActive) {
                openChest(rotations);
                isOpening = true;
                hasOpened = true;
            }
        }
    }

    private void openChest(BlockPos pos) {
        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private BlockPos findNearestUnopenedChest() {
        List<BlockPos> chests = new ArrayList<>();

        for (BlockEntity be : ChunkUtils.getLoadedBlockEntities().collect(Collectors.toList())) {
            if (be instanceof ChestBlockEntity) {
                BlockPos pos = be.getBlockPos();
                if (isChestOpened(pos)) continue;
                BlockState state = mc.level.getBlockState(pos);
                if (state.hasProperty(ChestBlock.TYPE)) {
                    ChestType type = state.getValue(ChestBlock.TYPE);
                    if (type == ChestType.LEFT) continue;
                }

                double distance = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distance <= range.getCurrentValue() * range.getCurrentValue()) {
                    chests.add(pos);
                }
            }
        }

        if (chests.isEmpty()) return null;
        chests.sort(Comparator.comparingDouble(pos ->
                mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));

        return chests.get(0);
    }

    private boolean isChestOpened(BlockPos pos) {
        ChestESP chestESP = (ChestESP) Naven.getInstance().getModuleManager().getModule(ChestESP.class);
        if (chestESP != null && chestESP.isEnabled()) {
            if (chestESP.openedChests != null && chestESP.openedChests.contains(pos)) {
                return true;
            }
        }
        return openedChests.contains(pos);
    }

    public void lookAtBlock(BlockPos pos) {
        if (pos == null) return;

        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 targetCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        double diffX = targetCenter.x - eyePos.x;
        double diffY = targetCenter.y - eyePos.y;
        double diffZ = targetCenter.z - eyePos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        chestRotations = new Vector2f(yaw, pitch);
        RotationManager.setRotations(chestRotations);
        RotationManager.active = true;
    }

}