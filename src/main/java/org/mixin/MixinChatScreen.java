package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.Interface;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;

@Mixin(value = ChatScreen.class, priority = 1000)
public abstract class MixinChatScreen {

    @Unique
    private static final SmoothAnimationTimer animation = new SmoothAnimationTimer(0, 0.2f);

    @Unique
    private static long lastMessageTime = 0;

    @Unique
    private static boolean hasNewMessage = false;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void onRenderHead(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Interface interfaceModule = getInterfaceModule();
        if (interfaceModule == null || !interfaceModule.chatScreen.getCurrentValue()) {
            return;
        }

        // 检测新消息的简化逻辑
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        if (chat != null) {
            // 如果有渲染的消息，假设可能有新消息
            if (MinecraftClient.getInstance().inGameHud.getTicks() % 20 == 0) {
                hasNewMessage = true;
                lastMessageTime = System.currentTimeMillis();
                animation.target = 100.0f;
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime > 1500) {
            hasNewMessage = false;
            animation.target = 0.0f;
        }

        animation.update(true);
    }


    @Unique
    private void renderChatBackground(DrawContext guiGraphics) {
        ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
        if (chat == null) return;

        float alpha = Math.max(animation.value / 100.0f, 0.3f); // 最小30%透明度

        int screenHeight = guiGraphics.getScaledWindowHeight();
        float scale = (float) chat.getChatScale();

        // 计算聊天区域
        int chatWidth = MathHelper.floor(chat.getWidth() * scale);
        int chatHeight = MathHelper.floor(chat.getHeight() * scale);
        int chatX = 2;
        int chatY = screenHeight - chatHeight - 40;

        // 类似IslandManager的动画效果
        float animatedWidth = chatWidth;
        float animatedHeight = chatHeight;

        if (hasNewMessage) {
            float pulse = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.05f + 1.0f;
            animatedWidth = chatWidth * pulse;
            animatedHeight = chatHeight * pulse;
        }

        // 绘制背景
        drawChatBackgroundWithEffects(guiGraphics, chatX, chatY, animatedWidth, animatedHeight, alpha);
    }

    @Unique
    private void drawChatBackgroundWithEffects(DrawContext guiGraphics, float x, float y, float width, float height, float alpha) {
        StencilUtils.write(false);

        // 主背景（类似Interface的bodyColor）
        int backgroundColor = new Color(0, 0, 0, (int)(120 * alpha)).getRGB();
        RenderUtils.drawRoundedRect(
                guiGraphics.getMatrices(),
                x,
                y,
                width,
                height,
                8.0f,
                backgroundColor
        );

        // 顶部装饰（类似Interface的headerColor）
        int headerColor = new Color(150, 45, 45, (int)(255 * alpha)).getRGB();
        RenderUtils.fillBound(
                guiGraphics.getMatrices(),
                x,
                y,
                width,
                4.0f,
                headerColor
        );

        // 模糊效果
        int blurColor = new Color(0, 0, 0, (int)(40 * alpha)).getRGB();
        RenderUtils.fillBound(
                guiGraphics.getMatrices(),
                x,
                y,
                width,
                height,
                blurColor
        );

        StencilUtils.erase(true);

        // 边框
        int borderColor = new Color(150, 45, 45, (int)(180 * alpha)).getRGB();
        RenderUtils.drawRoundedRect(
                guiGraphics.getMatrices(),
                x + 0.5f,
                y + 0.5f,
                width - 1,
                height - 1,
                7.5f,
                borderColor
        );

        StencilUtils.dispose();

        // 新消息特效
        if (hasNewMessage && animation.value > 50.0f) {
            float glowAlpha = (animation.value - 50.0f) / 50.0f * 0.3f;
            int glowColor = new Color(150, 45, 45, (int)(100 * glowAlpha)).getRGB();

            RenderUtils.drawRoundedRect(
                    guiGraphics.getMatrices(),
                    x - 2,
                    y - 2,
                    width + 4,
                    height + 4,
                    10.0f,
                    glowColor
            );
        }
    }

    @Unique
    private Interface getInterfaceModule() {
        try {
            return (Interface) Naven.getInstance().getModuleManager().getModule(Interface.class);
        } catch (Exception e) {
            return null;
        }
    }
}