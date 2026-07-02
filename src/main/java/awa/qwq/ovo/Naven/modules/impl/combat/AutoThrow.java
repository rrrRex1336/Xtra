package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.misc.Teams;
import awa.qwq.ovo.Naven.modules.impl.movement.Stuck;
import awa.qwq.ovo.Naven.modules.impl.player.Blink;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.managers.friends.FriendManager;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.managers.rotation.utils.Rotation;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import lombok.Getter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;

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
    private int swapBack = -1;
    private ThrowPlan pendingPlan;
    public Vector2f targetRotations = null;

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE) {
            if (swapBack != -1) {
                mc.player.getInventory().selected = swapBack;
                swapBack = -1;
            }
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        if (Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled() || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled() || Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            rotationSet = 0;
            pendingPlan = null;
            targetRotations = null;
            return;
        }

        rotation = null;

        ThrowPlan plan = findThrowPlan();
        if (plan == null) {
            return;
        }

        if (rotationSet > 0) {
            rotationSet--;
            if (targetRotations != null) {
                RotationManager.setRotations(targetRotations);
            }

            if (rotationSet == 0 && pendingPlan != null) {
                throwFromPlan(pendingPlan);
                pendingPlan = null;
            }
            return;
        }

        Optional<? extends LivingEntity> target = getTarget();
        if (target.isPresent() && timer.delay(delay.getCurrentValue()) && canRotate(plan.hand)) {
            Rotation newRotation = getRotationToEntity(target.get());
            targetRotations = new Vector2f(newRotation.getYaw(), newRotation.getPitch());
            RotationManager.setRotations(targetRotations);

            rotationSet = 2;
            pendingPlan = plan;
            timer.reset();
        }
    }

    private void throwFromPlan(ThrowPlan plan) {
        if (plan.hand == InteractionHand.MAIN_HAND) {
            int originalHotbar = mc.player.getInventory().selected;
            boolean shouldSwap = originalHotbar != plan.hotbarSlot;
            if (shouldSwap) {
                mc.player.getInventory().selected = plan.hotbarSlot;
                swapBack = originalHotbar;
            }
        }
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private ThrowPlan findThrowPlan() {
        ItemStack offhand = mc.player.getOffhandItem();
        if (isThrowable(offhand)) {
            return new ThrowPlan(InteractionHand.OFF_HAND, -1);
        }

        int selected = mc.player.getInventory().selected;
        ItemStack mainhand = mc.player.getInventory().items.get(selected);
        if (isThrowable(mainhand)) {
            return new ThrowPlan(InteractionHand.MAIN_HAND, selected);
        }

        for (int hotbar = 0; hotbar < 9; hotbar++) {
            ItemStack stack = mc.player.getInventory().items.get(hotbar);
            if (isThrowable(stack)) {
                return new ThrowPlan(InteractionHand.MAIN_HAND, hotbar);
            }
        }

        return null;
    }

    private boolean canRotate(InteractionHand hand) {
        if (mc.player.isUsingItem()) {
            return false;
        }
        ItemStack stack = hand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof EnderpearlItem) {
            return false;
        }
        if (item instanceof BowItem) {
            return false;
        }
        if (item instanceof PotionItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            return false;
        }
        return !item.isEdible();
    }

    private Rotation getRotationToEntity(LivingEntity target) {
        Vec3 velocity = target.getDeltaMovement();
        double targetX = target.getX();
        double targetY = target.getY() + target.getBbHeight() * 0.6;
        double targetZ = target.getZ();

        double time = 0.0;
        for (int i = 0; i < 3; i++) {
            double predictX = targetX + velocity.x * time;
            double predictZ = targetZ + velocity.z * time;
            double dx = predictX - mc.player.getX();
            double dz = predictZ - mc.player.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            time = horizontal / 0.6;
        }

        double predictX = targetX + velocity.x * time;
        double predictY = targetY + velocity.y * time;
        double predictZ = targetZ + velocity.z * time;

        double x = predictX - mc.player.getX();
        double z = predictZ - mc.player.getZ();
        double h = predictY - (mc.player.getY() + mc.player.getEyeHeight());
        double horizontal = Math.sqrt(x * x + z * z);

        float yaw = (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0F);
        float pitch = -getTrajAngleSolutionLow((float) horizontal, (float) h, (float) 0.6, (float) 0.006);
        return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
    }

    private float getTrajAngleSolutionLow(float distance, float height, float velocity, float gravity) {
        float v2 = velocity * velocity;
        float under = v2 * v2 - gravity * (gravity * distance * distance + 2.0f * height * v2);
        if (under <= 0.0f) {
            return (float) Math.toDegrees(Math.atan2(height, distance));
        }
        return (float) Math.toDegrees(Math.atan((v2 - Math.sqrt(under)) / (gravity * distance)));
    }

    private Optional<? extends LivingEntity> getTarget() {
        return mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(maxDistance.getCurrentValue()))
                .stream()
                .filter(e -> e != mc.player)
                .filter(LivingEntity::isAlive)
                .filter(e -> !e.isSpectator())
                .filter(e -> !AntiBots.isBot(e))
                .filter(e -> !Teams.isSameTeam(e))
                .filter(e -> !FriendManager.isFriend(e))
                .filter(mc.player::hasLineOfSight)
                .filter(e -> {
                    double dist = getHorizontalDistance(e);
                    return dist <= maxDistance.getCurrentValue() && dist >= minDistance.getCurrentValue();
                })
                .filter(e -> {
                    if (e instanceof AbstractClientPlayer) {
                        return targetMode.isSelected("Player");
                    }
                    if (e.isInvisible() || e.isInvisibleTo(mc.player)) {
                        return targetMode.isSelected("Invisible");
                    }
                    if (e instanceof Animal) {
                        return targetMode.isSelected("Animals");
                    }
                    if (e instanceof Monster) {
                        return targetMode.isSelected("Mobs");
                    }
                    return false;
                })
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
    }

    private double getHorizontalDistance(LivingEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isThrowable(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == Items.EGG || stack.getItem() == Items.SNOWBALL) && !InventoryUtils.isWindCharge(stack);
    }

    private static class ThrowPlan {
        private final InteractionHand hand;
        private final int hotbarSlot;

        private ThrowPlan(InteractionHand hand, int hotbarSlot) {
            this.hand = hand;
            this.hotbarSlot = hotbarSlot;
        }
    }
}
