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
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;

@ModuleInfo(
        name = "InventoryMove",
        description = "Enables movement while GUI is open(if With ACA maybe banned)",
        category = Category.MOVEMENT
)
public class InventoryMove extends Module {
    private final MinecraftClient minecraft = MinecraftClient.getInstance();

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
    private boolean releasingHeypixelInventoryPackets;
    private final Queue<Packet<?>> heypixelInventoryPackets = new ConcurrentLinkedQueue<>();

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        setSuffix(mode.getCurrentMode());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() != EventType.SEND || event.isCancelled() || this.minecraft.player == null || !mode.isCurrentMode("Heypixel")) {
            return;
        }

        if (this.releasingHeypixelInventoryPackets) {
            return;
        }

        if (event.getPacket() instanceof ClickSlotC2SPacket clickPacket && this.shouldQueueHeypixelInventoryClick(clickPacket)) {
            if (!this.quickMoveWarning) {
                this.quickMoveWarning = true;
                ChatUtils.addChatMessage("You must close inventory after 0.4s~.");
            }
            event.setCancelled(true);
            this.heypixelInventoryPackets.offer(clickPacket);
            return;
        }

        if (event.getPacket() instanceof CloseHandledScreenC2SPacket closePacket && this.shouldReleaseQueuedInventoryPackets(closePacket)) {
            event.setCancelled(true);
            this.releaseHeypixelInventoryPackets();
            NetworkUtils.sendPacketNoEvent(closePacket);
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
        event.setJump(this.isKeyActive(this.minecraft.options.jumpKey));
        event.setSneak(this.sneak.getCurrentValue() && this.isKeyActive(this.minecraft.options.sneakKey));
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

        ClientPlayerEntity player = this.minecraft.player;
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
            this.quickMoveWarning = false;
            this.wasInInventory = false;
            this.releaseHeypixelInventoryPackets();
            return;
        }

        boolean currentlyInInventory = this.isPlayerInventoryScreen();
        if (currentlyInInventory && !this.wasInInventory) {
            this.quickMoveWarning = false;
        }
        if (this.wasInInventory && !currentlyInInventory) {
            this.releaseHeypixelInventoryPackets();
        }
        this.wasInInventory = currentlyInInventory;
    }

    private boolean shouldQueueHeypixelInventoryClick(ClickSlotC2SPacket clickPacket) {
        return this.isPlayerInventoryScreen()
                && clickPacket.getSyncId() == this.minecraft.player.playerScreenHandler.syncId;
    }

    private boolean shouldReleaseQueuedInventoryPackets(CloseHandledScreenC2SPacket closePacket) {
        return !this.heypixelInventoryPackets.isEmpty()
                && this.minecraft.player != null
                && closePacket.getSyncId() == this.minecraft.player.playerScreenHandler.syncId;
    }

    private boolean isPlayerInventoryScreen() {
        if (this.minecraft.player == null) {
            return false;
        }

        ScreenHandler menu = this.minecraft.player.currentScreenHandler;
        return this.minecraft.currentScreen instanceof InventoryScreen
                && menu != null
                && menu.syncId == this.minecraft.player.playerScreenHandler.syncId;
    }

    private void releaseHeypixelInventoryPackets() {
        if (this.heypixelInventoryPackets.isEmpty()) {
            return;
        }

        if (this.minecraft.getNetworkHandler() == null) {
            this.heypixelInventoryPackets.clear();
            return;
        }

        this.releasingHeypixelInventoryPackets = true;
        try {
            while (!this.heypixelInventoryPackets.isEmpty()) {
                Packet<?> packet = this.heypixelInventoryPackets.poll();
                if (packet != null) {
                    NetworkUtils.sendPacketNoEvent(packet);
                }
            }
        } finally {
            this.releasingHeypixelInventoryPackets = false;
        }
    }

    private boolean isKeyActive(KeyBinding keyMapping) {
        return InputUtil.isKeyPressed(
                minecraft.getWindow().getHandle(),
                keyMapping.getDefaultKey().getCode()
        );
    }

    private boolean isKeyActive(int keyCode) {
        return InputUtil.isKeyPressed(
                minecraft.getWindow().getHandle(),
                keyCode
        );
    }

    private boolean canContinueSprinting(ClientPlayerEntity player) {
        boolean isMovingForward = player.input.movementForward > 0.0F;
        boolean isInValidState = player.getHealth() > 0.0F && !player.isTouchingWater() && !player.isInLava() && !player.isSneaking() && !player.hasVehicle() && !player.input.jumping;
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

    private void stopGuiSprint(ClientPlayerEntity player) {
        this.minecraft.options.sprintKey.setPressed(false);
        this.minecraft.options.getSprintToggled().setValue(false);
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private boolean isMovementAllowed() {
        Screen currentScreen = this.minecraft.currentScreen;
        return this.minecraft.player != null && currentScreen != null && (this.isContainerScreen(currentScreen) || this.isClickGuiScreen(currentScreen));
    }

    private boolean isContainerScreen(Screen screen) {
        return screen instanceof HandledScreen;
    }

    private boolean isClickGuiScreen(Screen screen) {
        String className = screen.getClass().getSimpleName();
        return screen instanceof ClickGUI || className.contains("ClickGUI") || className.contains("ClickGui");
    }

    private float calculateForwardMovement() {
        if (this.isKeyActive(this.minecraft.options.forwardKey)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.backKey) ? -1.0F : 0.0F;
        }
    }

    private float calculateStrafeMovement() {
        if (this.isKeyActive(this.minecraft.options.leftKey)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.rightKey) ? -1.0F : 0.0F;
        }
    }

    private void adjustPlayerRotation() {
        ClientPlayerEntity player = this.minecraft.player;
        float currentPitch = player.getPitch();
        float currentYaw = player.getYaw();
        if (this.isKeyActive(265)) {
            player.setPitch(Math.max(currentPitch - 5.0F, -90.0F));
        }

        if (this.isKeyActive(264)) {
            player.setPitch(Math.min(currentPitch + 5.0F, 90.0F));
        }

        if (this.isKeyActive(263)) {
            player.setYaw(currentYaw - 5.0F);
        }

        if (this.isKeyActive(262)) {
            player.setYaw(currentYaw + 5.0F);
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
        this.releaseHeypixelInventoryPackets();
        this.wasInInventory = false;
        this.wasSprintingBeforeGui = false;
        this.isInGui = false;
        this.releasingHeypixelInventoryPackets = false;
    }
}
