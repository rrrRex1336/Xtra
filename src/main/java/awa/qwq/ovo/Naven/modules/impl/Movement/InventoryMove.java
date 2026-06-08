package awa.qwq.ovo.Naven.modules.impl.Movement;

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
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "InventoryMove",
        description = "Enables movement while GUI is open(if With ACA maybe banned)",
        category = Category.MOVEMENT
)
public class InventoryMove extends Module {
    private final Minecraft minecraft = Minecraft.getInstance();

    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Normal", "Hypixel", "Latest Grim", "Heypixel")
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
    private boolean quickMoveWarning = false;
    private boolean wasInInventory = false;
    private int tick = 0;
    private double dist = 0;
    private boolean c16 = false;
    private boolean c0d = false;
    private boolean OpenInventory = false;
    private boolean processing = false;
    private boolean hasMovedToHotbar = false;
    private long inventoryOpenTime = 0;
    private boolean waitingForClose = false;
    private ServerboundContainerClosePacket pc = null;
    private ServerboundPlayerCommandPacket c16C = null;
    public static List<Packet<?>> InvPacketList = new ArrayList<>();

    private boolean wasSprintingBeforeGui = false;
    private boolean isInGui = false;

    @EventTarget
    public void onRunTicks(EventRunTicks e) {
        setSuffix(mode.getCurrentMode());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.PRE && mode.isCurrentMode("Hypixel")) {
            Packet<?> packet = event.getPacket();

            if (packet instanceof ServerboundMovePlayerPacket) {
                if ((minecraft.screen instanceof ContainerScreen) && tick > 0) {
                    InvPacketList.add(packet);
                    event.setCancelled(true);
                }
            }

            if (packet instanceof ServerboundPlayerCommandPacket p) {
                if (c16 && p.getAction() == ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
                    event.setCancelled(true);
                }
                c16 = true;
            }

            if (packet instanceof ServerboundContainerClickPacket && (tick > 0 || OpenInventory)) {
                InvPacketList.add(packet);
                event.setCancelled(true);
            }

            if (packet instanceof ServerboundContainerClosePacket) {
                if (c0d && !(tick > 1) && OpenInventory) {
                    event.setCancelled(true);
                } else {
                    if (!InvPacketList.isEmpty()) {
                        event.setCancelled(true);
                        for (Packet<?> p : InvPacketList) {
                            minecraft.getConnection().send(p);
                        }
                        InvPacketList.clear();
                        minecraft.getConnection().send(packet);
                    }
                }
                c0d = true;
            }
        }
        //By L1ngG3
        //TODO: 林妍璃我啊，可以最强skid大蛇
        if (event.getType() != EventType.SEND || event.isCancelled() || mc.player == null) {
            return;
        }

        if (!mode.isCurrentMode("Latest Grim") && !mode.isCurrentMode("Heypixel")) {
            return;
        }

        Object packet = event.getPacket();

        //By L1ngG3
        if (packet instanceof ServerboundPlayerCommandPacket) {
            return;
        }

