package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@ModuleInfo(
        name = "AutoMLG",
        description = "Automatically places water when falling",
        category = Category.PLAYER
)
public class AutoMLG extends Module {
    public static AutoMLG INSTANCE;

    private final FloatValue triggerDistance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private final FloatValue predictTicks = ValueBuilder.create(this, "Predict Ticks")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .build()
            .getFloatValue();

    private final BooleanValue solidCheck = ValueBuilder.create(this, "Solid Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue recovery = ValueBuilder.create(this, "Recovery")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    @Getter
    public Vector2f targetRotation = null;
    public boolean rotation = false;
    public BlockPos above = null;

    private float accumulatedFall;
    private double lastY;
    private Integer slotToRestore;
    private boolean waterPlaced;
    private boolean recoveryActive;
    private int recoveryDelay;
    private int recoveryCountdown;
    private Integer waterBucketSlot;
    private BlockPos placedWaterPos;
    private boolean readyToPlace;
    private int postPlaceCooldown;
    private int postActionCooldown;
    private int extraCooldown;
    private boolean placingWater;
    @Getter
    private boolean collectingWater;

    public AutoMLG() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.targetRotation = null;
        this.rotation = false;
        this.above = null;
        this.slotToRestore = null;
        this.waterPlaced = false;
        this.recoveryActive = false;
        this.recoveryDelay = 0;
        this.recoveryCountdown = 0;
        this.waterBucketSlot = null;
        this.placedWaterPos = null;
        this.readyToPlace = false;
        this.postPlaceCooldown = 0;
        this.postActionCooldown = 0;
        this.extraCooldown = 0;
        this.accumulatedFall = 0.0F;
        this.placingWater = false;
        this.collectingWater = false;

        this.lastY = mc.player != null ? mc.player.getY() : 0.0D;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null && this.slotToRestore != null) {
            mc.player.getInventory().selectedSlot = this.slotToRestore;
        }
        this.targetRotation = null;
        this.rotation = false;
        this.above = null;
        this.slotToRestore = null;
        this.waterPlaced = false;
        this.recoveryActive = false;
        this.recoveryDelay = 0;
        this.recoveryCountdown = 0;
        this.waterBucketSlot = null;
        this.placedWaterPos = null;
        this.readyToPlace = false;
        this.postPlaceCooldown = 0;
        this.postActionCooldown = 0;
        this.extraCooldown = 0;
        this.accumulatedFall = 0.0F;
        this.placingWater = false;
        this.collectingWater = false;
        super.onDisable();
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        this.targetRotation = null;
        this.rotation = false;
        this.placingWater = false;
        this.collectingWater = this.recoveryActive;

        if (mc.player.isOnGround()
                || mc.player.getAbilities().flying
                || mc.player.isWet()
                || mc.player.isInLava()) {
            this.accumulatedFall = 0.0F;
        } else {
            double deltaY = mc.player.getY() - this.lastY;
            if (deltaY < 0.0D) {
                this.accumulatedFall -= (float) deltaY;
            }
        }
        this.lastY = mc.player.getY();

        if (this.postPlaceCooldown > 0) {
            --this.postPlaceCooldown;
        }
        if (this.postActionCooldown > 0) {
            --this.postActionCooldown;
        }
        if (this.extraCooldown > 0) {
            --this.extraCooldown;
        }

        if (this.slotToRestore != null) {
            mc.player.getInventory().selectedSlot = this.slotToRestore;
            this.slotToRestore = null;
        }

        if (mc.player.isFallFlying()) {
            this.lastY = mc.player.getY();
            return;
        }

        if (mc.player.isOnGround() || this.accumulatedFall <= 0.0F) {
            this.waterPlaced = false;
            this.readyToPlace = false;
        }

