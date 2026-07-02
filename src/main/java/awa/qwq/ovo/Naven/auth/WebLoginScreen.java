package awa.qwq.ovo.Naven.auth;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.net.URI;

public class WebLoginScreen extends Screen {
    private final String loginUrl;
    private final Screen previousScreen;
    private Button openBrowserButton;
    private Button copyUrlButton;
    private Button exitButton;
    private int tickCount = 0;

    public WebLoginScreen(String loginUrl, Screen previousScreen) {
        super(Component.literal("LinYiLI 网页登录"));
        this.loginUrl = loginUrl;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 打开浏览器按钮
        this.openBrowserButton = Button.builder(
                Component.literal("打开浏览器进行登录"),
                button -> openBrowser()
        ).bounds(centerX - 155, centerY + 20, 310, 20).build();

        // 复制URL按钮
        this.copyUrlButton = Button.builder(
                Component.literal("复制登录链接"),
                button -> copyToClipboard()
        ).bounds(centerX - 155, centerY + 45, 150, 20).build();

        // 退出按钮
        this.exitButton = Button.builder(
                Component.literal("退出游戏"),
                button -> minecraft.stop()
        ).bounds(centerX + 5, centerY + 45, 150, 20).build();

        this.addRenderableWidget(this.openBrowserButton);
        this.addRenderableWidget(this.copyUrlButton);
        this.addRenderableWidget(this.exitButton);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 标题
        guiGraphics.drawCenteredString(this.font, "LinYiLI 需要网页登录验证",
                centerX, centerY - 80, 0xFFFFFF);

        // 闪烁的提示文字
        if ((tickCount / 10) % 2 == 0) {
            guiGraphics.drawCenteredString(this.font, "━━━━━━━━━━━━━━━━━━━━━━",
                    centerX, centerY - 60, 0xFF5555);
        }

        // 说明文字
        String[] instructions = {
            "请按照以下步骤完成验证：",
            "",
            "1. 点击下方按钮打开浏览器",
            "2. 在网页中完成登录确认",
            "3. 返回并重启游戏"
        };

        int yOffset = centerY - 40;
        for (String line : instructions) {
            guiGraphics.drawCenteredString(this.font, line, centerX, yOffset, 0xCCCCCC);
            yOffset += 12;
        }

        // URL显示区域（使用较小的字体）
        int urlBoxY = centerY + 75;
        int urlBoxHeight = 30;

        // 绘制URL背景框
        guiGraphics.fill(centerX - 155, urlBoxY, centerX + 155, urlBoxY + urlBoxHeight, 0x88000000);
        guiGraphics.fill(centerX - 155, urlBoxY, centerX + 155, urlBoxY + 1, 0xFF444444);
        guiGraphics.fill(centerX - 155, urlBoxY + urlBoxHeight - 1, centerX + 155, urlBoxY + urlBoxHeight, 0xFF444444);
        guiGraphics.fill(centerX - 155, urlBoxY, centerX - 154, urlBoxY + urlBoxHeight, 0xFF444444);
        guiGraphics.fill(centerX + 154, urlBoxY, centerX + 155, urlBoxY + urlBoxHeight, 0xFF444444);

        // 显示URL（自动换行）
        String urlLabel = "登录地址: ";
        guiGraphics.drawString(this.font, urlLabel, centerX - 150, urlBoxY + 5, 0xAAAA00);

        // 分行显示URL
        String displayUrl = loginUrl.length() > 60 ?
                loginUrl.substring(0, 60) + "..." : loginUrl;
        guiGraphics.drawString(this.font, displayUrl, centerX - 150, urlBoxY + 17, 0x55FF55);

        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 底部提示
        guiGraphics.drawCenteredString(this.font,
                "登录完成后请重启游戏 | 按 ESC 关闭此窗口",
                centerX, this.height - 20, 0x888888);
    }

    private void openBrowser() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(loginUrl));
                // 显示成功提示
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§a已在浏览器中打开登录页面"),
                            false
                    );
                }
            } else {
                copyToClipboard();
            }
        } catch (Exception e) {
            e.printStackTrace();
            copyToClipboard();
        }
    }

    private void copyToClipboard() {
        try {
            minecraft.keyboardHandler.setClipboard(loginUrl);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§a登录链接已复制到剪贴板"),
                        false
                );
            }
            // 修改按钮文字
            this.copyUrlButton.setMessage(Component.literal("已复制!"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // 按ESC关闭，但不返回上一个界面（因为需要登录）
            this.minecraft.stop();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // 防止意外关闭
    }

    @Override
    public void onClose() {
        // 不允许简单关闭，必须完成登录或退出游戏
        this.minecraft.stop();
    }
}
