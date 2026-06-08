package awa.qwq.ovo.Naven.modules.impl.Player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    @EventTarget
    public void onMouseClick(EventMouseClick event) {
        int triggerKey = 2;

        if (this.keyMode.isCurrentMode("Mouse4")) {
            triggerKey = 3;
        } else if (this.keyMode.isCurrentMode("Mouse5")) {
            triggerKey = 4;
        }

        if (event.getKey() == triggerKey && !event.isState()
                && MiddlePearl.mc.player != null && MiddlePearl.mc.gameMode != null) {

            int pearlSlot = this.findPearlSlot();
            if (pearlSlot == -1) {
                ChatUtils.addChatMessage("§cPearl Not Found in hotbar!");
                return;
            }

            if (this.mode.isCurrentMode("Fast Switch")) {
                int finalOriginalSlot = this.originalSlot = MiddlePearl.mc.player.getInventory().selected;

                new Thread(() -> {
                    try {
                        Thread.sleep((long) this.switchToDelay.getCurrentValue());
                        if (MiddlePearl.mc.player != null && MiddlePearl.mc.gameMode != null) {
                            MiddlePearl.mc.player.getInventory().selected = pearlSlot;
                            MiddlePearl.mc.gameMode.useItem(MiddlePearl.mc.player, InteractionHand.MAIN_HAND);
                        }

                        Thread.sleep((long) this.switchBackDelay.getCurrentValue());
                        if (MiddlePearl.mc.player != null) {
                            MiddlePearl.mc.player.getInventory().selected = finalOriginalSlot;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            } else if (this.mode.isCurrentMode("Spoof")) {
                this.originalSlot = MiddlePearl.mc.player.getInventory().selected;
                MiddlePearl.mc.player.getInventory().selected = pearlSlot;
                MiddlePearl.mc.gameMode.useItem(MiddlePearl.mc.player, InteractionHand.MAIN_HAND);
                MiddlePearl.mc.player.getInventory().selected = this.originalSlot;
            }
        }
    }

    private int findPearlSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = MiddlePearl.mc.player.getInventory().getItem(i);
            if (stack.getItem() != Items.ENDER_PEARL || stack.getCount() <= 0) {
                continue;
            }
            return i;
        }
        return -1;
    }
}