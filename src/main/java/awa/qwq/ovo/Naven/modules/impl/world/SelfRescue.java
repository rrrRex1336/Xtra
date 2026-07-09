package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.movement.Stuck;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.LavaCauldronBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "SelfRescue",
        description = "Automatically throws ender pearl when falling into void",
        category = Category.WORLD
)
public class SelfRescue extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public FloatValue fallDistValue = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    public BooleanValue scaffoldValue = ValueBuilder.create(this, "Scaffold")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue autoPearlValue = ValueBuilder.create(this, "Auto Pearl")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue debugValue = ValueBuilder.create(this, "Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue onlyVoidValue = ValueBuilder.create(this, "Only Void")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue attemptTime = ValueBuilder.create(this, "Max Attempt Times")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();

    private ImprovedCalculateThread calculateThread;
    private int attempted;
    private boolean scaffoldEnabled;
    private boolean calculating;
    private boolean scaffoldManuallyDisabled = false;
    private final TimeHelper timer = new TimeHelper();
    private final Random random = new Random();

    public SelfRescue() {
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (calculating) {
            event.setCancelled(true);
        }
    }

    private static class PearlTrajectory {
        public Vec3d hitPosition;
        public BlockPos hitBlockPos;
        public int ticksToHit;
        public boolean fellIntoVoid;
        public List<Vec3d> points = new ArrayList<>();

        public PearlTrajectory() {}
    }

    private PearlTrajectory simulatePearlTrajectory(double startX, double startY, double startZ,
                                                    float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double motionX = -Math.sin(yawRad) * Math.cos(pitchRad) * 1.5F;
        double motionY = -Math.sin(pitchRad) * 1.5F;
        double motionZ = Math.cos(yawRad) * Math.cos(pitchRad) * 1.5F;

        double x = startX;
        double y = startY + 1.62;
        double z = startZ;

        PearlTrajectory trajectory = new PearlTrajectory();

        for (int tick = 0; tick < 100; tick++) {
            x += motionX;
            y += motionY;
            z += motionZ;

            motionY -= 0.03F;

            motionX *= 0.99F;
            motionY *= 0.99F;
            motionZ *= 0.99F;

            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (!mc.world.getBlockState(blockPos).isAir()) {
                trajectory.hitPosition = new Vec3d(x, y, z);
                trajectory.hitBlockPos = blockPos;
                trajectory.ticksToHit = tick;
                break;
            }

            if (y < mc.world.getBottomY()) {
                trajectory.fellIntoVoid = true;
                break;
            }

            if (debugValue.getCurrentValue() && tick % 5 == 0) {
                trajectory.points.add(new Vec3d(x, y, z));
            }
        }

        return trajectory;
    }

    private double assessRotation(float yaw, float pitch) {
        Vec3d playerPos = mc.player.getPos();

        PearlTrajectory trajectory = simulatePearlTrajectory(
                playerPos.x, playerPos.y, playerPos.z, yaw, pitch);

        if (trajectory.fellIntoVoid) {
            return 0.0;
        }

        if (trajectory.hitPosition == null) {
            return 0.0;
        }

        double safetyScore = calculateSafetyScore(trajectory.hitBlockPos);

        double distance = Math.sqrt(
                Math.pow(trajectory.hitPosition.x - playerPos.x, 2) +
                        Math.pow(trajectory.hitPosition.z - playerPos.z, 2));
        double distanceScore = 1.0 / (1.0 + distance / 10.0);

        double heightDiff = Math.abs(trajectory.hitPosition.y - playerPos.y);
        double heightScore = 1.0 / (1.0 + heightDiff / 5.0);

        double timeScore = 1.0 / (1.0 + trajectory.ticksToHit / 20.0);

        return safetyScore * 0.5 + distanceScore * 0.2 + heightScore * 0.2 + timeScore * 0.1;
    }

    private double calculateSafetyScore(BlockPos hitPos) {
        double safety = 0.0;

        if (isSafeBlock(hitPos)) {
            safety += 0.6;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos checkPos = hitPos.add(dx, 0, dz);
                if (isSafeBlock(checkPos)) {
                    safety += 0.05;
                }
            }
        }

        if (isSolidBlock(hitPos.down())) {
            safety += 0.2;
        }

        return Math.min(1.0, safety);
    }

    private boolean isSafeBlock(BlockPos pos) {
        return isSolidBlock(pos) && !isDangerousBlock(pos);
    }

    private boolean isSolidBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).isSolid();
    }

    private boolean isDangerousBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block instanceof LavaCauldronBlock || block instanceof FireBlock;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.player == null) return;

        if (onlyVoidValue.getCurrentValue() && !isAboveVoid()) {
            return;
        }

        Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        if (scaffoldEnabled && scaffold != null && !scaffold.isEnabled()) {
            scaffoldEnabled = false;
            scaffoldManuallyDisabled = true;
            if (debugValue.getCurrentValue()) {
                ChatUtils.addChatMessage("Scaffold was manually disabled, stopping auto-enable");
            }
        }

        if (mc.player.isOnGround()) {
            if (scaffoldEnabled) {
                if (scaffold != null) {
                    scaffold.setEnabled(false);
                }
                scaffoldEnabled = false;
            }
            attempted = 0;
            calculating = false;
            scaffoldManuallyDisabled = false;
        }

        if (event.getType() == EventType.POST) {
            if (calculating && (calculateThread == null || calculateThread.completed)) {
                calculating = false;
                if (calculateThread != null && autoPearlValue.getCurrentValue()) {
                    throwPearl(calculateThread.solutionYaw, calculateThread.solutionPitch);
                }
            }
        }
        if (scaffoldManuallyDisabled) {
            return;
        }

        if (mc.player.getVelocity().y < 0.1 &&
                !isBlockUnder() &&
                mc.player.fallDistance > fallDistValue.getCurrentValue()) {
            Module stuck = Naven.getInstance().getModuleManager().getModule(Stuck.class);

            if (mc.player.getVelocity().y >= -1 &&
                    scaffoldValue.getCurrentValue() &&
                    scaffold != null && !scaffold.isEnabled() &&
                    stuck != null && !stuck.isEnabled()) {
                scaffoldEnabled = true;
                scaffold.setEnabled(true);
                if (debugValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Enabled scaffold for slow fall");
                }
            }
            else if (mc.player.getVelocity().y < -1 &&
                    autoPearlValue.getCurrentValue() &&
                    attempted <= this.attemptTime.getCurrentValue() &&
                    !mc.player.isOnGround()) {
                attempted += 1;
                int pearlSlot = findEnderPearlSlot();
                if (pearlSlot == -1) {
                    if (debugValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("No ender pearl found!");
                    }
                    return;
                }
                mc.player.getInventory().selectedSlot = pearlSlot < 9 ? pearlSlot : 8;
                if (scaffoldEnabled && scaffold != null && scaffold.isEnabled()) {
                    scaffold.setEnabled(false);
                    scaffoldEnabled = false;
                    if (debugValue.getCurrentValue()) {
                        ChatUtils.addChatMessage("Disabled scaffold for pearl throw");
                    }
                }

                calculating = true;
                calculateThread = new ImprovedCalculateThread();
                calculateThread.start();
                if (stuck != null) {
                    stuck.setEnabled(true);
                }

                if (debugValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Attempting pearl throw #" + attempted);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        Module stuck = Naven.getInstance().getModuleManager().getModule(Stuck.class);
        if (stuck != null) {
            stuck.setEnabled(false);
        }
        if (scaffoldEnabled) {
            Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
            if (scaffold != null) {
                scaffold.setEnabled(false);
            }
            scaffoldEnabled = false;
        }

        scaffoldManuallyDisabled = false;

        super.onDisable();
    }

    private int findEnderPearlSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }

        return -1;
    }

    private boolean isBlockUnder() {
        return mc.world.getBlockState(mc.player.getBlockPos().down()).isSolid();
    }

    private boolean isAboveVoid() {
        BlockPos playerPos = mc.player.getBlockPos();
        int playerY = playerPos.getY();
        for (int y = playerY - 1; y >= mc.world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (!(mc.world.getBlockState(checkPos).getBlock() instanceof AirBlock)) {
                return false;
            }
        }

        return true;
    }

    private void throwPearl(float yaw, float pitch) {
        if (!autoPearlValue.getCurrentValue()) {
            return;
        }
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
        NetworkUtils.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));

        if (debugValue.getCurrentValue()) {
            ChatUtils.addChatMessage("Throwing pearl at yaw: " + yaw + ", pitch: " + pitch);
        }
    }

    private class ImprovedCalculateThread extends Thread {
        private int iteration;
        public boolean completed;
        private double solutionE;
        public float solutionYaw, solutionPitch;
        public boolean stop;

        private ImprovedCalculateThread() {
            this.iteration = 0;
            this.stop = false;
            this.completed = false;
        }

        @Override
        public void run() {
            timer.reset();

            solutionYaw = mc.player.getYaw();
            solutionPitch = -45.0f;

            try {
                solutionE = assessRotation(solutionYaw, solutionPitch);
            } catch (Exception e) {
                if (debugValue.getCurrentValue()) {
                    ChatUtils.addChatMessage("Initial assessment failed: " + e.getMessage());
                }
                completed = true;
                return;
            }

            float currentYaw = solutionYaw;
            float currentPitch = solutionPitch;
            double currentE = solutionE;

            double temperature = 50.0F;
            iteration = 0;

            while (temperature > 0.1F && iteration < 150 && !stop) {
                float newYaw = (float) (currentYaw + (random.nextDouble() * 2 - 1) * temperature);
                float newPitch = (float) (currentPitch + (random.nextDouble() * 2 - 1) * temperature / 2);

                newYaw = normalizeYaw(newYaw);
                newPitch = Math.max(-90, Math.min(90, newPitch));

                double newE;
                try {
                    newE = assessRotation(newYaw, newPitch);
                } catch (Exception e) {
                    temperature *= 0.95F;
                    iteration++;
                    continue;
                }

                double deltaE = newE - currentE;

                if (deltaE > 0 || random.nextDouble() < Math.exp(deltaE / temperature)) {
                    currentYaw = newYaw;
                    currentPitch = newPitch;
                    currentE = newE;

                    if (newE > solutionE) {
                        solutionYaw = newYaw;
                        solutionPitch = newPitch;
                        solutionE = newE;

                        if (debugValue.getCurrentValue() && iteration % 20 == 0) {
                            ChatUtils.addChatMessage(String.format("Iteration %d: yaw=%.1f, pitch=%.1f, score=%.4f", iteration, solutionYaw, solutionPitch, solutionE));
                        }
                    }
                }

                temperature *= 0.95F;
                iteration++;
            }

            completed = true;

            if (debugValue.getCurrentValue()) {
                ChatUtils.addChatMessage(String.format("Simulated annealing completed in %d iterations, %dms. Best: yaw=%.1f, pitch=%.1f, score=%.4f", iteration, timer.time(), solutionYaw, solutionPitch, solutionE));
            }
        }

        private float normalizeYaw(float yaw) {
            while (yaw > 180) yaw -= 360;
            while (yaw < -180) yaw += 360;
            return yaw;
        }
    }
}