        if (this.recoveryActive) {
            this.collectingWater = true;

            if (this.recoveryDelay > 0) {
                --this.recoveryDelay;
                return;
            }

            if (this.recoveryCountdown-- <= 0) {
                this.recoveryActive = false;
                this.collectingWater = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.above = null;
                return;
            }

            if (this.waterBucketSlot == null) {
                int bucketSlot = -1;
                for (int i = 0; i < 9; ++i) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
                        bucketSlot = i;
                        break;
                    }
                }
                if (bucketSlot < 0) {
                    this.recoveryActive = false;
                    this.collectingWater = false;
                    this.waterBucketSlot = null;
                    this.placedWaterPos = null;
                    this.above = null;
                    return;
                }
                this.waterBucketSlot = bucketSlot;
            }

            ItemStack bucketStack = mc.player.getInventory().getStack(this.waterBucketSlot);
            if (bucketStack.getItem() == Items.WATER_BUCKET) {
                this.recoveryActive = false;
                this.collectingWater = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.above = null;
                this.postPlaceCooldown = Math.max(this.postPlaceCooldown, 1);
                return;
            }

            if (this.placedWaterPos == null) {
                this.recoveryActive = false;
                this.collectingWater = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.above = null;
                return;
            }

            FluidState fluidState = mc.world.getFluidState(this.placedWaterPos);
            if (!(fluidState.getFluid() == Fluids.WATER && fluidState.isStill())) {
                this.recoveryActive = false;
                this.collectingWater = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.above = null;
                return;
            }

            Vec3d eyesPos = mc.player.getCameraPosVec(1.0F);
            Vec3d blockCenter = Vec3d.ofCenter(this.placedWaterPos);
            double diffX = blockCenter.x - eyesPos.x;
            double diffY = blockCenter.y - eyesPos.y;
            double diffZ = blockCenter.z - eyesPos.z;
            double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
            float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
            Vector2f recoveryRotation = new Vector2f(yaw, pitch);

            Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
            Vec3d direction = Vec3d.fromPolar(recoveryRotation.getY(), recoveryRotation.getX());
            Vec3d endPos = eyePos.add(direction.multiply(4.5D));
            BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, mc.player));

            if (hit.getType() == HitResult.Type.MISS || !hit.getBlockPos().equals(this.placedWaterPos)) {
                this.recoveryActive = false;
                this.collectingWater = false;
                this.waterBucketSlot = null;
                this.placedWaterPos = null;
                this.above = null;
                return;
            }
            this.targetRotation = recoveryRotation;
            this.rotation = recoveryRotation != null;
            if (recoveryRotation != null) {
                RotationManager.setRotations(recoveryRotation);
                RotationManager.active = true;
            }
            if (this.waterBucketSlot >= 0 && this.waterBucketSlot <= 8) {
                this.slotToRestore = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = this.waterBucketSlot;
            }
            float originalYaw = mc.player.getYaw();
            float originalPitch = mc.player.getPitch();
            if (recoveryRotation != null) {
                mc.player.setYaw(recoveryRotation.getX());
                mc.player.setPitch(recoveryRotation.getY());
            }
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (recoveryRotation != null) {
                mc.player.setYaw(originalYaw);
                mc.player.setPitch(originalPitch);
            }
            return;
        }

        if (!this.waterPlaced
                && !this.recoveryActive
                && this.placedWaterPos == null
                && this.postPlaceCooldown <= 0
                && this.postActionCooldown <= 0
                && this.accumulatedFall <= 0.5F) {

            int bucketSlot = -1;
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() == Items.BUCKET) {
                    bucketSlot = i;
                    break;
                }
            }

            if (bucketSlot >= 0) {
                int waterBucketSlotLocal = bucketSlot;
                BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                BlockPos closestPos = null;
                double closestDistSq = Double.POSITIVE_INFINITY;
                int radius = 4;

                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dx = -radius; dx <= radius; ++dx) {
                        for (int dz = -radius; dz <= radius; ++dz) {
                            BlockPos candidatePos = playerPos.add(dx, dy, dz);
                            FluidState fluidStateCheck = mc.world.getFluidState(candidatePos);
                            if (!(fluidStateCheck.getFluid() == Fluids.WATER && fluidStateCheck.isStill())) {
                                continue;
                            }

                            double distSq = mc.player.getPos().squaredDistanceTo(
                                    candidatePos.getX() + 0.5D,
                                    candidatePos.getY() + 0.5D,
                                    candidatePos.getZ() + 0.5D
                            );
                            if (distSq >= closestDistSq) {
                                continue;
                            }
                            Vec3d eyesPosRot = mc.player.getCameraPosVec(1.0F);
                            Vec3d blockCenterRot = Vec3d.ofCenter(candidatePos);
                            double diffXRot = blockCenterRot.x - eyesPosRot.x;
                            double diffYRot = blockCenterRot.y - eyesPosRot.y;
                            double diffZRot = blockCenterRot.z - eyesPosRot.z;
                            double diffXZRot = Math.sqrt(diffXRot * diffXRot + diffZRot * diffZRot);
                            float yawRot = (float) Math.toDegrees(Math.atan2(diffZRot, diffXRot)) - 90.0F;
                            float pitchRot = (float) -Math.toDegrees(Math.atan2(diffYRot, diffXZRot));
                            Vector2f bucketRotation = new Vector2f(yawRot, pitchRot);
                            Vec3d eyePosRay = mc.player.getCameraPosVec(1.0F);
                            Vec3d directionRay = Vec3d.fromPolar(bucketRotation.getY(), bucketRotation.getX());
                            Vec3d endPosRay = eyePosRay.add(directionRay.multiply(4.5D));
                            BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePosRay, endPosRay, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, mc.player));

                            if (hit.getType() == HitResult.Type.MISS || !hit.getBlockPos().equals(candidatePos)) {
                                continue;
                            }

                            closestPos = candidatePos;
                            closestDistSq = distSq;
                        }
                    }
                }

                if (closestPos != null) {
                    Vec3d eyesPosFinal = mc.player.getCameraPosVec(1.0F);
                    Vec3d blockCenterFinal = Vec3d.ofCenter(closestPos);
                    double diffXFinal = blockCenterFinal.x - eyesPosFinal.x;
                    double diffYFinal = blockCenterFinal.y - eyesPosFinal.y;
                    double diffZFinal = blockCenterFinal.z - eyesPosFinal.z;
                    double diffXZFinal = Math.sqrt(diffXFinal * diffXFinal + diffZFinal * diffZFinal);
                    float yawFinal = (float) Math.toDegrees(Math.atan2(diffZFinal, diffXFinal)) - 90.0F;
                    float pitchFinal = (float) -Math.toDegrees(Math.atan2(diffYFinal, diffXZFinal));
                    Vector2f bucketRotation = new Vector2f(yawFinal, pitchFinal);

                    this.collectingWater = true;
                    this.above = closestPos;
                    this.targetRotation = bucketRotation;
                    this.rotation = bucketRotation != null;
                    if (bucketRotation != null) {
                        RotationManager.setRotations(bucketRotation);
                        RotationManager.active = true;
                    }
                    if (bucketSlot >= 0 && bucketSlot <= 8) {
                        this.slotToRestore = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = bucketSlot;
                    }
                    float originalYawUse = mc.player.getYaw();
                    float originalPitchUse = mc.player.getPitch();
                    if (bucketRotation != null) {
                        mc.player.setYaw(bucketRotation.getX());
                        mc.player.setPitch(bucketRotation.getY());
                    }
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    if (bucketRotation != null) {
                        mc.player.setYaw(originalYawUse);
                        mc.player.setPitch(originalPitchUse);
                    }

                    this.postActionCooldown = 8;
                    this.postPlaceCooldown = Math.max(this.postPlaceCooldown, 1);
                    return;
                }
            }
        }

        if (this.waterPlaced && !this.readyToPlace && mc.player.getVelocity().y < 0.0D) {
            Vec3d startPos = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());
            Vec3d endPos = startPos.add(0.0D, -2.5D, 0.0D);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(startPos, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            double distance = hit.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : startPos.y - hit.getPos().y;

            if (distance > 0.0D && distance <= 1.05D) {
                this.readyToPlace = true;
            }
        }

        if (this.waterPlaced || this.accumulatedFall < this.triggerDistance.getCurrentValue()) {
            return;
        }

        int slot = -1;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                slot = i;
                break;
            }
        }

        if (slot < 0) {
            return;
        }

        if (mc.player.getVelocity().y >= 0.0D) {
            return;
        }

        Vec3d startPosGround = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());
        Vec3d endPosGround = startPosGround.add(0.0D, -30.0D, 0.0D);
        BlockHitResult hitGround = mc.world.raycast(new RaycastContext(startPosGround, endPosGround, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        double distanceGround = hitGround.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : startPosGround.y - hitGround.getPos().y;

        if (distanceGround == Double.POSITIVE_INFINITY) {
            return;
        }

        double simulatedDrop = 0.0D;
        double simulatedVelocity = mc.player.getVelocity().y;
        int ticksToGround = 999;
        for (int i = 1; i <= 20; ++i) {
            simulatedDrop += simulatedVelocity;
            simulatedVelocity = (simulatedVelocity - 0.08D) * 0.98D;
            if (Math.abs(simulatedDrop) >= distanceGround) {
                ticksToGround = i;
                break;
            }
        }

        if (ticksToGround > (int) this.predictTicks.getCurrentValue()) {
            return;
        }

        if (this.solidCheck.getCurrentValue()) {
            BlockPos blockPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            BlockPos below1 = blockPos.down();
            BlockPos below2 = blockPos.down(2);
            BlockState blockState1 = mc.world.getBlockState(below1);
            boolean hasCollision1 = !blockState1.getCollisionShape(mc.world, below1).isEmpty();
            boolean noMenu1 = blockState1.createScreenHandlerFactory(mc.world, below1) == null;
            boolean solidBelow = (hasCollision1 && noMenu1);

            if (!solidBelow) {
                BlockState blockState2 = mc.world.getBlockState(below2);
                boolean hasCollision2 = !blockState2.getCollisionShape(mc.world, below2).isEmpty();
                boolean noMenu2 = blockState2.createScreenHandlerFactory(mc.world, below2) == null;
                solidBelow = (hasCollision2 && noMenu2);
            }

            if (!solidBelow) {
                return;
            }
        }

        Vector2f downRotation = new Vector2f(mc.player.getYaw(), 90.0F);
        Vec3d eyePosSolid = mc.player.getCameraPosVec(1.0F);
        Vec3d directionSolid = Vec3d.fromPolar(downRotation.getY(), downRotation.getX());
        Vec3d endPosSolid = eyePosSolid.add(directionSolid.multiply(5.0D));
        BlockHitResult hitSolid = mc.world.raycast(new RaycastContext(eyePosSolid, endPosSolid, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

        if (hitSolid.getType() == HitResult.Type.MISS) {
            return;
        }
        this.placingWater = true;
        this.targetRotation = downRotation;
        this.rotation = downRotation != null;
        if (downRotation != null) {
            RotationManager.setRotations(downRotation);
            RotationManager.active = true;
        }
        if (slot >= 0 && slot <= 8) {
            this.slotToRestore = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
        }

        float originalYawPlace = mc.player.getYaw();
        float originalPitchPlace = mc.player.getPitch();
        if (downRotation != null) {
            mc.player.setYaw(downRotation.getX());
            mc.player.setPitch(downRotation.getY());
        }
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (downRotation != null) {
            mc.player.setYaw(originalYawPlace);
            mc.player.setPitch(originalPitchPlace);
        }

        this.waterPlaced = true;
        this.recoveryActive = this.recovery.getCurrentValue();
        this.collectingWater = this.recoveryActive;
        this.recoveryDelay = 1;
        this.recoveryCountdown = this.recoveryActive ? 2 : 0;
        this.waterBucketSlot = null;
        BlockHitResult hitPlacement = mc.world.raycast(new RaycastContext(eyePosSolid, endPosSolid, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        this.placedWaterPos = hitPlacement.getType() == HitResult.Type.MISS ? null : hitPlacement.getBlockPos().offset(hitPlacement.getSide());
        this.above = this.placedWaterPos;
    }

    public boolean isInCooldown() {
        return this.postPlaceCooldown > 0;
    }

    public boolean isMLGActive() {
        return this.rotation || this.placingWater || this.collectingWater || this.waterPlaced || this.recoveryActive || this.slotToRestore != null;
    }

    public boolean isPlacingWater() {
        return this.placingWater || this.waterPlaced && !this.collectingWater;
    }

    public Vector2f calculateLookAt(BlockPos pos) {
        Vec3d eyesPos = mc.player.getCameraPosVec(1.0F);
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        double diffX = blockCenter.x - eyesPos.x;
        double diffY = blockCenter.y - eyesPos.y;
        double diffZ = blockCenter.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        return new Vector2f(yaw, pitch);
    }
}