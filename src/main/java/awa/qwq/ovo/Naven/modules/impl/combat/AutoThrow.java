package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.modules.impl.movement.Stuck;
import awa.qwq.ovo.Naven.modules.impl.player.Blink;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.Comparator;
import java.util.Optional;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
        name = "AutoThrow",
        description = "Automatically throw snowballs and eggs.",
        category = Category.COMBAT
)
public class AutoThrow extends Module {
    private final FloatValue minDistance = ValueBuilder.create(this, "Min Distance")
            .setDefaultFloatValue(5)
            .setFloatStep(1)
            .setMinFloatValue(3)
            .setMaxFloatValue(30)
            .build()
            .getFloatValue();

    private final FloatValue maxDistance = ValueBuilder.create(this, "Max Distance")
            .setDefaultFloatValue(10)
            .setFloatStep(1)
            .setMinFloatValue(3)
            .setMaxFloatValue(30)
            .build()
            .getFloatValue();

    private final FloatValue delay = ValueBuilder.create(this, "Delay")
            .setDefaultFloatValue(500)
            .setFloatStep(50)
            .setMinFloatValue(50)
            .setMaxFloatValue(2000)
            .build()
            .getFloatValue();

    private final AddonsValue targetMode = ValueBuilder.create(this, "Target")
            .setAddonsModes("Player", "Invisible", "Animals", "Mobs")
            .setDefaultSelectedAddons(true, true, false, false)
            .build()
            .getAddonsValue();

    private final TimeHelper timer = new TimeHelper();
    @Getter
    private Rotation rotation;
    public int rotationSet;
    public Vector2f targetRotations = null;

    private ThrowPlan pendingPlan;
    private int restoreSlot = -1;

    {
        minDistance.linkAsMin(maxDistance);
        maxDistance.linkAsMax(minDistance);
    }

