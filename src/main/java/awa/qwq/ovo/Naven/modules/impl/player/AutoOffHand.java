package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import java.util.List;

@ModuleInfo(name = "AutoOffHand", description = "Smart switches the items in your OffHand based on combat state and health", category = Category.PLAYER)
public class AutoOffHand extends Module {
    public final FloatValue minGappleHealth = ValueBuilder.create(this, "Min Gapple Health")
            .setDefaultFloatValue(12F)
            .setMaxFloatValue(24F)
            .setMinFloatValue(0.5F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();
    private boolean processing = false;
    public final FloatValue switchDistance = ValueBuilder.create(this, "Player Switch Distance")
            .setDefaultFloatValue(8F)
            .setMaxFloatValue(16F)
            .setMinFloatValue(3F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    public final BooleanValue bedwars = ValueBuilder.create(this, "BedWars")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private boolean hasTargetLastTick = false;
    private ItemStack lastOffhandItem = ItemStack.EMPTY;
    private int switchCooldown = 0;
    private static final int SWITCH_COOLDOWN_TICKS = 3;
    private boolean inCombatMode = false;
    private float lastHealth = 0;
    private static final String[] THROWABLE_ITEMS = {"snowball", "egg"};

    @Override
    public void onEnable() {
        hasTargetLastTick = false;
        lastOffhandItem = ItemStack.EMPTY;
        switchCooldown = 0;
        inCombatMode = false;
        processing = false;
        lastHealth = mc.player != null ? mc.player.getHealth() : 0;
    }

    @Override
    public void onDisable() {
        processing = false;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE || mc.player == null || mc.level == null) {
            return;
        }
        if (switchCooldown > 0) {
            switchCooldown--;
            return;
        }
        if (mc.screen instanceof InventoryScreen) {
            return;
        }
        Player player = mc.player;
        ItemStack currentOffhand = player.getOffhandItem();
        float currentHealth = player.getHealth();
        boolean hasPlayerNearby = false;
        float switchRange = switchDistance.getCurrentValue();
        AABB boundingBox = player.getBoundingBox().inflate(switchRange);
        List<Entity> entities = mc.level.getEntities(player, boundingBox);

        for (Entity entity : entities) {
            if (entity instanceof Player && entity != player) {
                float distance = player.distanceTo(entity);
                if (distance <= switchRange) {
                    hasPlayerNearby = true;
                    break;
                }
            }
        }

        boolean healthBelowThreshold = currentHealth < minGappleHealth.getCurrentValue();
        boolean healthDroppedBelowThreshold = currentHealth < minGappleHealth.getCurrentValue() &&
                lastHealth >= minGappleHealth.getCurrentValue();

        lastHealth = currentHealth;
        boolean shouldUseGapple = false;
        boolean shouldUseThrowable = false;

        if (healthBelowThreshold) {
            shouldUseGapple = true;
            inCombatMode = false;
        } else if (hasPlayerNearby) {
            shouldUseThrowable = true;
            inCombatMode = true;
        } else {
            shouldUseGapple = true;
            inCombatMode = false;
        }

        boolean needsToSwitch = false;
        if (shouldUseGapple) {
            if (!isGoldenApple(currentOffhand)) {
                needsToSwitch = true;
            }
        } else if (shouldUseThrowable) {
            if (!isThrowableItem(currentOffhand)) {
                needsToSwitch = true;
            }
        }

        if (needsToSwitch) {
            if (shouldUseGapple) {
                switchToGoldenApple();
            } else if (shouldUseThrowable) {
                switchToBestThrowable();
            }

            switchCooldown = SWITCH_COOLDOWN_TICKS;
        }

        if (hasPlayerNearby != hasTargetLastTick) {
            hasTargetLastTick = hasPlayerNearby;
        }
        lastOffhandItem = currentOffhand.copy();
    }


    private void switchToGoldenApple() {
        ItemStack currentOffhand = mc.player.getOffhandItem();
        if (isGoldenApple(currentOffhand)) {
            return;
        }
        int bestSlot = -1;
        boolean isEnchanted = false;
        int bestCount = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    bestSlot = i;
                    isEnchanted = true;
                    bestCount = stack.getCount();
                    break;
                } else if (stack.getItem() == Items.GOLDEN_APPLE) {
                    if (!isEnchanted && stack.getCount() > bestCount) {
                        bestSlot = i;
                        bestCount = stack.getCount();
                    }
                }
            }
        }

        if (bestSlot != -1) {
            swapItems(bestSlot, 40);
        }
    }

    @EventTarget
        public void sendC0BPacket(EventPacket event) {
        //By L1ngG3
        //TODO: 林妍璃我啊，可以最强skid大蛇
        if (event.getType() != EventType.SEND || event.isCancelled() || mc.player == null) {
            return;
        }

        Object packet = event.getPacket();

        //By L1ngG3
        if (packet instanceof ServerboundPlayerCommandPacket) {
            return;
        }
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
    }

    private void switchToBestThrowable() {
        ItemStack currentOffhand = mc.player.getOffhandItem();
        if (isThrowableItem(currentOffhand)) {
            return;
        }
        int bestSlot = -1;
        int bestCount = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (!stack.isEmpty()) {
                String itemName = stack.getItem().toString().toLowerCase();
                for (String throwable : THROWABLE_ITEMS) {
                    if (itemName.contains(throwable)) {
                        if (stack.getCount() > bestCount) {
                            bestSlot = i;
                            bestCount = stack.getCount();
                        }
                        break;
                    }
                }
            }
        }

        if (bestSlot != -1) {
            swapItems(bestSlot, 40);
        }
    }

    private boolean isGoldenApple(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Items.GOLDEN_APPLE ||
                stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private boolean isThrowableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String itemName = stack.getItem().toString().toLowerCase();
        for (String throwable : THROWABLE_ITEMS) {
            if (itemName.contains(throwable)) {
                return true;
            }
        }
        return false;
    }

    private void swapItems(int slot1, int slot2) {
        if (mc.player == null || mc.getConnection() == null) return;
        int containerId = mc.player.containerMenu.containerId;
        int stateId = mc.player.containerMenu.getStateId();
        int networkSlot = convertToNetworkSlot(slot1);
        Int2ObjectOpenHashMap<ItemStack> changedSlots =
                new Int2ObjectOpenHashMap<>();
        ServerboundContainerClickPacket clickPacket = new ServerboundContainerClickPacket(
                containerId,
                stateId,
                networkSlot,
                40,
                ClickType.SWAP,
                ItemStack.EMPTY,
                changedSlots
        );

        mc.getConnection().send(clickPacket);

        ItemStack itemToMove = getItemInSlot(slot1);
        if (slot2 == 40 && !itemToMove.isEmpty()) {
            mc.player.setItemInHand(InteractionHand.OFF_HAND, itemToMove.copy());
        }
    }

    private int convertToNetworkSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            return slot + 36;
        }
        else if (slot >= 9 && slot < 36) {
            return slot;
        }
        else if (slot == 40) {
            return 45;
        }
        return slot;
    }

    private ItemStack getItemInSlot(int slot) {
        if (slot == 40) {
            return mc.player.getOffhandItem();
        } else if (slot >= 0 && slot < 36) {
            return mc.player.getInventory().getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String getSuffix() {
        if (mc.player == null) return "";
        ItemStack offhand = mc.player.getOffhandItem();

        if (offhand.isEmpty()) {
            return "Empty";
        }
        if (isGoldenApple(offhand)) {
            if (offhand.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                return "E-Gapple";
            } else {
                return "Gapple";
            }
        } else if (isThrowableItem(offhand)) {
            String itemName = offhand.getItem().toString().toLowerCase();
            if (itemName.contains("snowball")) {
                return "Snowball";
            } else if (itemName.contains("egg")) {
                return "Egg";
            }
        }
        return "Other";
    }
}