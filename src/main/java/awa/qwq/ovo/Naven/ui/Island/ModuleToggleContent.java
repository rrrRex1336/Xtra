package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.modules.Module;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;

public class ModuleToggleContent implements IslandContent {
    private static class ModuleToggleEntry {
        final Module module;
        long toggleTime;
        boolean isEnabled;
        final SmoothAnimationTimer toggleAnimation;
        final SmoothAnimationTimer progressAnimation; // 进度条动画
        final SmoothAnimationTimer colorAnimation; // 颜色渐变动画

        ModuleToggleEntry(Module module, long toggleTime, boolean isEnabled) {
            this.module = module;
            this.toggleTime = toggleTime;
            this.isEnabled = isEnabled;
            // 开关动画
            this.toggleAnimation = new SmoothAnimationTimer(isEnabled ? 1.0f : 0.0f, 0.0f, 0.3f);
            // 进度条动画 - 快速重置
            this.progressAnimation = new SmoothAnimationTimer(1.0f, 1.0f, 0.3f); // 增加速度到0.3f
            // 颜色动画
            this.colorAnimation = new SmoothAnimationTimer(1.0f, 0.0f, 0.5f); // 快速颜色过渡
        }

        void updateToggleState(boolean newState, long newTime) {
            boolean stateChanged = this.isEnabled != newState;
            this.isEnabled = newState;
            this.toggleTime = newTime;
            this.toggleAnimation.target = newState ? 1.0f : 0.0f;

            if (stateChanged) {
                // 状态改变时，快速重置进度条并开始颜色动画
                this.progressAnimation.value = 1.0f;
                this.progressAnimation.target = 1.0f;
                this.colorAnimation.value = 0.0f;
                this.colorAnimation.target = 1.0f;
            } else {
                // 同一状态被点击，快速重置进度条
                this.progressAnimation.value = 1.0f;
                this.progressAnimation.target = 1.0f;
                this.toggleTime = newTime; // 更新时间
            }
        }

        void updateAnimations() {
            // 更新开关动画
            toggleAnimation.update(true);

            // 更新颜色动画
            colorAnimation.update(true);

            // 计算目标进度（基于剩余时间）
            long currentTime = System.currentTimeMillis();
            float timeElapsed = currentTime - toggleTime;
            float targetProgress = 1.0f - Math.min(timeElapsed / (float) DISPLAY_DURATION, 1.0f);

            // 更新进度条动画
            progressAnimation.target = targetProgress;
            progressAnimation.update(true);
        }

        float getAnimatedProgress() {
            return progressAnimation.value;
        }

        float getColorAnimationValue() {
            return colorAnimation.value;
        }
    }

    private final List<ModuleToggleEntry> toggleEntries = new ArrayList<>();
    private static final long DISPLAY_DURATION = 1500;
    private static final float PADDING = 6f;
    private static final float TOGGLE_WIDTH = 28f;
    private static final float TOGGLE_HEIGHT = 14f;
    private static final float ENTRY_SPACING = 4f;
    private static final float ICON_SIZE = 35f;
    private static final float TEXT_SPACING = 5f;

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean shouldDisplay() {
        cleanupExpiredEntries();
        return !toggleEntries.isEmpty();
    }

