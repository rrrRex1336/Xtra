package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
        name = "MiddlePearl",
        description = "Middle click to auto switch and throw ender pearl",
        category = Category.PLAYER
)
public class MiddlePearl extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Fast Switch", "Spoof")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final ModeValue keyMode = ValueBuilder.create(this, "Trigger Key")
            .setModes("Middle Button", "Mouse4", "Mouse5")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final FloatValue switchToDelay = ValueBuilder.create(this, "Switch Delay")
            .setDefaultFloatValue(50.0f)
            .setMinFloatValue(50.0f)
            .setMaxFloatValue(500.0f)
            .setFloatStep(50.0f)
            .setVisibility(() -> this.mode.isCurrentMode("Fast Switch"))
            .build()
            .getFloatValue();

    private final FloatValue switchBackDelay = ValueBuilder.create(this, "Switch Back Delay")
            .setDefaultFloatValue(50.0f)
            .setMinFloatValue(50.0f)
            .setMaxFloatValue(500.0f)
            .setFloatStep(50.0f)
            .setVisibility(() -> this.mode.isCurrentMode("Fast Switch"))
            .build()
            .getFloatValue();

    private int originalSlot = -1;
    private int pearlSlot = -1;
    private boolean preparingPearl;
    private long switchAt = -1L;
    private long restoreAt = -1L;

    @Override
    public void onDisable() {
        this.cancelPreparedPearl();
        super.onDisable();
    }

    @EventTarget
    public void onMouseClick(EventMouseClick event) {
        int triggerKey = this.getTriggerKey();
        if (event.getKey() != triggerKey || mc.player == null || mc.interactionManager == null) {
            return;
        }

        if (!event.isState()) {
            this.preparePearl();
        } else {
            this.throwPreparedPearl();
        }
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (event.getKey() == GLFW.GLFW_KEY_TAB && event.isState() && this.preparingPearl) {
            this.cancelPreparedPearl();
        }
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        long now = System.currentTimeMillis();
        if (this.preparingPearl && this.switchAt > 0L && now >= this.switchAt) {
            this.switchToPearl();
            this.switchAt = -1L;
        }

        if (!this.preparingPearl && this.restoreAt > 0L && now >= this.restoreAt) {
            this.restoreOriginalSlot();
            this.restoreAt = -1L;
        }
    }

    private void preparePearl() {
        if (this.preparingPearl) {
            return;
        }

        this.pearlSlot = this.findPearlSlot();
        if (this.pearlSlot == -1) {
            ChatUtils.addChatMessage("Pearl Not Found in hotbar!");
            return;
        }

        this.originalSlot = mc.player.getInventory().selectedSlot;
        this.preparingPearl = true;
        this.restoreAt = -1L;
        if (this.mode.isCurrentMode("Fast Switch")) {
            this.switchAt = System.currentTimeMillis() + (long) this.switchToDelay.getCurrentValue();
        } else {
            this.switchAt = -1L;
            this.switchToPearl();
        }
    }

    private void throwPreparedPearl() {
        if (!this.preparingPearl || mc.player == null || mc.interactionManager == null) {
            return;
        }

        this.switchToPearl();
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        this.preparingPearl = false;
        this.switchAt = -1L;

        if (this.mode.isCurrentMode("Fast Switch")) {
            this.restoreAt = System.currentTimeMillis() + (long) this.switchBackDelay.getCurrentValue();
        } else {
            this.restoreOriginalSlot();
        }
    }

    private void cancelPreparedPearl() {
        if (this.preparingPearl || this.restoreAt > 0L) {
            this.restoreOriginalSlot();
        }
        this.preparingPearl = false;
        this.switchAt = -1L;
        this.restoreAt = -1L;
        this.pearlSlot = -1;
    }

    private void switchToPearl() {
        if (mc.player != null && this.pearlSlot >= 0 && this.pearlSlot < 9) {
            mc.player.getInventory().selectedSlot = this.pearlSlot;
        }
    }

    private void restoreOriginalSlot() {
        if (mc.player != null && this.originalSlot >= 0 && this.originalSlot < 9) {
            mc.player.getInventory().selectedSlot = this.originalSlot;
        }
        this.originalSlot = -1;
    }

    private int getTriggerKey() {
        if (this.keyMode.isCurrentMode("Mouse4")) {
            return 3;
        }
        if (this.keyMode.isCurrentMode("Mouse5")) {
            return 4;
        }
        return 2;
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENDER_PEARL && stack.getCount() > 0) {
                return i;
            }
        }
        return -1;
    }
}
