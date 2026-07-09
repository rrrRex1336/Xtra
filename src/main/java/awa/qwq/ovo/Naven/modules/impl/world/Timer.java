package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventKey;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.MoveUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
        name = "Timer",
        description = "Balance Timer",
        category = Category.PLAYER
)
public class Timer extends Module {

    private final FloatValue speed = ValueBuilder.create(this, "Speed")
            .setDefaultFloatValue(2.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(5.0f)
            .build()
            .getFloatValue();

    public BooleanValue pulse = ValueBuilder.create(this, "Pulse")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    // Sakura 原版参数
    private static final double CHARGE_SPEED = 0.9;
    private static final double CHARGE_TIME = 3.4;
    private static final double PULSE_DURATION = 1.1;
    private static final double NORMAL_DURATION = 0.58;

    private boolean active;
    private double progress;
    private long lastUpdateNs;
    private boolean mouseHeld;

    @Override
    public void onEnable() {
        active = false;
        progress = 0.0;
        mouseHeld = false;
        lastUpdateNs = System.nanoTime();
    }

    @Override
    public void onDisable() {
        active = false;
        progress = 0.0;
        mouseHeld = false;
        Naven.TICK_TIMER = 1.0f;
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (event.getKey() == GLFW.GLFW_KEY_TAB) {
            if (event.isState()) {
                mouseHeld = true;
                if (progress > 0) {
                    active = true;
                }
            } else {
                mouseHeld = false;
                active = false;
            }
        }
    }

    @EventTarget
    public void onPostTick(EventRunTicks e) {
        if (e.getType() != EventType.POST) return;
        if (mc.player == null || mc.world == null) return;

        long now = System.nanoTime();
        double dt = (now - lastUpdateNs) / 1_000_000_000.0;
        lastUpdateNs = now;

        if (!mouseHeld && active) {
            active = false;
        }

        if (active) {
            // 加速阶段：消耗能量
            double duration = pulse.getCurrentValue() ? PULSE_DURATION : NORMAL_DURATION;
            double d = Math.max(0.1, duration);
            progress -= dt / d;
            if (progress <= 0.0) {
                progress = 0.0;
                active = false;
            }
        } else {
            // 充能阶段：Sakura 原版逻辑，moveCharge 默认开启，静止或移动都能充能
            if (!mouseHeld) {
                double d = Math.max(0.1, CHARGE_TIME);
                progress += dt / d;
                if (progress > 1.0) {
                    progress = 1.0;
                }
            }
        }
    }

    @EventTarget
    public void onPreTick(EventRunTicks e) {
        if (e.getType() != EventType.PRE) return;
        if (mc.player == null) return;

        if (active && progress > 0) {
            float currentSpeed = getTimerSpeed();
            Naven.TICK_TIMER = 1F / currentSpeed;
        } else {
            // 没加速时恢复
            Naven.TICK_TIMER = 1.0f;
        }
    }

    private float getTimerSpeed() {
        if (!isEnabled()) return 1.0f;

        if (active && progress > 0) {
            if (pulse.getCurrentValue()) {
                int tick = mc.player.age % 4;
                if (tick == 0) {
                    return 0.8f;
                }
            }
            return speed.getCurrentValue();
        }

        // 充能时微微减速（Sakura 原版 CHARGE_SPEED = 0.9）
        if (progress < 1.0) {
            return (float) CHARGE_SPEED;
        }

        return 1.0f;
    }

    public boolean isActive() {
        return active;
    }

    public double getProgress() {
        return progress;
    }
}