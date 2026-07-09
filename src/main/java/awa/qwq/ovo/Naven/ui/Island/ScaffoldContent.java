package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.world.Scaffold;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.*;
import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class ScaffoldContent implements IslandContent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private double bps = 0.0;
    private final SmoothAnimationTimer progressAnimation = new SmoothAnimationTimer(0.0f, 0.3f);
    private String currentTitle = "Scaffold Toggled";

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean shouldDisplay() {
        Scaffold scaffold = getScaffoldModule();
        return scaffold != null && scaffold.isEnabled();
    }

    private Scaffold getScaffoldModule() {
        try {
            return (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        } catch (Exception e) {
            return null;
        }
    }

    private int getBlockCount() {
        Scaffold scaffold = getScaffoldModule();
        if (scaffold != null) {
            return scaffold.getBlockCount();
        }
        return 0;
    }

    private boolean isClutching() {
        Scaffold scaffold = getScaffoldModule();
        return false;
    }

    private ItemStack getCurrentBlockItem() {
        if (mc.player != null) {
            ItemStack heldItem = mc.player.getMainHandStack();
            if (heldItem.getItem() instanceof BlockItem) {
                return heldItem;
            }
        }
        return ItemStack.EMPTY;
    }

    private double speedCalculator() {
        if (mc.player != null) {
            PlayerEntity player = mc.player;

            double deltaX = player.getX() - player.prevX;
            double deltaZ = player.getZ() - player.prevZ;

            double timerSpeed = 1.0;
            try {
                Field timerField = MinecraftClient.class.getDeclaredField("timer");
                timerField.setAccessible(true);
                Object timer = timerField.get(mc);
                Field speedField = timer.getClass().getDeclaredField("timerSpeed");
                speedField.setAccessible(true);
                timerSpeed = speedField.getFloat(timer);
            } catch (Exception ignored) {
            }

            double bps = Math.hypot(deltaX, deltaZ) * timerSpeed * 20.0;

            return Math.round(bps * 100.0) / 100.0;
        } else {
            return 0.00;
        }
    }

    private void updateBps() {
        bps = speedCalculator();
        if (isClutching()) {
            currentTitle = "Clutching!!!";
        } else {
            currentTitle = "Scaffold Toggled";
        }
    }

    @Override
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        if (!shouldDisplay()) {
            return;
        }

        updateBps();

        float padding = 6f;
        float titleScale = 0.4f;
        float subtitleScale = 0.35f;
        float iconSize = 35f;
        float iconPadding = 5f;
        float iconBgX = x + padding;
        float iconBgY = y + padding;
        RenderUtils.drawRoundedRect(stack, iconBgX, iconBgY, iconSize, iconSize, 5f, new Color(40, 40, 40, 200).getRGB());
        ItemStack currentBlock = getCurrentBlockItem();

        if (!currentBlock.isEmpty()) {
            RenderSystem.enableDepthTest();
            int iconX = (int)(iconBgX + (iconSize - 16) / 2);
            int iconY = (int)(iconBgY + (iconSize - 16) / 2);
            graphics.drawItem(currentBlock, iconX, iconY);
            graphics.drawItemInSlot(mc.textRenderer, currentBlock, iconX, iconY);

            RenderSystem.disableDepthTest();
            if (isClutching()) {
                // 卡住时添加红色边框 - 使用四个矩形来模拟边框
                int borderColor = new Color(255, 100, 100, 180).getRGB();
                float borderWidth = 2f;

                // 上边框
                RenderUtils.drawRoundedRect(stack,
                        iconBgX,
                        iconBgY,
                        iconSize,
                        borderWidth,
                        0,
                        borderColor);

                // 下边框
                RenderUtils.drawRoundedRect(stack,
                        iconBgX,
                        iconBgY + iconSize - borderWidth,
                        iconSize,
                        borderWidth,
                        0,
                        borderColor);

                // 左边框
                RenderUtils.drawRoundedRect(stack,
                        iconBgX,
                        iconBgY + borderWidth,
                        borderWidth,
                        iconSize - 2 * borderWidth,
                        0,
                        borderColor);

                // 右边框
                RenderUtils.drawRoundedRect(stack,
                        iconBgX + iconSize - borderWidth,
                        iconBgY + borderWidth,
                        borderWidth,
                        iconSize - 2 * borderWidth,
                        0,
                        borderColor);
            }
        } else {
            // 如果没有手持方块，显示一个占位符图标
            int placeholderColor = isClutching() ?
                    new Color(255, 100, 100, 100).getRGB() :
                    new Color(100, 150, 255, 100).getRGB();

            // 绘制一个简单的方块图标
            float blockSize = 20f;
            float blockX = iconBgX + (iconSize - blockSize) / 2;
            float blockY = iconBgY + (iconSize - blockSize) / 2;

            // 绘制方块背景
            RenderUtils.drawRoundedRect(stack, blockX, blockY, blockSize, blockSize, 3f, placeholderColor);

            // 绘制方块轮廓 - 使用四个矩形来模拟边框
            int outlineColor = isClutching() ?
                    new Color(255, 100, 100, 200).getRGB() :
                    new Color(100, 150, 255, 200).getRGB();
            float outlineWidth = 1.5f;

            // 上边框
            RenderUtils.drawRoundedRect(stack,
                    blockX,
                    blockY,
                    blockSize,
                    outlineWidth,
                    0,
                    outlineColor);

            // 下边框
            RenderUtils.drawRoundedRect(stack,
                    blockX,
                    blockY + blockSize - outlineWidth,
                    blockSize,
                    outlineWidth,
                    0,
                    outlineColor);

            // 左边框
            RenderUtils.drawRoundedRect(stack,
                    blockX,
                    blockY + outlineWidth,
                    outlineWidth,
                    blockSize - 2 * outlineWidth,
                    0,
                    outlineColor);

            // 右边框
            RenderUtils.drawRoundedRect(stack,
                    blockX + blockSize - outlineWidth,
                    blockY + outlineWidth,
                    outlineWidth,
                    blockSize - 2 * outlineWidth,
                    0,
                    outlineColor);

            // 绘制问号
            String questionMark = "?";
            float qmScale = 0.8f;
            float qmWidth = Fonts.opensans.getWidth(questionMark, qmScale);
            float qmHeight = (float) Fonts.opensans.getHeight(true, qmScale);
            float qmX = blockX + blockSize/2 - qmWidth/2;
            float qmY = blockY + blockSize/2 - qmHeight/2;
            Fonts.opensans.render(stack, questionMark, qmX, qmY, Color.WHITE, true, qmScale);
        }

        // 文本位置（在图标右边）
        float textX = iconBgX + iconSize + iconPadding;
        float titleY = y + padding;

        // 根据是否卡住选择颜色
        Color titleColor = isClutching() ? new Color(255, 100, 100) : Color.WHITE;
        Fonts.opensans.render(stack, currentTitle, textX, titleY, titleColor, true, titleScale);

        int blockCount = getBlockCount();
        String subtitle = blockCount + " blocks left - " + String.format("%.2f", bps) + " blocks/s";
        float titleHeight = (float) Fonts.opensans.getHeight(true, titleScale);
        float subtitleY = titleY + titleHeight + 4;
        Fonts.opensans.render(stack, subtitle, textX, subtitleY, new Color(200, 200, 200, 255), true, subtitleScale);

        // 进度条在整体下面
        float contentHeight = titleHeight + 4 + (float) Fonts.opensans.getHeight(true, subtitleScale);
        float progressBarY = y + padding + contentHeight + 8;
        float progressBarWidth = getWidth() - padding * 2;
        float progressBarHeight = 6f;
        float progressBarRadius = 3f;

        // 进度条背景（圆角矩形）
        RenderUtils.drawRoundedRect(stack, x + padding, progressBarY, progressBarWidth, progressBarHeight, progressBarRadius, new Color(30, 30, 30, 200).getRGB());

        // 计算目标进度值（0.0 - 1.0）
        float targetProgress = Math.min(blockCount / 100.0f, 1.0f);

        // 更新进度条动画
        progressAnimation.target = targetProgress;
        progressAnimation.speed = 0.05f;
        progressAnimation.update(true);

        // 使用动画值来计算进度条宽度（平滑过渡）
        float animatedProgress = progressAnimation.value;
        float progressFillWidth = progressBarWidth * animatedProgress;

        // 进度条填充（圆角矩形）
        if (progressFillWidth > 0) {
            // 根据是否卡住选择颜色
            Color progressColor = isClutching() ? new Color(255, 100, 100, 255) : new Color(100, 150, 255, 255);
            RenderUtils.drawRoundedRect(stack, x + padding, progressBarY, progressFillWidth, progressBarHeight, progressBarRadius, progressColor.getRGB());
        }
    }

    @Override
    public float getWidth() {
        if (!shouldDisplay()) {
            return 200;
        }

        float padding = 6f;
        float iconSize = 35f;
        float iconPadding = 5f;
        float titleScale = 0.4f;
        float subtitleScale = 0.35f;

        int blockCount = getBlockCount();
        String subtitle = blockCount + " blocks left - " + String.format("%.2f", bps) + " blocks/s";

        float titleWidth = Fonts.opensans.getWidth(currentTitle, titleScale);
        float subtitleWidth = Fonts.opensans.getWidth(subtitle, subtitleScale);
        float textWidth = Math.max(titleWidth, subtitleWidth);

        return padding + iconSize + iconPadding + textWidth + padding;
    }

    @Override
    public float getHeight() {
        if (!shouldDisplay()) {
            return 40;
        }

        float padding = 6f;
        float titleScale = 0.4f;
        float subtitleScale = 0.35f;
        float titleHeight = (float) Fonts.opensans.getHeight(true, titleScale);
        float subtitleHeight = (float) Fonts.opensans.getHeight(true, subtitleScale);
        float progressBarHeight = 6f;
        float spacing = 4f + 8f; // 标题和副标题间距 + 副标题和进度条间距

        return padding + titleHeight + spacing + subtitleHeight + progressBarHeight + padding;
    }
}