        // Latest Grim 和 Heypixel
        if ((packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerClosePacket)
                && mc.player.isSprinting()) {
            if (processing) {
                return;
            }
            processing = true;
            event.setCancelled(true);
            mc.player.connection.send(new ServerboundPlayerCommandPacket(
                    mc.player,
                    ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
            ));
            mc.player.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
            mc.player.connection.send(new ServerboundPlayerCommandPacket(
                    mc.player,
                    ServerboundPlayerCommandPacket.Action.START_SPRINTING
            ));
            processing = false;
        }
        if (mode.isCurrentMode("Heypixel") && packet instanceof ServerboundContainerClickPacket clickPacket) {
            if (clickPacket.getClickType() == ClickType.QUICK_MOVE && clickPacket.getSlotNum() >= 0 && clickPacket.getSlotNum() <= 8) {
                if (!quickMoveWarning) {
                    quickMoveWarning = true;
                    ChatUtils.addChatMessage("You must close inventory after 0.4s~.");
                }
            }
        }
    }

    @EventTarget(3)
    public void onMoveInput(EventMoveInput event) {
        if (!this.isMovementAllowed()) {
            if (this.isInGui) {
                this.wasSprintingBeforeGui = false;
                this.isInGui = false;
            }

            if (mode.isCurrentMode("Hypixel")) {
                if (minecraft.screen instanceof ContainerScreen) {
                    if (dist / tick > 0.05) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 0;
                        dist = 0;
                    } else if (tick > 0) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 1;
                        dist = 0;
                    }
                }
            }
        } else {
            if (!this.isInGui) {
                this.isInGui = true;
                if (this.minecraft.player != null) {
                    this.wasSprintingBeforeGui = this.minecraft.player.isSprinting();
                    if (!this.sprint.getCurrentValue() && this.minecraft.player.isSprinting()) {
                        this.minecraft.player.setSprinting(false);
                    }
                }
            }

            event.setForward(this.calculateForwardMovement());
            event.setStrafe(this.calculateStrafeMovement());
            event.setJump(this.isKeyActive(this.minecraft.options.keyJump));

            event.setSneak(this.sneak.getCurrentValue() && this.isKeyActive(this.minecraft.options.keyShift));
        }
    }

    @EventTarget
    public void processTick(EventRunTicks event) {
        if (this.isValidTickEvent(event) && this.minecraft.player != null) {
            LocalPlayer player = this.minecraft.player;

            if (mode.isCurrentMode("Hypixel")) {
                c16 = false;
                c0d = false;
                OpenInventory = false;

                if (minecraft.screen instanceof InventoryScreen) {
                    double xDist = player.getX() - player.xOld;
                    double zDist = player.getZ() - player.zOld;
                    double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                    OpenInventory = true;

                    if (tick == 1) {
                        minecraft.getConnection().send(new ServerboundPlayerCommandPacket(
                                player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
                    }

                    if (dist / tick > 0.00) {
                        if (tick == ((dist / tick > 0.45) ? 2 : 3)) {
                            if (!InvPacketList.isEmpty()) {
                                for (Packet<?> p : InvPacketList) {
                                    minecraft.getConnection().send(p);
                                }
                                InvPacketList.clear();
                            }
                        }
                        if (tick > ((dist / tick > 0.45) ? 2 : 3)) {
                            minecraft.getConnection().send(new ServerboundContainerClosePacket(0));
                            tick = 0;
                            dist = 0;
                        }
                    } else if (tick > 0) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 1;
                        dist = 0;
                    }

                    tick++;
                    dist += lastDist;
                } else if (minecraft.screen instanceof ContainerScreen) {
                    double xDist = player.getX() - player.xOld;
                    double zDist = player.getZ() - player.zOld;
                    double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                    tick++;
                    dist += lastDist;
                } else {
                    tick = 0;
                    dist = 0;
                }
            }
            if (mode.isCurrentMode("Heypixel")) {
                boolean currentlyInInventory = (minecraft.screen instanceof InventoryScreen) ||
                        (minecraft.screen instanceof AbstractContainerScreen);
                if (!wasInInventory && currentlyInInventory) {
                    quickMoveWarning = false;
                }

                wasInInventory = currentlyInInventory;
            }
            if (!this.sprint.getCurrentValue() && player.isSprinting()) {
                player.setSprinting(false);
            }

            if (this.sprint.getCurrentValue() && this.wasSprintingBeforeGui && this.canContinueSprinting(player)) {
                player.setSprinting(true);
            }

            this.adjustPlayerRotation();
        }

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

    private boolean isValidTickEvent(EventRunTicks event) {
        return event.getType() == EventType.PRE && this.isMovementAllowed();
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
        InvPacketList.clear();
        pc = null;
        c16C = null;
        quickMoveWarning = false;
        wasInInventory = false;
    }
}