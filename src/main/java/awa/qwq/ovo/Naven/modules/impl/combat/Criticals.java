package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.SkipTicks;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.managers.packets.AlinkManager;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.mixin.accessors.MultiPlayerGameModeAccessor;

@ModuleInfo(
        name = "Criticals",
        category = Category.COMBAT,
        description = "more damage"
)
public class Criticals extends Module {
    public static Criticals instance;
    public final ModeValue modeValue = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Switch", "Skip Ticks", "Packet", "Legit").build().getModeValue();
    public final BooleanValue packet = ValueBuilder.create(this, "Packet (Danger)").setVisibility(() -> modeValue.isCurrentMode("Switch")).setDefaultBooleanValue(false).build().getBooleanValue();
    public final BooleanValue silent = ValueBuilder.create(this, "Silent").setVisibility(() -> modeValue.isCurrentMode("Switch")).setDefaultBooleanValue(false).build().getBooleanValue();
    public FloatValue skipTicks = ValueBuilder.create(this, "Skip Ticks").setVisibility(() -> modeValue.isCurrentMode("Skip Ticks")).setDefaultFloatValue(1.0f).setMinFloatValue(1.0f).setMaxFloatValue(5.0f).setFloatStep(1f).build().getFloatValue();
    public FloatValue rangeValue = ValueBuilder.create(this, "Range").setVisibility(() -> modeValue.isCurrentMode("Skip Ticks") || modeValue.isCurrentMode("Packet")).setDefaultFloatValue(3.0f).setMinFloatValue(0.1f).setMaxFloatValue(6.0f).setFloatStep(0.1f).build().getFloatValue();
    public final BooleanValue autoJump = ValueBuilder.create(this, "Auto Jump").setVisibility(() -> modeValue.isCurrentMode("Skip Ticks") || modeValue.isCurrentMode("Packet") || modeValue.isCurrentMode("Legit")).setDefaultBooleanValue(false).build().getBooleanValue();
    int lastSlot = -1;
    private int previousSlot = -1;
    public static TimeHelper timer = new TimeHelper();
    private final TimeHelper packetDelay = new TimeHelper();
    private boolean attacking = false;
    private int offGroundTicks = 0;
    private int lastStackTick = -1;
    private int lastStackTargetId = -1;
    private Entity pendingAuraCriticalTarget = null;
    private boolean auraCriticalPrepared = false;
    private boolean restorePlayerSprintingAfterAuraCritical = false;
    private boolean restoreSprintKeyAfterAuraCritical = false;
    private boolean runningAuraCriticalAttack = false;

