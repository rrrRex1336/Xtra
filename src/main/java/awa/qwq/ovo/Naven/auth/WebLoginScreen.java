package awa.qwq.ovo.Naven.auth;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.net.URI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class WebLoginScreen extends Screen {
    private final String loginUrl;
    private final Screen previousScreen;
    private ButtonWidget openBrowserButton;
    private ButtonWidget copyUrlButton;
    private ButtonWidget exitButton;
    private int tickCount = 0;

    public WebLoginScreen(String loginUrl, Screen previousScreen) {
        super(Text.literal(VerifyClient.CLIENT_DISPLAY_NAME + "-LinYanLi 网页登录"));
        this.loginUrl = loginUrl;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 打开浏览器按钮
        this.openBrowserButton = ButtonWidget.builder(
                Text.literal("打开浏览器进行登录"),
                button -> openBrowser()
        ).dimensions(centerX - 155, centerY + 20, 310, 20).build();

        // 复制URL按钮
        this.copyUrlButton = ButtonWidget.builder(
                Text.literal("复制登录链接"),
                button -> copyToClipboard()
        ).dimensions(centerX - 155, centerY + 45, 150, 20).build();

        // 退出按钮
        this.exitButton = ButtonWidget.builder(
                Text.literal("退出游戏"),
                button -> client.scheduleStop()
        ).dimensions(centerX + 5, centerY + 45, 150, 20).build();

        this.addDrawableChild(this.openBrowserButton);
        this.addDrawableChild(this.copyUrlButton);
        this.addDrawableChild(this.exitButton);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 标题
        guiGraphics.drawCenteredTextWithShadow(this.textRenderer, VerifyClient.CLIENT_DISPLAY_NAME + " 需要网页登录验证",
                centerX, centerY - 80, 0xFFFFFF);

        // 闪烁的提示文字
        if ((tickCount / 10) % 2 == 0) {
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, "━━━━━━━━━━━━━━━━━━━━━━",
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
            guiGraphics.drawCenteredTextWithShadow(this.textRenderer, line, centerX, yOffset, 0xCCCCCC);
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
        guiGraphics.drawTextWithShadow(this.textRenderer, urlLabel, centerX - 150, urlBoxY + 5, 0xAAAA00);

        // 分行显示URL
        String displayUrl = loginUrl.length() > 60 ?
                loginUrl.substring(0, 60) + "..." : loginUrl;
        guiGraphics.drawTextWithShadow(this.textRenderer, displayUrl, centerX - 150, urlBoxY + 17, 0x55FF55);

        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 底部提示
        guiGraphics.drawCenteredTextWithShadow(this.textRenderer,
                "登录完成后请重启游戏 | 按 ESC 关闭此窗口",
                centerX, this.height - 20, 0x888888);
    }

    private void openBrowser() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(loginUrl));
                // 显示成功提示
                if (client != null && client.player != null) {
                    client.player.sendMessage(
                            Text.literal("§a已在浏览器中打开登录页面"),
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
            client.keyboard.setClipboard(loginUrl);
            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal("§a登录链接已复制到剪贴板"),
                        false
                );
            }
            // 修改按钮文字
            this.copyUrlButton.setMessage(Text.literal("已复制!"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // 按ESC关闭，但不返回上一个界面（因为需要登录）
            this.client.scheduleStop();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // 防止意外关闭
    }

    @Override
    public void close() {
        // 不允许简单关闭，必须完成登录或退出游戏
        this.client.scheduleStop();
    }
}
