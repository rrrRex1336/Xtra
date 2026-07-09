package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.modules.impl.visual.TargetInfo;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import org.joml.Matrix4f;
import org.mixin.accessors.MinecraftAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "Aura",
        description = "Automatically attacks entities.(Another Kill Aura)",
        category = Category.COMBAT
)
public class Aura extends Module {

    public Vector2f targetRotation = new Vector2f();
    public boolean working = false;
    private final Random random = new Random();
    private float counter = 0.0F;
    private float currentCPS = 10.0F;
    private int currentTargetIndex = 0;
    private int attackCountOnCurrentTarget = 0;
    public static Entity target;
    public static List<Entity> targets = new ArrayList<>();
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
    private float espRotationAngle = 0;
    private float currentRotationSpeed = 0;
    private float targetRotationSpeed = 0;
    private int rotationDirection = 1;
    private int rotationSpeedTickCounter = 0;
    private int rotationDirectionTickCounter = 0;
    private final Random espRandom = new Random();

    public ModeValue targetTrack = ValueBuilder.create(this, "Target Mode")
            .setModes("Single", "Switch", "Multi")
            .build()
            .getModeValue();

    public ModeValue attackTiming = ValueBuilder.create(this, "Attack Timing")
            .setModes("Pre", "Post", "Both")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public ModeValue attackMethod = ValueBuilder.create(this, "Attack Method")
            .setModes("KeyMapping", "Direct", "Packet")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public ModeValue espMode = ValueBuilder.create(this, "ESP Mode")
            .setModes("Box", "Rectangle")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    AddonsValue atarget = ValueBuilder.create(this, "Target")
            .setAddonsModes("Player", "Invisible", "Animals", "Mobs")
            .setDefaultSelectedAddons(true, true, false, false)
            .build()
            .getAddonsValue();

    BooleanValue attackCooldowns = ValueBuilder.create(this, "Attack Cooldowns(1.9+)")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public ModeValue attackMode = ValueBuilder.create(this, "Attack Mode")
            .setModes("Delay", "HurtTime", "Tick")
            .setDefaultModeIndex(0)
            .setVisibility(() -> !attackCooldowns.getCurrentValue())
            .build()
            .getModeValue();

    BooleanValue smooth = ValueBuilder.create(this, "Smooth")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    ModeValue priority = ValueBuilder.create(this, "Priority")
            .setModes("Health", "FoV", "Range", "None")
            .build()
            .getModeValue();

    FloatValue minCPS = ValueBuilder.create(this, "Min CPS")
            .setVisibility(() -> !attackCooldowns.getCurrentValue() && attackMode.isCurrentMode("Delay"))
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

    FloatValue maxCPS = ValueBuilder.create(this, "Max CPS")
            .setVisibility(() -> !attackCooldowns.getCurrentValue() && attackMode.isCurrentMode("Delay"))
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

