package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.ChatUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "AutoPlay", description = "Automatically joins the next game after a delay.", category = Category.MISC)
public class AutoPlay extends Module {

    private final FloatValue delay = ValueBuilder.create(this, "Delay (Seconds)")
            .setDefaultFloatValue(2.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(0.1f)
            .build()
            .getFloatValue();

    private long scheduledTime = 0L;
    private long waitStartTime = 0L;
    private long totalWaitTime = 0L;

    @EventTarget
    public void onPacket(EventPacket event) {
        if (AutoPlay.mc.player == null || !this.isEnabled()) {
            return;
        }

        if (event.getPacket() instanceof GameMessageS2CPacket) {
            String message = ((GameMessageS2CPacket) event.getPacket()).content().getString();
            if (message.contains("游戏结束，请对")) {
                long delayMillis = (long) (this.delay.getCurrentValue() * 1000.0f);
                this.scheduledTime = System.currentTimeMillis() + delayMillis;
                this.waitStartTime = System.currentTimeMillis();
                this.totalWaitTime = delayMillis;
            }
            if (message.contains("正在为您匹配可用的游戏服务器") && this.scheduledTime > 0L) {
                this.resetTimer();
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (AutoPlay.mc.player == null || !this.isEnabled()) {
            return;
        }

        if (this.scheduledTime > 0L && System.currentTimeMillis() >= this.scheduledTime) {
            AutoPlay.mc.player.networkHandler.sendChatCommand("again");
            ChatUtils.addChatMessage("§b[AutoPlay] §fEntering the next game.");
            this.resetTimer();
        }
    }

    private void resetTimer() {
        this.scheduledTime = 0L;
        this.waitStartTime = 0L;
        this.totalWaitTime = 0L;
    }

    @Override
    public void onEnable() {
        this.resetTimer();
    }

    @Override
    public void onDisable() {
        this.resetTimer();
    }

    public boolean isWaiting() {
        return this.scheduledTime > 0L;
    }

    public float getProgress() {
        if (!this.isWaiting() || this.totalWaitTime == 0L) {
            return 0.0f;
        }
        long elapsedTime = System.currentTimeMillis() - this.waitStartTime;
        return Math.min(1.0f, (float) elapsedTime / (float) this.totalWaitTime);
    }

    public float getRemainingSeconds() {
        if (!this.isWaiting()) {
            return 0.0f;
        }
        long remainingMillis = this.scheduledTime - System.currentTimeMillis();
        return Math.max(0.0f, (float) remainingMillis / 1000.0f);
    }
}