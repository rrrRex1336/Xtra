package awa.qwq.ovo.Naven.modules.impl.movement;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMoveInput;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.ClickGUI;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@ModuleInfo(
        name = "InventoryMove",
        description = "Enables movement while GUI is open(if With ACA maybe banned)",
        category = Category.MOVEMENT
)
public class InventoryMove extends Module {
    private final Minecraft minecraft = Minecraft.getInstance();

    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Normal", "Heypixel")
            .build()
            .getModeValue();

    public BooleanValue sneak = ValueBuilder.create(this, "Sneak")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue sprint = ValueBuilder.create(this, "Sprint")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private boolean quickMoveWarning;
    private boolean wasInInventory;
    private boolean wasSprintingBeforeGui;
    private boolean isInGui;

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        setSuffix(mode.getCurrentMode());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() != EventType.SEND || event.isCancelled() || this.minecraft.player == null || !mode.isCurrentMode("Heypixel")) {
            return;
        }

        if (!this.quickMoveWarning && event.getPacket() instanceof ServerboundContainerClickPacket clickPacket && this.isQuickMoveToHotbar(clickPacket)) {
            this.quickMoveWarning = true;
            ChatUtils.addChatMessage("You must close inventory after 0.4s~.");
        }
    }

    @EventTarget(3)
    public void onMoveInput(EventMoveInput event) {
        if (!this.isMovementAllowed()) {
            if (this.isInGui) {
                this.wasSprintingBeforeGui = false;
                this.isInGui = false;
            }
            return;
        }

        if (!this.isInGui) {
            this.isInGui = true;
            if (this.minecraft.player != null) {
                this.wasSprintingBeforeGui = this.minecraft.player.isSprinting();
                if (this.shouldStopSprintInGui()) {
                    this.stopGuiSprint(this.minecraft.player);
                }
            }
        }

        event.setForward(this.calculateForwardMovement());
        event.setStrafe(this.calculateStrafeMovement());
        event.setJump(this.isKeyActive(this.minecraft.options.keyJump));
        event.setSneak(this.sneak.getCurrentValue() && this.isKeyActive(this.minecraft.options.keyShift));
    }

    @EventTarget
    public void processTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE || this.minecraft.player == null) {
            return;
        }

        this.updateHeypixelInventoryState();

        if (!this.isMovementAllowed()) {
            return;
        }

        LocalPlayer player = this.minecraft.player;
        if (this.shouldStopSprintInGui()) {
            this.stopGuiSprint(player);
        }

        if (this.sprint.getCurrentValue() && this.wasSprintingBeforeGui && this.canContinueSprinting(player)) {
            player.setSprinting(true);
        }

        this.adjustPlayerRotation();
    }

    private void updateHeypixelInventoryState() {
        if (!mode.isCurrentMode("Heypixel")) {
            this.wasInInventory = false;
            this.quickMoveWarning = false;
            return;
        }

        boolean currentlyInInventory = this.minecraft.screen instanceof AbstractContainerScreen;
        if (currentlyInInventory && !this.wasInInventory) {
            this.quickMoveWarning = false;
        }
        this.wasInInventory = currentlyInInventory;
    }

    private boolean isQuickMoveToHotbar(ServerboundContainerClickPacket clickPacket) {
        if (clickPacket.getClickType() != ClickType.QUICK_MOVE || this.minecraft.player == null) {
            return false;
        }

        AbstractContainerMenu menu = this.minecraft.player.containerMenu;
        if (menu == null || clickPacket.getContainerId() != menu.containerId) {
            return false;
        }

        for (int slotId : clickPacket.getChangedSlots().keySet()) {
            if (slotId < 0 || slotId >= menu.slots.size()) {
                continue;
            }

            Slot slot = menu.getSlot(slotId);
            ItemStack changedStack = clickPacket.getChangedSlots().get(slotId);
            if (changedStack != null
                    && !changedStack.isEmpty()
                    && slot.container == this.minecraft.player.getInventory()
                    && slot.getContainerSlot() >= 0
                    && slot.getContainerSlot() <= 8) {
                return true;
            }
        }

        return false;
    }

    private boolean isKeyActive(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(
                minecraft.getWindow().getWindow(),
                keyMapping.getDefaultKey().getValue()
        );
    }

    private boolean isKeyActive(int keyCode) {
        return InputConstants.isKeyDown(
                minecraft.getWindow().getWindow(),
                keyCode
        );
    }

    private boolean canContinueSprinting(LocalPlayer player) {
        boolean isMovingForward = player.input.forwardImpulse > 0.0F;
        boolean isInValidState = player.getHealth() > 0.0F && !player.isInWater() && !player.isInLava() && !player.isShiftKeyDown() && !player.isPassenger() && !player.input.jumping;
        return isMovingForward && isInValidState;
    }

    public boolean shouldStopSprintInGui() {
        return this.isEnabled() && !this.sprint.getCurrentValue() && this.isMovementAllowed();
    }

    public static boolean shouldStopSprintForSprintModule() {
        try {
            Module module = Naven.getInstance().getModuleManager().getModule(InventoryMove.class);
            if (module instanceof InventoryMove) {
                return ((InventoryMove) module).shouldStopSprintInGui();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void stopGuiSprint(LocalPlayer player) {
        this.minecraft.options.keySprint.setDown(false);
        this.minecraft.options.toggleSprint().set(false);
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private boolean isMovementAllowed() {
        Screen currentScreen = this.minecraft.screen;
        return this.minecraft.player != null && currentScreen != null && (this.isContainerScreen(currentScreen) || this.isClickGuiScreen(currentScreen));
    }

    private boolean isContainerScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen;
    }

    private boolean isClickGuiScreen(Screen screen) {
        String className = screen.getClass().getSimpleName();
        return screen instanceof ClickGUI || className.contains("ClickGUI") || className.contains("ClickGui");
    }

    private float calculateForwardMovement() {
        if (this.isKeyActive(this.minecraft.options.keyUp)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.keyDown) ? -1.0F : 0.0F;
        }
    }

    private float calculateStrafeMovement() {
        if (this.isKeyActive(this.minecraft.options.keyLeft)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.keyRight) ? -1.0F : 0.0F;
        }
    }

    private void adjustPlayerRotation() {
        LocalPlayer player = this.minecraft.player;
        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();
        if (this.isKeyActive(265)) {
            player.setXRot(Math.max(currentPitch - 5.0F, -90.0F));
        }

        if (this.isKeyActive(264)) {
            player.setXRot(Math.min(currentPitch + 5.0F, 90.0F));
        }

        if (this.isKeyActive(263)) {
            player.setYRot(currentYaw - 5.0F);
        }

        if (this.isKeyActive(262)) {
            player.setYRot(currentYaw + 5.0F);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.quickMoveWarning = false;
        this.wasInInventory = false;
        this.wasSprintingBeforeGui = false;
        this.isInGui = false;
    }
}
