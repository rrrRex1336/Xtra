package awa.qwq.ovo.Naven.modules.impl.Combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(name = "ShieldBreaker", category = Category.COMBAT, description = "Automatically breaks shields by silently attacking them.")
public class ShieldBreaker extends Module {

    private final FloatValue spamCPS = ValueBuilder.create(this, "Spam CPS")
            .setDefaultFloatValue(10.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(20.0f)
            .build()
            .getFloatValue();

    private final FloatValue switchBackDelay = ValueBuilder.create(this, "Switch Back Delay (Ticks)")
            .setDefaultFloatValue(2.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0f)
            .build()
            .getFloatValue();

    private long lastAttackTime = 0L;
    private int originalSlot = -1;
    private long attackTick = -1L;

    @Override
    public void onDisable() {
        if (this.originalSlot != -1) {
            ShieldBreaker.mc.player.getInventory().selected = this.originalSlot;
        }
        this.originalSlot = -1;
        this.attackTick = -1L;
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (ShieldBreaker.mc.player == null ||
                ShieldBreaker.mc.gameMode == null ||
                ShieldBreaker.mc.level == null) {
            return;
        }

        if (ShieldBreaker.mc.player.isUsingItem()) {
            return;
        }

        if (this.originalSlot != -1) {
            if ((float) (ShieldBreaker.mc.player.tickCount - this.attackTick) >= this.switchBackDelay.getCurrentValue()) {
                    ShieldBreaker.mc.player.getInventory().selected = this.originalSlot;
                this.originalSlot = -1;
                this.attackTick = -1L;
            }
            return;
        }

        long currentTime = System.currentTimeMillis();
        long attackInterval = (long) (1000.0f / this.spamCPS.getCurrentValue());
        if (currentTime - this.lastAttackTime < attackInterval) {
            return;
        }

        HitResult hitResult = ShieldBreaker.mc.hitResult;
        if (hitResult instanceof EntityHitResult) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof Player) {
                Player target = (Player) entity;

                if (this.isPlayerUsingShield(target)) {
                    int axeSlot = this.findAxeInHotbar();
                    if (axeSlot != -1) {
                        this.originalSlot = ShieldBreaker.mc.player.getInventory().selected;
                        ShieldBreaker.mc.player.getInventory().selected = axeSlot;
                        ShieldBreaker.mc.gameMode.attack(ShieldBreaker.mc.player, target);
                        ShieldBreaker.mc.player.swing(InteractionHand.MAIN_HAND);
                        this.attackTick = ShieldBreaker.mc.player.tickCount;
                        this.lastAttackTime = currentTime;
                    }
                }
            }
        }
    }

    private int findAxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ShieldBreaker.mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlayerUsingShield(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return player.isUsingItem() && player.getUseItem().is(Items.SHIELD);
        }
        return false;
    }
}