    public void onModuleToggled(Module module) {
        long currentTime = System.currentTimeMillis();
        boolean newState = module.isEnabled();

        ModuleToggleEntry existingEntry = null;
        for (ModuleToggleEntry entry : toggleEntries) {
            if (entry.module == module) {
                existingEntry = entry;
                break;
            }
        }

        if (existingEntry != null) {
            existingEntry.updateToggleState(newState, currentTime);
        } else {
            toggleEntries.add(0, new ModuleToggleEntry(module, currentTime, newState));
        }
    }

    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        toggleEntries.removeIf(entry -> (currentTime - entry.toggleTime) >= DISPLAY_DURATION);
    }

    @Override
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        cleanupExpiredEntries();

        if (toggleEntries.isEmpty()) {
            return;
        }

        float currentY = y + PADDING;

        for (ModuleToggleEntry entry : toggleEntries) {
            entry.updateAnimations();

            float animValue = entry.toggleAnimation.value;
            float colorAnimValue = entry.getColorAnimationValue();

            // 图标背景位置
            float iconBgX = x + PADDING;
            float iconBgY = currentY;

            // 绘制图标背景（圆角正方形）
            RenderUtils.drawRoundedRect(stack, iconBgX, iconBgY, ICON_SIZE, ICON_SIZE, 5f, new Color(40, 40, 40, 200).getRGB());

            // 在图标背景中绘制开关
            float toggleX = iconBgX + (ICON_SIZE - TOGGLE_WIDTH) / 2f;
            float toggleY = iconBgY + (ICON_SIZE - TOGGLE_HEIGHT) / 2f;

            // 开关背景
            Color toggleBgColor = new Color(30, 30, 30, 255);
            RenderUtils.drawRoundedRect(stack, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2f, toggleBgColor.getRGB());

            // 开关内圆位置计算
            float circleSize = 10f;
            float circleY = toggleY + (TOGGLE_HEIGHT - circleSize) / 2f;
            float leftPos = toggleX + 2f;
            float rightPos = toggleX + TOGGLE_WIDTH - circleSize - 2f;
            float circleX = leftPos + (rightPos - leftPos) * animValue;

            // 开关圆点阴影
            RenderUtils.drawRoundedRect(stack, circleX + 0.5f, circleY + 0.5f, circleSize, circleSize, circleSize / 2f, new Color(0, 0, 0, 80).getRGB());

            // 开关圆点（颜色平滑过渡）
            Color enabledCircleColor = new Color(100, 150, 255, 255);
            Color disabledCircleColor = new Color(150, 150, 150, 255);

            // 使用颜色动画值来平滑过渡
            float colorMix = colorAnimValue;
            if (!entry.isEnabled) {
                colorMix = 1.0f - colorAnimValue;
            }

            int circleR = (int) (disabledCircleColor.getRed() + (enabledCircleColor.getRed() - disabledCircleColor.getRed()) * colorMix);
            int circleG = (int) (disabledCircleColor.getGreen() + (enabledCircleColor.getGreen() - disabledCircleColor.getGreen()) * colorMix);
            int circleB = (int) (disabledCircleColor.getBlue() + (enabledCircleColor.getBlue() - disabledCircleColor.getBlue()) * colorMix);
            Color circleColor = new Color(circleR, circleG, circleB, 255);

            RenderUtils.drawRoundedRect(stack, circleX, circleY, circleSize, circleSize, circleSize / 2f, circleColor.getRGB());

            // 文本位置
            float textX = iconBgX + ICON_SIZE + TEXT_SPACING;

            // 模块名
            Color titleColor = entry.isEnabled ?
                    new Color(100, 150, 255, 255) :
                    new Color(200, 200, 200, 255);
            Fonts.opensans.render(stack, "Module Toggled", textX, currentY, titleColor, true, 0.4f);

            // 状态文本
            String statusText = entry.isEnabled ?
                    Formatting.DARK_AQUA + entry.module.getName() + Formatting.WHITE + " has been" + Formatting.GREEN + " Enabled" + Formatting.WHITE + "!" :
                    Formatting.DARK_AQUA + entry.module.getName() + Formatting.WHITE + " has been" + Formatting.RED + " Disabled" + Formatting.WHITE + "!";

            float titleHeight = (float) Fonts.opensans.getHeight(true, 0.4f);
            float statusY = currentY + titleHeight + 4;

            Fonts.opensans.render(stack, statusText, textX, statusY, new Color(-1), true, 0.3f);

            // 进度条
            float contentHeight = titleHeight + 4 + (float) Fonts.opensans.getHeight(true, 0.3f);
            float progressBarY = currentY + contentHeight + 8;
            float progressBarWidth = getEntryWidth(entry) - PADDING * 2;
            float progressBarHeight = 6f;
            float progressBarRadius = 3f;

            // 进度条背景
            RenderUtils.drawRoundedRect(stack, x + PADDING, progressBarY, progressBarWidth, progressBarHeight, progressBarRadius, new Color(30, 30, 30, 200).getRGB());

            // 进度条填充
            float progressValue = entry.getAnimatedProgress();
            if (progressValue > 0) {
                float progressFillWidth = progressBarWidth * progressValue;

                // 进度条颜色（根据是否启用和颜色动画）
                Color progressColor;
                if (entry.isEnabled) {
                    // 启用时蓝色，使用颜色动画平滑过渡
                    int alpha = (int) (255 * progressValue);
                    float intensity = 0.7f + 0.3f * colorAnimValue;
                    int r = (int)(100 * intensity);
                    int g = (int)(150 * intensity);
                    int b = 255;
                    progressColor = new Color(r, g, b, alpha);
                } else {
                    // 禁用时红色，使用颜色动画平滑过渡
                    int alpha = (int) (255 * progressValue);
                    float intensity = 0.7f + 0.3f * colorAnimValue;
                    int r = 255;
                    int g = (int)(100 * intensity);
                    int b = (int)(100 * intensity);
                    progressColor = new Color(r, g, b, alpha);
                }

                RenderUtils.drawRoundedRect(stack, x + PADDING, progressBarY, progressFillWidth, progressBarHeight, progressBarRadius, progressColor.getRGB());
            }

            currentY += getEntryHeight() + ENTRY_SPACING;
        }
    }

    private float getEntryWidth(ModuleToggleEntry entry) {
        float padding = PADDING;
        float iconSize = ICON_SIZE;
        float textSpacing = TEXT_SPACING;

        float moduleNameWidth = Fonts.opensans.getWidth("Module Toggled", 0.4f);

        String statusText = entry.isEnabled ?
                entry.module.getName() + " has been Enabled!" :
                entry.module.getName() + " has been Disabled!";
        float statusWidth = Fonts.opensans.getWidth(statusText, 0.3f);

        float textWidth = Math.max(moduleNameWidth, statusWidth);

        return padding + iconSize + textSpacing + textWidth + padding;
    }

    private float getEntryHeight() {
        float padding = PADDING;
        float titleHeight = (float) Fonts.opensans.getHeight(true, 0.4f);
        float statusHeight = (float) Fonts.opensans.getHeight(true, 0.3f);
        float progressBarHeight = 6f;
        float spacing = 4f + 8f;

        return padding + titleHeight + spacing + statusHeight + progressBarHeight + padding;
    }

    @Override
    public float getWidth() {
        cleanupExpiredEntries();

        if (toggleEntries.isEmpty()) {
            return 200;
        }

        float maxWidth = 0;
        for (ModuleToggleEntry entry : toggleEntries) {
            maxWidth = Math.max(maxWidth, getEntryWidth(entry));
        }

        return maxWidth;
    }

    @Override
    public float getHeight() {
        cleanupExpiredEntries();

        if (toggleEntries.isEmpty()) {
            return 40;
        }

        float totalHeight = 0;
        for (int i = 0; i < toggleEntries.size(); i++) {
            totalHeight += getEntryHeight();
            if (i < toggleEntries.size() - 1) {
                totalHeight += ENTRY_SPACING;
            }
        }

        return totalHeight;
    }
}