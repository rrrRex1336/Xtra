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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

@Mixin(value = ChatHud.class, priority = 1000)
public abstract class MixinChatComponent {

    @Unique
    private static final SmoothAnimationTimer animation = new SmoothAnimationTimer(0, 0.2f);

    @Unique
    private static long lastRenderTime = System.currentTimeMillis();

    @Unique
    private static int lastMessageCount = 0;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void onRenderHead(DrawContext guiGraphics, int p_283491_, int p_282406_, int p_283111_, CallbackInfo ci) {
        Interface interfaceModule = getInterfaceModule();
        if (interfaceModule == null || !interfaceModule.chatScreen.getCurrentValue()) {
            return;
        }

        // 更新动画
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRenderTime < 100) { // 最近有渲染，认为是新消息
            animation.target = 100.0f;
        } else {
            animation.target = 0.0f;
        }

        animation.update(true);
        lastRenderTime = currentTime;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void redirectChatFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
        // 检查是否启用聊天优化
        try {
            Interface interfaceModule = (Interface) Naven.getInstance().getModuleManager().getModule(Interface.class);
            if (interfaceModule != null && interfaceModule.chatScreen.getCurrentValue()) {
                // 计算矩形大小
                float width = x2 - x1;
                float height = y2 - y1;

                // 只处理背景填充（根据位置判断）
                if (x1 == -4 && height > 8) { // 聊天行背景
                    // 绘制圆角矩形
                    int backgroundColor = new Color(0, 0, 0, (color >> 24) & 0xFF).getRGB();
                    RenderUtils.drawRoundedRect(
                            instance.getMatrices(),
                            x1,
                            y1,
                            width,
                            height,
                            4.0f,
                            backgroundColor
                    );
                } else {
                    // 其他填充保持原样
                    instance.fill(x1, y1, x2, y2, color);
                }
            } else {
                instance.fill(x1, y1, x2, y2, color);
            }
        } catch (Exception e) {
            instance.fill(x1, y1, x2, y2, color);
        }
    }

    @Unique
    private void drawChatBackground(DrawContext guiGraphics, ChatHud chat) {
        float alpha = Math.max(animation.value / 100.0f, 0.3f);

        int screenWidth = guiGraphics.getScaledWindowWidth();
        int screenHeight = guiGraphics.getScaledWindowHeight();

        // 计算聊天区域（根据源码中的计算）
        int chatWidth = MathHelper.floor(chat.getWidth() * chat.getChatScale());
        int chatHeight = MathHelper.floor(chat.getHeight() * chat.getChatScale());

        // 位置：源码中是屏幕底部，上方留40像素
        int chatX = 2;
        int chatY = screenHeight - chatHeight - 40;

        // 使用模板缓冲区
        StencilUtils.write(false);

        // 绘制圆角矩形背景（类似Interface）
        int backgroundColor = new Color(0, 0, 0, (int)(160 * alpha)).getRGB();
        RenderUtils.drawRoundedRect(
                guiGraphics.getMatrices(),
                chatX,
                chatY,
                chatWidth,
                chatHeight,
                10.0f,
                backgroundColor
        );

        // 顶部装饰条（类似Interface的header）
        int headerColor = new Color(150, 45, 45, (int)(255 * alpha)).getRGB();
        RenderUtils.fillBound(
                guiGraphics.getMatrices(),
                chatX,
                chatY,
                chatWidth,
                4.0f,
                headerColor
        );

        // 模糊效果
        int blurColor = new Color(0, 0, 0, (int)(80 * alpha)).getRGB();
        RenderUtils.fillBound(
                guiGraphics.getMatrices(),
                chatX,
                chatY,
                chatWidth,
                chatHeight,
                blurColor
        );

        StencilUtils.erase(true);

        // 边框
        int borderColor = new Color(150, 45, 45, (int)(180 * alpha)).getRGB();
        RenderUtils.drawRoundedRect(
                guiGraphics.getMatrices(),
                chatX + 0.5f,
                chatY + 0.5f,
                chatWidth - 1,
                chatHeight - 1,
                9.0f,
                borderColor
        );

        StencilUtils.dispose();

        // 新消息动画效果
        if (animation.value > 50.0f) {
            float glowAlpha = (animation.value - 50.0f) / 50.0f * 0.4f;
            int glowColor = new Color(150, 45, 45, (int)(120 * glowAlpha)).getRGB();

            RenderUtils.drawRoundedRect(
                    guiGraphics.getMatrices(),
                    chatX - 3,
                    chatY - 3,
                    chatWidth + 6,
                    chatHeight + 6,
                    13.0f,
                    glowColor
            );
        }
    }

    /**
     * 修改聊天背景的填充颜色为透明，因为我们自定义绘制了
     */
    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                    ordinal = 0
            ),
            index = 4
    )
    private int modifyChatBackgroundColor(int originalColor) {
        Interface interfaceModule = getInterfaceModule();
        if (interfaceModule != null && interfaceModule.chatScreen.getCurrentValue()) {
            return 0; // 设置为透明
        }
        return originalColor;
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
