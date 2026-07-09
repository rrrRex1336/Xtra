package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.awt.Color;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@ModuleInfo(
        name = "PearlInfo",
        description = "Renders prediction and can automatically counter thrown ender pearls.",
        category = Category.VISUAL
)
public class PearlInfo extends Module {

    public static Vector2f rotations;
    public static boolean isThrowing;

    private final BooleanValue renderHud = ValueBuilder.create(this, "Render Interface").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue hudSize = ValueBuilder.create(this, "Interface Size")
            .setVisibility(renderHud::getCurrentValue)
            .setDefaultFloatValue(0.4f).setFloatStep(0.01f).setMinFloatValue(0.1f).setMaxFloatValue(1.0f).build().getFloatValue();
    private final BooleanValue hideMyOwnPearls = ValueBuilder.create(this, "Hide My Own Pearls").setVisibility(renderHud::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue renderBackGround = ValueBuilder.create(this, "RenderBackGround").setVisibility(renderHud::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue renderBlur = ValueBuilder.create(this, "RenderBlur").setVisibility(renderHud::getCurrentValue).setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue renderHeaderBar = ValueBuilder.create(this, "RenderHeaderBar").setVisibility(renderHud::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue pearlCounter = ValueBuilder.create(this, "Pearl Counter").setDefaultBooleanValue(false).build().getBooleanValue();

    private final BooleanValue entityCheck = ValueBuilder.create(this, "Entity Check").setVisibility(pearlCounter::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();
    private final ModeValue triggerMode = ValueBuilder.create(this, "Trigger Key").setVisibility(pearlCounter::getCurrentValue).setModes("Mid Button", "Mouse4", "Mouse5", "None").setDefaultModeIndex(3).build().getModeValue();
    private final FloatValue rotationTicks = ValueBuilder.create(this, "Rotation Ticks").setVisibility(pearlCounter::getCurrentValue).setDefaultFloatValue(1.0f).setFloatStep(1.0f).setMinFloatValue(1.0f).setMaxFloatValue(20.0f).build().getFloatValue();
    private final FloatValue timeOut = ValueBuilder.create(this, "TimeOut")
            .setVisibility(pearlCounter::getCurrentValue)
            .setDefaultFloatValue(5.0f)
            .setFloatStep(0.25f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(30.0f)
            .build()
            .getFloatValue();
    private final FloatValue fov = ValueBuilder.create(this, "Fov").setVisibility(pearlCounter::getCurrentValue).setDefaultFloatValue(180.0F).setFloatStep(1.0f).setMinFloatValue(10.0f).setMaxFloatValue(360.0F).build().getFloatValue();
    private final FloatValue minRange = ValueBuilder.create(this, "Min Range").setVisibility(pearlCounter::getCurrentValue).setFloatStep(1.0F).setDefaultFloatValue(5.0f).setMinFloatValue(0.0f).setMaxFloatValue(50.0f).build().getFloatValue();
    private final FloatValue maxRange = ValueBuilder.create(this, "Max Range").setVisibility(pearlCounter::getCurrentValue).setFloatStep(1.0F).setDefaultFloatValue(100.0f).setMinFloatValue(10.0f).setMaxFloatValue(200.0f).build().getFloatValue();
    private final BooleanValue debug = ValueBuilder.create(this, "Debug Messages").setVisibility(pearlCounter::getCurrentValue).setDefaultBooleanValue(true).build().getBooleanValue();

    private final Map<UUID, PredictedPearlInfo> predictedPearls = new HashMap<>();
    private final Set<UUID> processedPearls = new HashSet<>();
    private Vec3d lastSafePosition = null;
    private long lockedPositionExpireTime = 0;
    private Vector2f targetRotations = null;
    private boolean isAiming = false;
    private int aimingTicks = 0;
    private boolean triggerAction = false;
    private static final List<Vec3d> SAMPLE_POINTS = Arrays.asList(new Vec3d(0.5, 0.5, 0.5), new Vec3d(0.2, 0.5, 0.5), new Vec3d(0.8, 0.5, 0.5), new Vec3d(0.5, 0.5, 0.2), new Vec3d(0.5, 0.5, 0.8));

    @Override public void onEnable() { super.onEnable(); reset(); }
    @Override public void onDisable() {
        super.onDisable();
        predictedPearls.values().forEach(PredictedPearlInfo::resetAnimation);
        reset();
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (!renderHud.getCurrentValue() || predictedPearls.isEmpty()) return;
        if (!renderBlur.getCurrentValue()) return;
        for(PredictedPearlInfo info : predictedPearls.values()) {
            if (info.currentWidth > 0.1F && info.currentHeight > 0.1F) {
                RenderUtils.drawRoundedRect(e.getStack(), info.animX, info.animY, info.currentWidth, info.currentHeight, 5.0F, Integer.MIN_VALUE);
            }
        }
    }

    @EventTarget
    public void onMouseClick(EventMouseClick event) {
        if (!pearlCounter.getCurrentValue() || mc.player == null || triggerMode.isCurrentMode("None")) return;
        int key = triggerMode.isCurrentMode("Mid Button") ? 2 : triggerMode.isCurrentMode("Mouse4") ? 3 : 4;
        if (event.getKey() == key && !event.isState()) { this.triggerAction = true; }
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (event.getType() != EventType.PRE || mc.player == null || mc.world == null) return;

        updatePearlLogicStates();
        if (pearlCounter.getCurrentValue()) {
            handleCounterLogic();
        } else {
            if (isAiming) reset();
            isThrowing = false;
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (renderHud.getCurrentValue()) {
            for (PredictedPearlInfo info : this.predictedPearls.values()) {
                info.calculateAnimation();
                if (info.currentWidth > 0.1F && info.currentHeight > 0.1F) {
                    renderLandingMark(e.getStack(), info);
                }
            }
        }
    }

    private void updatePearlLogicStates() {
        Map<UUID, PredictedPearlInfo> updatedPearls = new HashMap<>();
        List<UUID> toRemove = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl) {
                if (hideMyOwnPearls.getCurrentValue() && Objects.equals(pearl.getOwner(), mc.player)) continue;
                UUID uuid = pearl.getUuid();
                PredictedPearlInfo info = predictedPearls.computeIfAbsent(uuid, k -> new PredictedPearlInfo());
                info.update(pearl);
                updatedPearls.put(uuid, info);
            }
        }
        for (UUID uuid : predictedPearls.keySet()) {
            if (!updatedPearls.containsKey(uuid)) toRemove.add(uuid);
        }
        toRemove.forEach(predictedPearls::remove);
    }

    private void handleCounterLogic() {
        if (isAiming) {
            aimingTicks++;
            PearlInfo.isThrowing = true;
            PearlInfo.rotations = this.targetRotations;
            float yawDiff = RotationUtils.getAngleDifference(RotationManager.lastRotations.x, this.targetRotations.x);
            float pitchDiff = Math.abs(RotationManager.lastRotations.y - this.targetRotations.y);
            if (yawDiff < 1.0f && pitchDiff < 1.0f && aimingTicks >= rotationTicks.getCurrentValue()) {
                executeThrow();
                reset();
            }
            return;
        }
        PearlInfo.isThrowing = false;
        updateLastKnownSafePosition();

        if (lastSafePosition != null && System.currentTimeMillis() > lockedPositionExpireTime) {
            if (debug.getCurrentValue()) {
                ChatUtils.addChatMessage("§e[INFO]§7 Locked position expired.");
            }
            lastSafePosition = null;
        }

        boolean shouldTrigger = triggerMode.isCurrentMode("None") || this.triggerAction;
        if (shouldTrigger && lastSafePosition != null) {
            startAiming();
        }
        this.triggerAction = false;
    }

    private void updateLastKnownSafePosition() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl && !processedPearls.contains(pearl.getUuid())) {
                if (Objects.equals(pearl.getOwner(), mc.player)) continue;
                Object[] data = PearlPhysicsUtil.predictPearlLandingWithTicks(pearl, mc.world);
                Vec3d originalPos = (Vec3d) data[0];
                int ticksToLand = (int) data[1];
                if (originalPos == null) continue;
                processedPearls.add(pearl.getUuid());
                if (!isEnemyPearlThreatening(originalPos) || !isInFov(originalPos) || !(mc.player.getPos().distanceTo(originalPos) >= minRange.getCurrentValue() && mc.player.getPos().distanceTo(originalPos) <= maxRange.getCurrentValue())) {
                    if (debug.getCurrentValue() && !isEnemyPearlThreatening(originalPos)) ChatUtils.addChatMessage("§e[INFO]§7 Ignored non-threatening pearl.");
                    continue;
                }
                Vec3d safePos = findBestSafeLandingSpot(originalPos);
                if (safePos != null) {
                    if (debug.getCurrentValue()) ChatUtils.addChatMessage(String.format("§a[LOCKED]§7 New safe spot: %.1f, %.1f, %.1f", safePos.x, safePos.y, safePos.z));
                    this.lastSafePosition = safePos;
                    long landingDurationMs = (long) ticksToLand * 50;
                    long timeoutMs = (long) (timeOut.getCurrentValue() * 1000);
                    this.lockedPositionExpireTime = System.currentTimeMillis() + landingDurationMs + timeoutMs;
                }
            }
        }
    }

    private void startAiming() {
        this.targetRotations = PearlPhysicsUtil.calculateOptimalRotations(mc.player.getEyePos(), lastSafePosition);
        if (this.targetRotations != null) {
            this.isAiming = true;
            this.aimingTicks = 0;
            if (debug.getCurrentValue()) ChatUtils.addChatMessage(String.format("§e[AIMING]§7 Yaw: %.1f, Pitch: %.1f", this.targetRotations.x, this.targetRotations.y));
            if (triggerMode.isCurrentMode("None")) { this.lastSafePosition = null; }
        } else {
            if (debug.getCurrentValue()) ChatUtils.addChatMessage("§c[FAIL]§7 Cannot aim, calculation failed.");
        }
    }

    private void executeThrow() {
        int pearlSlot = findPearlSlot();
        if (pearlSlot == -1) {
            if (debug.getCurrentValue()) ChatUtils.addChatMessage("§c[FAIL]§7 No pearl found in hotbar.");
            return;
        }
        if (debug.getCurrentValue()) ChatUtils.addChatMessage("§b[THROW]§7 Rotation synced. Firing pearl!");
        int originalInvSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = pearlSlot;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = originalInvSlot;
    }

    private void reset() {
        this.lastSafePosition = null; this.triggerAction = false; this.isAiming = false;
        this.aimingTicks = 0; this.targetRotations = null;
        this.lockedPositionExpireTime = 0;
        PearlInfo.rotations = null; PearlInfo.isThrowing = false;
        if (processedPearls.size() > 50) processedPearls.clear();
    }

    private Vec3d findBestSafeLandingSpot(Vec3d target) {
        BlockPos originalPos = BlockPos.ofFloored(target);
        Vec3d viablePoint = findViablePointInBlock(originalPos);
        if (viablePoint != null) return viablePoint;
        int radius=8, x=0, z=0, dx=0, dz=-1;
        for (int i=0; i<Math.pow(radius*2+1,2); i++) {
            if ((-radius/2<=x && x<=radius/2) && (-radius/2<=z && z<=radius/2))
                for (int yOff = 3; yOff >= -3; yOff--) {
                    BlockPos finalPos = originalPos.add(x, yOff, z);
                    Vec3d finalViablePoint = findViablePointInBlock(finalPos);
                    if (finalViablePoint != null) return finalViablePoint;
                }
            if(x==z||(x<0&&x==-z)||(x>0&&x==1-z)){int tmp=dx;dx=-dz;dz=tmp;} x+=dx;z+=dz;
        }
        if(debug.getCurrentValue()) ChatUtils.addChatMessage(String.format("§6[SEARCH FAIL]§7 No clear spot found near (%.0f, %.0f, %.0f).", target.x, target.y, target.z));
        return null;
    }

    private Vec3d findViablePointInBlock(BlockPos pos) {
        if (!isSafeFloor(pos.down()) || !hasHeadroom(pos)) return null;
        for (Vec3d sampleOffset : SAMPLE_POINTS) {
            Vec3d targetPoint = new Vec3d(pos.getX() + sampleOffset.x, pos.getY() + sampleOffset.y, pos.getZ() + sampleOffset.z);
            if (hasLineOfSight(targetPoint) && (!entityCheck.getCurrentValue() || PearlPhysicsUtil.isTrajectoryClear(mc.world, mc.player, targetPoint))) {
                return targetPoint;
            }
        }
        return null;
    }

    private boolean isEnemyPearlThreatening(Vec3d pos) {
        if (pos.getY() < mc.world.getBottomY()) return false;
        return mc.world.getBlockState(BlockPos.ofFloored(pos).down()).isFullCube(mc.world, BlockPos.ofFloored(pos).down());
    }

    private boolean isInFov(Vec3d point) {
        float angleDiff = RotationUtils.getAngleDifference(mc.player.getYaw(), (float)(Math.toDegrees(Math.atan2(point.z - mc.player.getEyePos().z, point.x - mc.player.getEyePos().x)) - 90.0));
        return Math.abs(angleDiff) <= fov.getCurrentValue() / 2.0;
    }

    private boolean isSafeFloor(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isFullCube(mc.world, pos) && !state.isOf(Blocks.LAVA) && !state.isOf(Blocks.MAGMA_BLOCK) && !state.isOf(Blocks.CACTUS);
    }

    private boolean hasHeadroom(BlockPos pos) {
        return !mc.world.getBlockState(pos).isOpaqueFullCube(mc.world, pos) && !mc.world.getBlockState(pos.up()).isOpaqueFullCube(mc.world, pos.up());
    }

    private boolean hasLineOfSight(Vec3d target) {
        return mc.world.raycast(new RaycastContext(mc.player.getEyePos(), target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    private int findPearlSlot() {
        for (int i=0;i<9;i++) if(mc.player.getInventory().getStack(i).getItem()==Items.ENDER_PEARL) return i; return -1;
    }

    private void renderLandingMark(MatrixStack matrix, PredictedPearlInfo info) {
        if (info.projectedPos == null) return;

        matrix.push();

        if (renderBackGround.getCurrentValue()) {
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(matrix, info.animX, info.animY, info.currentWidth, info.currentHeight, 5.0F, -1);
            StencilUtils.erase(true);
        }

        if (renderHeaderBar.getCurrentValue()) {
            RenderUtils.fill(matrix, info.finalX, info.finalY, info.finalX + info.finalWidth, info.finalY + info.headerHeight, WaterMark.headerColor);
        }

        if (renderBackGround.getCurrentValue()) {
            RenderUtils.fill(matrix, info.finalX, info.finalY + info.headerHeight, info.finalX + info.finalWidth, info.finalY + info.finalHeight, WaterMark.bodyColor);
        }

        float tY = info.finalY + (renderHeaderBar.getCurrentValue() ? info.headerHeight : 0.0f) + 2.0f;
        float textRowHeight = (float)Fonts.harmony.getHeight(true, hudSize.getCurrentValue());

        for (String l : info.lines) {
            float textXOffset = (info.finalWidth - Fonts.harmony.getWidth(l, hudSize.getCurrentValue())) / 2;
            Fonts.harmony.drawString(matrix, l, info.finalX + textXOffset, tY, Color.WHITE, true, hudSize.getCurrentValue());
            tY += textRowHeight * 0.875F;
        }

        if (renderBackGround.getCurrentValue()) {
            StencilUtils.dispose();
        }
        matrix.pop();
    }

    private class PredictedPearlInfo {
        Vec3d pos; Vector2f projectedPos; int ticksToLand; String ownerName;
        List<String> lines = new ArrayList<>();
        float totalTextHeight, headerHeight = 3.0f;
        float finalWidth, finalHeight, currentWidth, currentHeight, finalX, finalY, animX, animY;
        long lastUpdateTime;

        public PredictedPearlInfo() { this.lastUpdateTime = System.currentTimeMillis(); }

        public void update(EnderPearlEntity pearl) {
            Object[] data = PearlPhysicsUtil.predictPearlLandingWithTicks(pearl, mc.world);
            this.pos = (Vec3d) data[0];
            this.ticksToLand = (int) data[1];
            if (this.pos != null) {
                Entity owner = pearl.getOwner();
                String name = (owner != null) ? owner.getDisplayName().getString() : "Unknown";
                this.ownerName = NameProtect.getName(name);
            }
        }

        public void calculateAnimation() {
            if (this.pos == null) {
                this.finalWidth = 0; this.finalHeight = 0;
                this.currentWidth *= 0.85f; this.currentHeight *= 0.85f;
                return;
            }

            this.projectedPos = ProjectionUtils.project(pos.x, pos.y, pos.z, mc.getTickDelta());
            if (projectedPos == null) { this.finalWidth = 0; this.finalHeight = 0; } else {
                lines.clear();
                lines.add("Thrown by: "+this.ownerName);
                lines.add(String.format("Lands in: %.1f s",this.ticksToLand/20.0));
                lines.add(String.format("Distance: %.1f m", mc.player.getPos().distanceTo(pos)));

                float textHeight = (float)Fonts.harmony.getHeight(true, hudSize.getCurrentValue());
                this.totalTextHeight = (textHeight * 0.875f) * (lines.size() - 1) + textHeight;

                float w = 0.0f;
                for(String l : lines) w = Math.max(w, (float)Fonts.harmony.getWidth(l, hudSize.getCurrentValue()));

                this.finalWidth = w + 8.0f;
                this.finalHeight = this.totalTextHeight + headerHeight + 4.0f;
            }

            long currentTime = System.currentTimeMillis();
            if(this.lastUpdateTime==0) this.lastUpdateTime=currentTime;
            float deltaTime = (currentTime - this.lastUpdateTime) / 1000.0F;
            float animationSpeed = 10.0F;
            this.currentWidth += (this.finalWidth-this.currentWidth)*animationSpeed*deltaTime;
            this.currentHeight += (this.finalHeight-this.currentHeight)*animationSpeed*deltaTime;
            this.lastUpdateTime = currentTime;
            if(Math.abs(this.finalWidth-this.currentWidth)<0.01f) this.currentWidth=this.finalWidth;
            if(Math.abs(this.finalHeight-this.currentHeight)<0.01f) this.currentHeight=this.finalHeight;

            if (this.projectedPos != null) {
                this.finalX = this.projectedPos.x - this.finalWidth/2.0f;
                this.finalY = this.projectedPos.y - this.finalHeight/2.0f;
                this.animX = this.finalX+(this.finalWidth-this.currentWidth)/2.0f;
                this.animY = this.finalY+(this.finalHeight-this.currentHeight)/2.0f;
            }
        }

        public void resetAnimation() {
            this.currentWidth=0; this.currentHeight=0; this.finalWidth=0; this.finalHeight=0; this.lastUpdateTime=0;
        }
    }
}
