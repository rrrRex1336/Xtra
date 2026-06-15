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
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.mixin.accessors.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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
    public FloatValue rangeValue = ValueBuilder.create(this, "Range").setVisibility(() -> modeValue.isCurrentMode("Skip Ticks")).setDefaultFloatValue(3.0f).setMinFloatValue(0.1f).setMaxFloatValue(6.0f).setFloatStep(0.1f).build().getFloatValue();
    public final BooleanValue autoJump = ValueBuilder.create(this, "Auto Jump").setVisibility(() -> modeValue.isCurrentMode("Skip Ticks") || modeValue.isCurrentMode("Packet") || modeValue.isCurrentMode("Legit")).setDefaultBooleanValue(false).build().getBooleanValue();
    int lastSlot = -1;
    private int previousSlot = -1;
    public static TimeHelper timer = new TimeHelper();
    private final double[] criticalOffsets = {0.0625, 0.0};
    private final TimeHelper packetDelay = new TimeHelper();
    private boolean hasSentCriticalPackets = false;
    private boolean attacking = false;
    private int offGroundTicks = 0;

    public Criticals() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.previousSlot = -1;
        this.lastSlot = -1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.previousSlot = -1;
        this.lastSlot = -1;
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
            if (mc.player.onGround()) {
                SkipTicks.dispatch();
                return;
            }
            if (SkipTicks.isActive()) {
                HitResult hit = mc.hitResult;
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
            if (mc.player.onGround()) {
                offGroundTicks = 0;
                attacking = false;
            } else {
                offGroundTicks++;
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE) return;

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
            if (mc.player.onGround() || mc.player.getDeltaMovement().y >= 0) {
                return;
            }
            if (!SkipTicks.isActive()) {
                SkipTicks.skipTicks(skipTicks.getCurrentValue());
            }
            return;
        }

        if (modeValue.isCurrentMode("Packet")) {
            if (mc.player.onGround() && !hasSentCriticalPackets && packetDelay.delay(500)) {
                HitResult hit = mc.hitResult;
                if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) hit).getEntity();
                    if (entity instanceof LivingEntity && mc.player.distanceTo(entity) <= rangeValue.getCurrentValue()) {
                        if (mc.player == null) return;

                        double x = mc.player.getX();
                        double y = mc.player.getY();
                        double z = mc.player.getZ();

                        for (double offset : criticalOffsets) {
                            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + offset, z, false));
                        }
                        hasSentCriticalPackets = true;
                        packetDelay.reset();
                    }
                }
            }
            if (mc.player.onGround() && hasSentCriticalPackets) {
                hasSentCriticalPackets = false;
            }
            return;
        }

        if (modeValue.isCurrentMode("Switch")) {
            if (event.isPost()) {
                if (timer.delay(300.0) && this.previousSlot != -1 && mc.player.getInventory().selected != this.previousSlot) {
                    mc.player.getInventory().selected = this.previousSlot;
                    ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
                    this.previousSlot = -1;
                }
            } else if (this.overrideSwordSorting() && this.doTiger(mc.player.getMainHandItem())) {
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, mc.player.getMainHandItem()) == 0 && KillAura.targets.isEmpty()) {
                    for (int i = 36; i < 45; i++) {
                        ItemStack curSlot = mc.player.inventoryMenu.getSlot(i).getItem();
                        if (curSlot != mc.player.getMainHandItem()
                                && curSlot.getItem() instanceof SwordItem
                                && curSlot.getItem() == Items.WOODEN_SWORD
                                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, curSlot) > 0) {
                            mc.player.getInventory().selected = i - 36;
                            ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
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
                    ItemStack curSlot = mc.player.inventoryMenu.getSlot(ix).getItem();
                    if (curSlot != mc.player.getMainHandItem() && !curSlot.isEmpty() && (ix - 36 == 8 || ix - 36 == 0) && this.doTiger(curSlot)) {
                        float delta = (float) (InventoryUtils.getItemDamage(curSlot) - InventoryUtils.getItemDamage(mc.player.getMainHandItem()));
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
                        this.previousSlot = mc.player.getInventory().selected;
                    }

                    if (this.packet.getCurrentValue()) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(resultSlot));
                    } else {
                        mc.player.getInventory().selected = resultSlot;
                        ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
                    }
                } else if (worstChoice != -1) {
                    int resultSlotx = worstChoice - 36;
                    if (this.previousSlot == -1) {
                        this.previousSlot = mc.player.getInventory().selected;
                    }

                    if (this.packet.getCurrentValue()) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(resultSlotx));
                    } else {
                        mc.player.getInventory().selected = resultSlotx;
                        ((MultiPlayerGameModeAccessor) mc.gameMode).invokeEnsureHasSentCarriedItem();
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
        if (autoJump.getCurrentValue() && mc.player.onGround()) {
            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                Entity entity = ((EntityHitResult) hit).getEntity();
                if (entity instanceof LivingEntity && mc.player.distanceTo(entity) <= rangeValue.getCurrentValue()) {
                    event.setJump(true);
                }
            }
        }
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (modeValue.isCurrentMode("Switch")) {
            if (this.overrideSwordSorting()
                    && !KillAura.targets.isEmpty()
                    && !this.packet.getCurrentValue()
                    && e.getHand() == InteractionHand.MAIN_HAND
                    && this.previousSlot != -1
                    && this.silent.getCurrentValue()) {
                e.setItem(mc.player.getInventory().getItem(this.previousSlot));
            }
        }
    }

    @EventTarget
    public void onPacketEvent(EventPacket event) {
        if (AlinkManager.onPacketReceive(event)) {
            event.setCancelled(true);
        }
        if (this.packet.getCurrentValue() && event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSetCarriedItemPacket carriedItemPacket) {
            int slot = carriedItemPacket.getSlot();
            if (slot == this.lastSlot && slot != -1) {
                event.setCancelled(true);
            }
            this.lastSlot = carriedItemPacket.getSlot();
        }
    }

    public boolean overrideSwordSorting() {
        return this.isEnabled() && modeValue.isCurrentMode("Switch");
    }
}