    public FloatValue attackRange = ValueBuilder.create(this, "Attack Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();

    public FloatValue rotationRange = ValueBuilder.create(this, "Rotation Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();

    FloatValue rotateMinSpeed = ValueBuilder.create(this, "Min Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();

    FloatValue rotateMaxSpeed = ValueBuilder.create(this, "Max Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();

    FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();

    {
        minCPS.linkAsMin(maxCPS);
        maxCPS.linkAsMax(minCPS);
    }

    {
        rotateMinSpeed.linkAsMin(rotateMaxSpeed);
        rotateMaxSpeed.linkAsMax(rotateMinSpeed);
    }

    @EventTarget
    public void onMotion(EventRunTicks e) {
        if (mc.player == null || mc.world == null) {
            this.working = false;
            return;
        }
        setSuffix(targetTrack.getCurrentMode());

        if (e.getType() == EventType.PRE && mc.player != null) {
            float min = this.minCPS.getCurrentValue();
            float max = this.maxCPS.getCurrentValue();
            this.currentCPS = min + (random.nextFloat() * (max - min));

            Entity targetEntity = this.getTarget();
            float targetYaw;
            float targetPitch;

            if (targetEntity != null) {
                Vector2f rotations = RotationUtils.getRotations(targetEntity);
                targetYaw = rotations.getX();
                targetPitch = rotations.getY();
                this.working = true;
            } else {
                targetYaw = mc.player.getYaw();
                targetPitch = mc.player.getPitch();
                if (this.targetRotation.getX() % 360.0F == targetYaw % 360.0F) {
                    this.working = false;
                }
            }

            if (this.working) {
                if (smooth.getCurrentValue()) {
                    float minSpeed = this.rotateMinSpeed.getCurrentValue();
                    float maxSpeed = this.rotateMaxSpeed.getCurrentValue();
                    float currentSpeed = minSpeed + (random.nextFloat() * (maxSpeed - minSpeed));
                    float currentYaw = this.targetRotation.getX();
                    float yawDiff = getAngleDifference(targetYaw, currentYaw);
                    float yawStep = Math.min(Math.abs(yawDiff), currentSpeed) * Math.signum(yawDiff);
                    float newYaw = normalizeAngle(currentYaw + yawStep);
                    this.targetRotation.setX(Math.abs(yawDiff) <= 0.1F ? targetYaw : newYaw);
                    float currentPitch = this.targetRotation.getY();
                    float pitchDiff = targetPitch - currentPitch;
                    float pitchStep = Math.min(Math.abs(pitchDiff), currentSpeed) * Math.signum(pitchDiff);
                    float newPitch = clampPitch(currentPitch + pitchStep);
                    this.targetRotation.setY(Math.abs(pitchDiff) <= 0.1F ? targetPitch : newPitch);
                } else {
                    this.targetRotation.setX(targetYaw);
                    this.targetRotation.setY(targetPitch);
                }
            } else if (smooth.getCurrentValue()) {
                float minSpeed = this.rotateMinSpeed.getCurrentValue();
                float maxSpeed = this.rotateMaxSpeed.getCurrentValue();
                float currentSpeed = minSpeed + (random.nextFloat() * (maxSpeed - minSpeed));

                float originalYaw = mc.player.getYaw();
                float currentYaw = this.targetRotation.getX();
                float yawDiff = getAngleDifference(originalYaw, currentYaw);

                if (Math.abs(yawDiff) <= currentSpeed) {
                    this.targetRotation.setX(originalYaw);
                    this.targetRotation.setY(mc.player.getPitch());
                } else {
                    float yawStep = currentSpeed * Math.signum(yawDiff);
                    this.targetRotation.setX(normalizeAngle(currentYaw + yawStep));
                    this.targetRotation.setY(mc.player.getPitch());
                }
            } else {
                this.targetRotation.setX(mc.player.getYaw());
                this.targetRotation.setY(mc.player.getPitch());
            }
            if (attackTiming.isCurrentMode("Pre") || attackTiming.isCurrentMode("Both")) {
                doAttack();
            }
        }
        if (e.getType() == EventType.POST && (attackTiming.isCurrentMode("Post") || attackTiming.isCurrentMode("Both"))) {
            doAttack();
        }
    }

    private void doAttack() {
        MinecraftAccessor accessor = (MinecraftAccessor) mc;
        Entity targetEntity = this.getTarget();
        boolean hasValidTarget = false;

        if (targetEntity != null) {
            hasValidTarget = this.targetTrack.isCurrentMode("Multi")
                    ? hasAttackableTarget()
                    : isWithinAttackRange(targetEntity);
        }

        if (hasValidTarget && targetEntity != null) {
            if (attackCooldowns.getCurrentValue()) {
                if (mc.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                    performAttackByMode(targetEntity);
                    accessor.setMissTime(0);
                }
            } else {
                String mode = attackMode.getCurrentMode();

                if (mode.equals("Delay")) {
                    this.counter += this.currentCPS / 20.0F;
                    if (this.counter >= 1.0F / this.currentCPS) {
                        performAttackByMode(targetEntity);
                        accessor.setMissTime(0);
                        this.counter -= 1.0F;
                        float min = this.minCPS.getCurrentValue();
                        float max = this.maxCPS.getCurrentValue();
                        this.currentCPS = min + (random.nextFloat() * (max - min));
                    }
                } else if (mode.equals("HurtTime")) {
                    if (targetEntity instanceof LivingEntity living) {
                        boolean canAttack = mc.player.fallDistance < 0.2F;

                        if (canAttack) {
                            if (living.hurtTime == 0 || living.hurtTime > 6) {
                                performAttackByMode(targetEntity);
                                accessor.setMissTime(0);
                            }
                        }
                    }
                } else if (mode.equals("Tick")) {
                    performAttackByMode(targetEntity);
                    accessor.setMissTime(0);
                }
            }
        } else {
            this.counter = 0.0F;
        }
    }

    private boolean hasAttackableTarget() {
        for (Entity entity : getAllValidTargets()) {
            if (isWithinAttackRange(entity)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWithinAttackRange(Entity entity) {
        Vec3d closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePos(), entity.getBoundingBox());
        return closestPoint.distanceTo(mc.player.getEyePos()) <= this.attackRange.getCurrentValue();
    }

    private float getAngleDifference(float target, float current) {
        float difference = target - current;
        while (difference > 180) difference -= 360;
        while (difference < -180) difference += 360;
        return difference;
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    private float clampPitch(float pitch) {
        return Math.max(-90, Math.min(90, pitch));
    }

    private void performAttackByMode(Entity primaryTarget) {
        String mode = this.targetTrack.getCurrentMode();
        String method = attackMethod.getCurrentMode();

        if (mode.equals("Multi")) {
            List<Entity> validTargets = getAllValidTargets();

            for (Entity entity : validTargets) {
                if (isWithinAttackRange(entity)) {
                    performAttack(entity, method);
                }
            }
        } else {
            if (primaryTarget != null) {
                if (isWithinAttackRange(primaryTarget)) {
                    performAttack(primaryTarget, method);
                    this.attackCountOnCurrentTarget++;
                }
            }
        }
    }

    private void performAttack(Entity target, String method) {
        if (mc.player == null || target == null) {
            return;
        }

        TargetInfo.trackTarget(target);
        if (Criticals.shouldHoldAuraAttack(target) || Criticals.tryPerformAuraCriticalAttack(target)) {
            return;
        }

        boolean attacked = false;
        if (method.equals("Direct")) {
            if (mc.interactionManager != null) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                attacked = true;
            }
        } else if (method.equals("Packet")) {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking());
                mc.getNetworkHandler().sendPacket(packet);
                mc.player.attack(target);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.resetLastAttackedTicks();
                attacked = true;
            }
        } else {
            KeyBinding.onKeyPressed(mc.options.attackKey.getDefaultKey());
            attacked = true;
        }

        if (attacked) {
            Criticals.afterAuraAttack(target);
        }
    }

    public boolean isValidTarget(Entity entity) {
        if (mc.player == null || entity == null) {
            return false;
        }
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof LivingEntity living) {
            if (living instanceof BlinkingPlayer) {
                return false;
            } else {
                AntiBots module = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
                if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
                    if (Teams.isSameTeam(living)) {
                        return false;
                    } else if (FriendManager.isFriend(living)) {
                        return false;
                    } else if (living.isDead() || living.getHealth() <= 0.0F) {
                        return false;
                    } else if (entity instanceof ArmorStandEntity) {
                        return false;
                    } else if (entity instanceof VillagerEntity) {
                        return false;
                    } else if (entity.isInvisible() && !atarget.isSelected("Invisible")) {
                        return false;
                    } else if (entity instanceof PlayerEntity && !atarget.isSelected("Player")) {
                        return false;
                    } else if ((entity instanceof MobEntity || entity instanceof SlimeEntity || entity instanceof BatEntity || entity instanceof GolemEntity)
                            && !atarget.isSelected("Mobs")) {
                        return false;
                    } else if ((entity instanceof AnimalEntity || entity instanceof SquidEntity) && !atarget.isSelected("Animals")) {
                        return false;
                    } else {
                        return !(entity instanceof PlayerEntity) || !entity.isSpectator();
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean isValidAttack(Entity entity) {
        if (!this.isValidTarget(entity)) {
            return false;
        }

        Vec3d closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePos(), entity.getBoundingBox());
        double distance = closestPoint.distanceTo(mc.player.getEyePos());
        if (distance > (double) this.rotationRange.getCurrentValue()) {
            return false;
        }
        if (this.fov.getCurrentValue() < 360.0F) {
            boolean inFoV = RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
            if (!inFoV) {
                return false;
            }
        }

        return true;
    }

    private Entity getTarget() {
        List<Entity> possibleTargets = getAllValidTargets();

        if (possibleTargets.isEmpty()) {
            this.currentTargetIndex = 0;
            this.attackCountOnCurrentTarget = 0;
            targets.clear();
            target = null;
            return null;
        }

        targets = new ArrayList<>(possibleTargets);
        String mode = this.targetTrack.getCurrentMode();

        if (mode.equals("Single")) {
            target = possibleTargets.get(0);
            return target;
        }

        if (mode.equals("Switch")) {
            if (this.currentTargetIndex >= possibleTargets.size()) {
                this.currentTargetIndex = 0;
            }
            Entity currentTarget = possibleTargets.get(this.currentTargetIndex);
            boolean needSwitch = false;
            if (this.attackCountOnCurrentTarget >= (int) this.switchAttackTimes.getCurrentValue()) {
                needSwitch = true;
            }
            if (!needSwitch && currentTarget != null) {
                Vec3d closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePos(), currentTarget.getBoundingBox());
                if (closestPoint.distanceTo(mc.player.getEyePos()) > this.attackRange.getCurrentValue() + 0.5) {
                    needSwitch = true;
                }
            }
            if (needSwitch) {
                this.currentTargetIndex = (this.currentTargetIndex + 1) % possibleTargets.size();
                this.attackCountOnCurrentTarget = 0;
                currentTarget = possibleTargets.get(this.currentTargetIndex);
            }

            target = currentTarget;
            return target;
        }

        if (mode.equals("Multi")) {
            target = possibleTargets.isEmpty() ? null : possibleTargets.get(0);
            return target;
        }

        target = possibleTargets.isEmpty() ? null : possibleTargets.get(0);
        return target;
    }

    private List<Entity> getAllValidTargets() {
        if (mc.world == null || mc.player == null) {
            return new ArrayList<>();
        }

        Stream<Entity> stream = StreamSupport.stream(mc.world.getEntities().spliterator(), true)
                .filter(entity -> entity instanceof Entity)
                .filter(this::isValidAttack);

        List<Entity> possibleTargets = stream.collect(Collectors.toList());
        if (this.priority.isCurrentMode("Range")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> o.distanceTo(mc.player)));
        } else if (this.priority.isCurrentMode("FoV")) {
            possibleTargets.sort(Comparator.comparingDouble(o ->
                    RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x)));
        } else if (this.priority.isCurrentMode("Health")) {
            possibleTargets.sort(Comparator.comparingDouble(o ->
                    o instanceof LivingEntity living ? (double) living.getHealth() : Double.MAX_VALUE));
        }

        return possibleTargets;
    }

    @EventTarget
    public void onRender(EventRender e) {
        if (!this.targetEsp.getCurrentValue()) {
            return;
        }

        String currentEspMode = espMode.getCurrentMode();

        if (currentEspMode.equals("Box")) {
            MatrixStack stack = e.getPMatrixStack();
            float partialTicks = e.getRenderPartialTicks();
            stack.push();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionProgram);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity living) {
                    float[] color = target == living ? targetColorRed : targetColorGreen;
                    stack.push();
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    double motionX = entity.getX() - entity.prevX;
                    double motionY = entity.getY() - entity.prevY;
                    double motionZ = entity.getZ() - entity.prevZ;
                    Box boundingBox = entity.getBoundingBox()
                            .offset(-motionX, -motionY, -motionZ)
                            .offset(partialTicks * motionX, partialTicks * motionY, partialTicks * motionZ);
                    RenderUtils.drawSolidBox(boundingBox, stack);
                    stack.pop();
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.pop();
        } else if (currentEspMode.equals("Rectangle")) {
            rotationSpeedTickCounter++;
            if (rotationSpeedTickCounter >= 10) {
                rotationSpeedTickCounter = 0;
                targetRotationSpeed = 3.0f + espRandom.nextFloat() * 3.0f;
            }

            rotationDirectionTickCounter++;
            if (rotationDirectionTickCounter >= 40 + espRandom.nextInt(41)) {
                rotationDirectionTickCounter = 0;
                targetRotationSpeed = 0.0f;
            }

            if (Math.abs(currentRotationSpeed) < 0.1f && targetRotationSpeed == 0.0f) {
                rotationDirection *= -1;
                targetRotationSpeed = 3.0f + espRandom.nextFloat() * 3.0f;
            }
            currentRotationSpeed = MathHelper.lerp(0.1f, currentRotationSpeed, targetRotationSpeed * rotationDirection);
            espRotationAngle += currentRotationSpeed;
            int color = Naven.getInstance().getThemeManager().getColor(espRotationAngle);

            MatrixStack stack = e.getPMatrixStack();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            float partialTicks = e.getRenderPartialTicks();

            stack.push();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);

            Identifier texture = new Identifier("naven-modern", "client/textures/targets/rectangle.png");
            RenderSystem.setShaderTexture(0, texture);
            if (color == -1) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f);
            } else {
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                RenderSystem.setShaderColor(r, g, b, 0.8f);
            }

            Tessellator tesselator = Tessellator.getInstance();
            BufferBuilder buffer = tesselator.getBuffer();

            for (Entity entity : targets) {
                if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
                    continue;
                }

                double x = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
                double y = MathHelper.lerp(partialTicks, entity.prevY, entity.getY()) + entity.getHeight() * 0.5;
                double z = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

                float distance = (float) cameraPos.distanceTo(new Vec3d(x, y, z));
                float size = 1.2f;

                stack.push();
                stack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                stack.multiply(mc.gameRenderer.getCamera().getRotation());
                stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(espRotationAngle));
                stack.scale(size, size, size);

                Matrix4f matrix = stack.peek().getPositionMatrix();

                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                buffer.vertex(matrix, -0.5f, -0.5f, 0.0f).texture(0.0f, 1.0f).next();
                buffer.vertex(matrix, 0.5f, -0.5f, 0.0f).texture(1.0f, 1.0f).next();
                buffer.vertex(matrix, 0.5f, 0.5f, 0.0f).texture(1.0f, 0.0f).next();
                buffer.vertex(matrix, -0.5f, 0.5f, 0.0f).texture(0.0f, 0.0f).next();
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                stack.pop();
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            stack.pop();
        }
    }
}
