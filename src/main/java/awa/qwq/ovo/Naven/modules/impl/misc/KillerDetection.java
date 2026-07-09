package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.events.impl.EventUpdate;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

@ModuleInfo(
        name = "KillerDetection",
        description = "KillerDetection you",
        category = Category.MISC
)

public class KillerDetection extends Module {
    public KillerDetection() {
    }

    private static final Set<String> detectedKillers = new HashSet<>();
    public static Set<String> getDetectedKillers() {
        return detectedKillers;
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) {
                if (player == mc.player) {
                    continue;
                }

                String playerName = player.getName().getString();

                if (detectedKillers.contains(playerName)) {
                    continue;
                }

                ItemStack mainHandItem = player.getMainHandStack();

                if (mainHandItem.getItem() == Items.IRON_SWORD || mainHandItem.getItem() == Items.DIAMOND_SWORD || mainHandItem.getItem() instanceof SwordItem) {
                    ChatUtils.addChatMessage("[KillerDetection] Player " + playerName + " It's a killer!");
                    Notification notification = new Notification(NotificationLevel.ERROR, "The killer is " + playerName, 6000L);
                    Naven.getInstance().getNotificationManager().addNotification(notification);
                    detectedKillers.add(playerName);
                }
            }
        }
    }

    @EventTarget
    private void onRender3D(EventRender2D event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        int screenWidth = mc.getWindow().getFramebufferWidth();
        int screenHeight = mc.getWindow().getFramebufferHeight();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) {
                String playerName = player.getName().getString();
                if (detectedKillers.contains(playerName)) {
                    float size = 50.0f;
                    float x = (screenWidth - size) / 2;
                    float y = (screenHeight - size) / 2;

                    RenderUtils.drawRoundedRect(
                            event.getStack(),
                            x,
                            y,
                            size,
                            size,
                            5.0f,
                            new Color(255, 0, 0, 50).getRGB()
                    );
                }
            }
        }
    }
    @EventTarget
    private void onWorld(EventRespawn event) {
        detectedKillers.clear();
    }
}