    @Override
    public void onEnable() {
        this.clearState();
        this.timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.restoreSlot();
        this.clearState();
        super.onDisable();
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() == EventType.POST) {
            this.restoreSlot();
        }
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.clearState();
            return;
        }
        if (this.shouldPause()) {
            this.clearState();
            return;
        }

        this.setSuffix(this.minDistance.getCurrentValue() + " - " + this.maxDistance.getCurrentValue());
        if (this.rotationSet > 0 && this.pendingPlan != null) {
            RotationManager.setRotations(this.targetRotations);
            if (--this.rotationSet <= 0) {
                this.throwPending();
            }
            return;
        }

        if (!this.timer.delay(this.delay.getCurrentValue())) {
            return;
        }

        Optional<ThrowPlan> plan = this.findThrowPlan();
        Optional<LivingEntity> target = this.findTarget();
        if (plan.isEmpty() || target.isEmpty() || !this.canThrow(plan.get())) {
            return;
        }

        this.rotation = this.getRotationToEntity(target.get());
        this.targetRotations = new Vector2f(this.rotation.getYaw(), this.rotation.getPitch());
        this.pendingPlan = plan.get();
        this.rotationSet = 2;
        RotationManager.setRotations(this.targetRotations);
        this.timer.reset();
    }

    private void throwPending() {
        if (this.pendingPlan == null || mc.player == null || mc.interactionManager == null) {
            this.clearState();
            return;
        }

        ThrowPlan plan = this.pendingPlan;
        if (plan.hand == Hand.MAIN_HAND && plan.hotbarSlot != mc.player.getInventory().selectedSlot) {
            this.restoreSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = plan.hotbarSlot;
        }

        mc.interactionManager.interactItem(mc.player, plan.hand);
        mc.player.swingHand(plan.hand);
        this.pendingPlan = null;
        this.rotationSet = 0;
    }

    private Optional<ThrowPlan> findThrowPlan() {
        if (this.isThrowable(mc.player.getOffHandStack())) {
            return Optional.of(new ThrowPlan(Hand.OFF_HAND, -1));
        }

        int selected = mc.player.getInventory().selectedSlot;
        if (this.isThrowable(mc.player.getInventory().main.get(selected))) {
            return Optional.of(new ThrowPlan(Hand.MAIN_HAND, selected));
        }

        for (int slot = 0; slot < 9; slot++) {
            if (this.isThrowable(mc.player.getInventory().main.get(slot))) {
                return Optional.of(new ThrowPlan(Hand.MAIN_HAND, slot));
            }
        }
        return Optional.empty();
    }

    private Optional<LivingEntity> findTarget() {
        double max = this.maxDistance.getCurrentValue();
        double min = this.minDistance.getCurrentValue();
        return mc.world.getNonSpectatingEntities(LivingEntity.class, mc.player.getBoundingBox().expand(max))
                .stream()
                .filter(entity -> entity != mc.player)
                .filter(LivingEntity::isAlive)
                .filter(entity -> !entity.isSpectator())
                .filter(entity -> !AntiBots.isBot(entity))
                .filter(entity -> !AntiBots.isBedWarsBot(entity))
                .filter(entity -> !Teams.isSameTeam(entity))
                .filter(entity -> !FriendManager.isFriend(entity))
                .filter(entity -> !entity.isInvisibleTo(mc.player) || this.targetMode.isSelected("Invisible"))
                .filter(entity -> this.isSelectedTargetType(entity))
                .filter(mc.player::canSee)
                .filter(entity -> {
                    double distance = this.getHorizontalDistance(entity);
                    return distance >= min && distance <= max;
                })
                .min(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)));
    }

    private boolean isSelectedTargetType(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            return this.targetMode.isSelected("Player");
        }
        if (entity instanceof AnimalEntity) {
            return this.targetMode.isSelected("Animals");
        }
        if (entity instanceof HostileEntity || entity instanceof MobEntity) {
            return this.targetMode.isSelected("Mobs");
        }
        return false;
    }

    private boolean canThrow(ThrowPlan plan) {
        if (mc.player.isUsingItem()) {
            return false;
        }
        ItemStack activeStack = plan.hand == Hand.MAIN_HAND
                ? mc.player.getInventory().main.get(plan.hotbarSlot)
                : mc.player.getOffHandStack();
        Item item = activeStack.getItem();
        return !(item instanceof EnderPearlItem)
                && !(item instanceof BowItem)
                && !(item instanceof PotionItem)
                && !(item instanceof SplashPotionItem)
                && !(item instanceof LingeringPotionItem)
                && !item.isFood();
    }

    private Rotation getRotationToEntity(LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        double targetX = target.getX();
        double targetY = target.getY() + target.getHeight() * 0.55D;
        double targetZ = target.getZ();

        double time = 0.0D;
        for (int i = 0; i < 3; i++) {
            double predictedX = targetX + velocity.x * time;
            double predictedZ = targetZ + velocity.z * time;
            double dx = predictedX - mc.player.getX();
            double dz = predictedZ - mc.player.getZ();
            time = Math.sqrt(dx * dx + dz * dz) / 0.6D;
        }

        double predictedX = targetX + velocity.x * time;
        double predictedY = targetY + velocity.y * time;
        double predictedZ = targetZ + velocity.z * time;
        double x = predictedX - mc.player.getX();
        double z = predictedZ - mc.player.getZ();
        double y = predictedY - (mc.player.getY() + mc.player.getStandingEyeHeight());
        double horizontal = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = -this.getLowArcPitch((float) horizontal, (float) y, 0.6F, 0.006F);
        return new Rotation(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
    }

    private float getLowArcPitch(float distance, float height, float velocity, float gravity) {
        float velocitySq = velocity * velocity;
        float root = velocitySq * velocitySq - gravity * (gravity * distance * distance + 2.0F * height * velocitySq);
        if (root <= 0.0F) {
            return (float) Math.toDegrees(Math.atan2(height, distance));
        }
        return (float) Math.toDegrees(Math.atan((velocitySq - Math.sqrt(root)) / (gravity * distance)));
    }

    private double getHorizontalDistance(LivingEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isThrowable(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.isOf(Items.EGG) || stack.isOf(Items.SNOWBALL))
                && !InventoryUtils.isWindCharge(stack);
    }

    private boolean shouldPause() {
        return Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
                || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                || Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled();
    }

    private void restoreSlot() {
        if (mc.player != null && this.restoreSlot >= 0 && this.restoreSlot < 9) {
            mc.player.getInventory().selectedSlot = this.restoreSlot;
        }
        this.restoreSlot = -1;
    }

    private void clearState() {
        this.rotation = null;
        this.targetRotations = null;
        this.rotationSet = 0;
        this.pendingPlan = null;
    }

    private static class ThrowPlan {
        private final Hand hand;
        private final int hotbarSlot;

        private ThrowPlan(Hand hand, int hotbarSlot) {
            this.hand = hand;
            this.hotbarSlot = hotbarSlot;
        }
    }
}