    public Criticals() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.previousSlot = -1;
        this.lastSlot = -1;
        this.lastStackTick = -1;
        this.lastStackTargetId = -1;
        clearAuraCritical();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.previousSlot = -1;
        this.lastSlot = -1;
        this.lastStackTick = -1;
        this.lastStackTargetId = -1;
        clearAuraCritical();
        if (SkipTicks.isActive()) {
            SkipTicks.dispatch();
        }
        super.onDisable();
    }

    private boolean doTiger(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || InventoryUtils.isSharpnessAxe(stack) && !InventoryUtils.isGodAxe(stack);
    }

    @EventTarget
    public void onWorld(EventRespawn event) {
        this.lastSlot = -1;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {

        if (modeValue.isCurrentMode("Skip Ticks")) {
            if (mc.player.isOnGround()) {
                SkipTicks.dispatch();
                return;
            }
            if (SkipTicks.isActive()) {
                HitResult hit = mc.crosshairTarget;
                boolean hasTarget = false;
                if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) hit).getEntity();
                    if (entity instanceof LivingEntity && mc.player.distanceTo(entity) <= rangeValue.getCurrentValue()) {
                        hasTarget = true;
                    }
                }
                if (!hasTarget) {
                    SkipTicks.dispatch();
                }
            }
        }
        if (modeValue.isCurrentMode("Legit")) {
            if (mc.player.isOnGround()) {
                offGroundTicks = 0;
                attacking = false;
            } else {
                offGroundTicks++;
            }
        }
    }

    @EventTarget(4)
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE) return;

        prepareAuraCriticalSprint();

        if (modeValue.isCurrentMode("Legit")) {
            if (KillAura.target != null && attacking) {
                if (mc.player.fallDistance > 0 || offGroundTicks > 3) {
                    e.setOnGround(false);
                }
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (modeValue.isCurrentMode("Legit")) {
            if (event.isPost()) return;
            attacking = true;
            if ((mc.player.fallDistance > 0 || offGroundTicks > 3) && event.getTarget() instanceof LivingEntity) {

            }
            return;
        }

        if (modeValue.isCurrentMode("Skip Ticks")) {
            if (mc.player.isOnGround() || mc.player.getVelocity().y >= 0) {
                return;
            }
            if (!SkipTicks.isActive()) {
                SkipTicks.skipTicks(skipTicks.getCurrentValue());
            }
            return;
        }

        if (modeValue.isCurrentMode("Packet")) {
            if (!event.isPost() && !runningAuraCriticalAttack && canAttemptHighVersionCritical(event.getTarget())) {
                mc.player.resetLastAttackedTicks();
            }
            return;
        }

        if (modeValue.isCurrentMode("Switch")) {
            if (event.isPost()) {
                if (timer.delay(300.0) && this.previousSlot != -1 && mc.player.getInventory().selectedSlot != this.previousSlot) {
                    mc.player.getInventory().selectedSlot = this.previousSlot;
                    ((MultiPlayerGameModeAccessor) mc.interactionManager).invokeEnsureHasSentCarriedItem();
                    this.previousSlot = -1;
                }
            } else if (this.overrideSwordSorting() && this.doTiger(mc.player.getMainHandStack())) {
                if (EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, mc.player.getMainHandStack()) == 0 && KillAura.targets.isEmpty()) {
                    for (int i = 36; i < 45; i++) {
                        ItemStack curSlot = mc.player.playerScreenHandler.getSlot(i).getStack();
                        if (curSlot != mc.player.getMainHandStack()
                                && curSlot.getItem() instanceof SwordItem
                                && curSlot.getItem() == Items.WOODEN_SWORD
                                && EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, curSlot) > 0) {
                            mc.player.getInventory().selectedSlot = i - 36;
                            ((MultiPlayerGameModeAccessor) mc.interactionManager).invokeEnsureHasSentCarriedItem();
                            timer.reset();
                            return;
                        }
                    }
                }

                if (KillAura.targets.isEmpty()) {
                    return;
                }

                int choice = -1;
                float score = 10000.0F;
                int worstChoice = -1;
                float worstScore = 0.0F;

                for (int ix = 36; ix < 45; ix++) {
                    ItemStack curSlot = mc.player.playerScreenHandler.getSlot(ix).getStack();
                    if (curSlot != mc.player.getMainHandStack() && !curSlot.isEmpty() && (ix - 36 == 8 || ix - 36 == 0) && this.doTiger(curSlot)) {
                        float delta = (float) (InventoryUtils.getItemDamage(curSlot) - InventoryUtils.getItemDamage(mc.player.getMainHandStack()));
                        if (delta > 0.0F && delta < score) {
                            choice = ix;
                            score = delta;
                        }

                        if (delta < 0.0F && delta < worstScore) {
                            worstChoice = ix;
                            worstScore = delta;
                        }
                    }
                }

                if (choice != -1) {
                    int resultSlot = choice - 36;
                    if (this.previousSlot == -1) {
                        this.previousSlot = mc.player.getInventory().selectedSlot;
                    }

                    if (this.packet.getCurrentValue()) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(resultSlot));
                    } else {
                        mc.player.getInventory().selectedSlot = resultSlot;
                        ((MultiPlayerGameModeAccessor) mc.interactionManager).invokeEnsureHasSentCarriedItem();
                    }
                } else if (worstChoice != -1) {
                    int resultSlotx = worstChoice - 36;
                    if (this.previousSlot == -1) {
                        this.previousSlot = mc.player.getInventory().selectedSlot;
                    }

                    if (this.packet.getCurrentValue()) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(resultSlotx));
                    } else {
                        mc.player.getInventory().selectedSlot = resultSlotx;
                        ((MultiPlayerGameModeAccessor) mc.interactionManager).invokeEnsureHasSentCarriedItem();
                    }
                }

                timer.reset();
            } else {
                this.lastSlot = -1;
            }
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (autoJump.getCurrentValue() && mc.player.isOnGround()) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hit).getEntity();
                if (entity instanceof LivingEntity && mc.player.distanceTo(entity) <= rangeValue.getCurrentValue()) {
                    event.setJump(true);
                }
            }
        }
    }

    public static boolean shouldHoldAuraAttack(Entity target) {
        return instance != null && instance.shouldHoldAuraAttack0(target);
    }

    public static boolean tryPerformAuraCriticalAttack(Entity target) {
        return instance != null && instance.tryPerformAuraCriticalAttack0(target);
    }

    public static void afterAuraAttack(Entity target) {
        if (instance != null) {
            instance.queueAuraCritical(target);
        }
    }

    private boolean shouldHandlePacketMode() {
        return isEnabled() && modeValue.isCurrentMode("Packet") && mc.player != null && mc.world != null;
    }

    private boolean shouldHoldAuraAttack0(Entity target) {
        if (!shouldHandlePacketMode() || runningAuraCriticalAttack || pendingAuraCriticalTarget == null) {
            return false;
        }
        if (!isPendingAuraTarget(target)) {
            return false;
        }
        if (!canAttemptHighVersionCritical(target)) {
            clearAuraCritical();
            return false;
        }
        return !auraCriticalPrepared;
    }

    private boolean tryPerformAuraCriticalAttack0(Entity target) {
        if (!shouldHandlePacketMode()
                || runningAuraCriticalAttack
                || !auraCriticalPrepared
                || !isPendingAuraTarget(target)
                || !canAttemptHighVersionCritical(target)
                || mc.interactionManager == null) {
            return false;
        }

        boolean restorePlayerSprinting = restorePlayerSprintingAfterAuraCritical;
        boolean restoreSprintKey = restoreSprintKeyAfterAuraCritical;
        runningAuraCriticalAttack = true;
        try {
            mc.options.sprintKey.setPressed(false);
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();
            lastStackTick = mc.player.age;
            lastStackTargetId = target.getId();
            packetDelay.reset();
        } finally {
            runningAuraCriticalAttack = false;
            clearAuraCritical();
            if (restorePlayerSprinting) {
                mc.player.setSprinting(true);
            }
            if (restoreSprintKey) {
                mc.options.sprintKey.setPressed(true);
            }
        }
        return true;
    }

    private void queueAuraCritical(Entity target) {
        if (!shouldHandlePacketMode()
                || runningAuraCriticalAttack
                || !packetDelay.delay(45.0)
                || !canAttemptHighVersionCritical(target)) {
            return;
        }
        if (mc.player.age == lastStackTick && target.getId() == lastStackTargetId) {
            return;
        }

        pendingAuraCriticalTarget = target;
        auraCriticalPrepared = false;
        restorePlayerSprintingAfterAuraCritical = false;
        restoreSprintKeyAfterAuraCritical = false;
        lastStackTick = mc.player.age;
        lastStackTargetId = target.getId();
        packetDelay.reset();
    }

    private void prepareAuraCriticalSprint() {
        if (!shouldHandlePacketMode() || pendingAuraCriticalTarget == null) {
            return;
        }
        if (!canAttemptHighVersionCritical(pendingAuraCriticalTarget)) {
            clearAuraCritical();
            return;
        }

        if (!auraCriticalPrepared) {
            restorePlayerSprintingAfterAuraCritical = mc.player.isSprinting();
            restoreSprintKeyAfterAuraCritical = mc.options.sprintKey.isPressed();
        }
        mc.options.sprintKey.setPressed(false);
        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
        auraCriticalPrepared = true;
    }

    private boolean canAttemptHighVersionCritical(Entity target) {
        if (!(target instanceof LivingEntity living) || mc.player == null) {
            return false;
        }
        return !living.isDead()
                && living.getHealth() > 0.0F
                && mc.player.distanceTo(target) <= rangeValue.getCurrentValue()
                && mc.player.fallDistance > 0.0F
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle();
    }

    private boolean isPendingAuraTarget(Entity target) {
        return target != null
                && pendingAuraCriticalTarget != null
                && target.getId() == pendingAuraCriticalTarget.getId();
    }

    private void clearAuraCritical() {
        pendingAuraCriticalTarget = null;
        auraCriticalPrepared = false;
        restorePlayerSprintingAfterAuraCritical = false;
        restoreSprintKeyAfterAuraCritical = false;
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (modeValue.isCurrentMode("Switch")) {
            if (this.overrideSwordSorting()
                    && !KillAura.targets.isEmpty()
                    && !this.packet.getCurrentValue()
                    && e.getHand() == Hand.MAIN_HAND
                    && this.previousSlot != -1
                    && this.silent.getCurrentValue()) {
                e.setItem(mc.player.getInventory().getStack(this.previousSlot));
            }
        }
    }

    @EventTarget
    public void onPacketEvent(EventPacket event) {
        if (AlinkManager.onPacketReceive(event)) {
            event.setCancelled(true);
        }
        if (this.packet.getCurrentValue() && event.getType() == EventType.SEND && event.getPacket() instanceof UpdateSelectedSlotC2SPacket carriedItemPacket) {
            int slot = carriedItemPacket.getSelectedSlot();
            if (slot == this.lastSlot && slot != -1) {
                event.setCancelled(true);
            }
            this.lastSlot = carriedItemPacket.getSelectedSlot();
        }
    }

    public boolean overrideSwordSorting() {
        return this.isEnabled() && modeValue.isCurrentMode("Switch");
    }
}
