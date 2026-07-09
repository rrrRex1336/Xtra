package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.visual.Island;
import awa.qwq.ovo.Naven.utils.FontIcons;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;

public class IslandManager {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Color CLIENT_NAME_COLOR = new Color(150, 45, 45, 255);
    private static final Color PING_COLOR = new Color(50, 200, 50, 255);
    private static final Color WHITE_COLOR = new Color(255, 255, 255, 255);

    protected static final SmoothAnimationTimer animW = new SmoothAnimationTimer(
            Fonts.opensans.getWidth(Naven.CLIENT_DISPLAY_NAME + " | Shiroko | 999ms to mc.hypixel.net | 999 FPS", 0.4f) + 30,
            0.2f
    );

    protected static final SmoothAnimationTimer animH = new SmoothAnimationTimer(
            (float) Fonts.opensans.getHeight(false, 0.4f) + 10,
            0.2f
    );

    private final SmoothAnimationTimer posX = new SmoothAnimationTimer(0, 0.2f);
    private final SmoothAnimationTimer posY = new SmoothAnimationTimer(0, 0.2f);

    private final List<IslandContent> contents = new ArrayList<>();

    public IslandManager() {
    }

    private Island getIslandModule() {
        try {
            return (Island) Naven.getInstance()
                    .getModuleManager().getModule(Island.class);
        } catch (Exception ignored) {
        }
        return null;
    }

    private float[] getOffsets(Island islandModule) {
        if (islandModule != null) {
            return new float[]{
                    islandModule.xOffset.getCurrentValue(),
                    islandModule.yOffset.getCurrentValue()
            };
        }
        return new float[]{0.0f, 0.0f};
    }

    public void addContent(IslandContent content) {
        this.contents.add(content);
    }

    public void removeContent(IslandContent content) {
        this.contents.remove(content);
    }

    private IslandContent getActiveContent() {
        return contents.stream()
                .filter(IslandContent::shouldDisplay)
                .max(Comparator.comparingInt(IslandContent::getPriority))
                .orElse(null);
    }

    private float[] getActiveDimensions() {
        IslandContent activeContent = getActiveContent();

        if (activeContent != null) {
            return new float[]{activeContent.getWidth(), activeContent.getHeight()};
        } else {
            String username = getSessionUsername();
            String fpsText = StringUtils.split(mc.fpsDebugString, " ")[0] + " FPS";
            String serverIP = getCurrentServerIP();
            String latencyText = getPingText();

            String clientIcon = FontIcons.CLIENT;
            String userIcon = FontIcons.PLER;
            String connectIcon = FontIcons.CONNECT;

            float fontSize = 0.4f;
            float iconFontSize = 0.45f;

            float totalWidth =
                    Fonts.icons.getWidth(clientIcon, iconFontSize) +
                            Fonts.opensans.getWidth(" ", fontSize) +
                            Fonts.opensans.getWidth(Naven.CLIENT_DISPLAY_NAME, fontSize) +
                            Fonts.opensans.getWidth(" · ", fontSize) +
                            Fonts.icons.getWidth(userIcon, iconFontSize) +
                            Fonts.opensans.getWidth(" ", fontSize) +
                            Fonts.opensans.getWidth(username, fontSize) +
                            Fonts.opensans.getWidth(" · ", fontSize) +
                            Fonts.icons.getWidth(connectIcon, iconFontSize) +
                            Fonts.opensans.getWidth(" ", fontSize) +
                            Fonts.opensans.getWidth(latencyText, fontSize) +
                            Fonts.opensans.getWidth(" to ", fontSize) +
                            Fonts.opensans.getWidth(serverIP, fontSize) +
                            Fonts.opensans.getWidth(" · ", fontSize) +
                            Fonts.opensans.getWidth(fpsText, fontSize);

            return new float[]{
                    totalWidth + 40,
                    (float) Fonts.opensans.getHeight(false, fontSize) + 10
            };
        }
    }

    public void renderShader(DrawContext graphics) {
        IslandContent activeContent = getActiveContent();
        if (activeContent == null) {
            RenderUtils.drawRoundedRect(graphics.getMatrices(), posX.value, posY.value, animW.value, animH.value, 10, new Color(0, 0, 0, 160).getRGB()); // 从8增加到12
        } else {
            RenderUtils.drawRoundedRect(graphics.getMatrices(), posX.value, posY.value, animW.value, animH.value, 10, new Color(0, 0, 0, 160).getRGB()); // 从8增加到12
        }
    }

    public void render(DrawContext graphics) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float[] dimensions = getActiveDimensions();
        float targetWidth = dimensions[0];
        float targetHeight = dimensions[1];

        animW.target = targetWidth;
        animH.target = targetHeight;
        animW.update(true);
        animH.update(true);

        float baseX = (screenWidth - animW.value) / 2.0f;
        float baseY = screenHeight * 0.05f;
        Island islandModule = getIslandModule();
        if (islandModule != null) {
            islandModule.updateDrag(baseX, baseY, animW.value, animH.value);
        }

        float[] offsets = getOffsets(islandModule);
        float xOffset = offsets[0];
        float yOffset = offsets[1];

        float x = baseX + xOffset;
        float y = baseY + yOffset;

        posX.target = x;
        posY.target = y;
        posX.speed = 0.9f;
        posY.speed = 0.9f;
        posX.update(true);
        posY.update(true);

        IslandContent activeContent = getActiveContent();

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.getMatrices(), posX.value, posY.value, animW.value, animH.value, 10, 0xFFFFFFFF); // 从8增加到12
        StencilUtils.erase(true);
        RenderUtils.drawRoundedRect(graphics.getMatrices(), posX.value, posY.value, animW.value, animH.value, 10, new Color(20, 20, 20, 160).getRGB()); // 从8增加到12

        if (activeContent != null) {
            activeContent.render(graphics, graphics.getMatrices(), posX.value, posY.value);
        } else {
            renderDefaultContent(graphics.getMatrices());
        }

        StencilUtils.dispose();
    }

    public String getCurrentServerIP() {
        ServerInfo serverData = MinecraftClient.getInstance().getCurrentServerEntry();
        if (serverData != null) {
            return serverData.address;
        }
        return "SinglePlayer";
    }

    private String getPingText() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return "0ms";
        }

        try {
            var playerList = mc.player.networkHandler.getListedPlayerListEntries();
            for (var playerInfo : playerList) {
                if (playerInfo.getProfile().getId().equals(mc.player.getUuid())) {
                    int ping = playerInfo.getLatency();
                    if (ping < 0) {
                        return "0ms";
                    }
                    return ping + "ms";
                }
            }
        } catch (Exception e) {
        }

        return "0ms";
    }

    private void renderDefaultContent(MatrixStack stack) {
        String username = getSessionUsername();

        String fpsText = StringUtils.split(mc.fpsDebugString, " ")[0] + " FPS";
        String serverIP = getCurrentServerIP();
        String latencyText = getPingText();

        String clientIcon = FontIcons.CLIENT;
        String userIcon = FontIcons.PLER;
        String connectIcon = FontIcons.CONNECT;

        float fontSize = 0.4f;
        float iconFontSize = 0.45f;
        float textHeight = (float) Fonts.opensans.getHeight(false, fontSize);
        float iconHeight = (float) Fonts.icons.getHeight(false, iconFontSize);
        float y = getPosY() + (animH.value - textHeight) / 2.0f;
        float iconY = getPosY() + (animH.value - iconHeight) / 2.0f;

        float totalWidth =
                Fonts.icons.getWidth(clientIcon, iconFontSize) + Fonts.opensans.getWidth(" ", fontSize) +
                        Fonts.opensans.getWidth(Naven.CLIENT_DISPLAY_NAME, fontSize) +
                        Fonts.opensans.getWidth(" · ", fontSize) +
                        Fonts.icons.getWidth(userIcon, iconFontSize) + Fonts.opensans.getWidth(" ", fontSize) +
                        Fonts.opensans.getWidth(username, fontSize) +  // 使用动态用户名
                        Fonts.opensans.getWidth(" · ", fontSize) +
                        Fonts.icons.getWidth(connectIcon, iconFontSize) + Fonts.opensans.getWidth(" ", fontSize) +
                        Fonts.opensans.getWidth(latencyText, fontSize) +
                        Fonts.opensans.getWidth(" to ", fontSize) +
                        Fonts.opensans.getWidth(serverIP, fontSize) +
                        Fonts.opensans.getWidth(" · ", fontSize) +
                        Fonts.opensans.getWidth(fpsText, fontSize);

        float startX = getPosX() + (animW.value - totalWidth) / 2.0f;
        float currentX = startX;

        Fonts.icons.render(stack, clientIcon, currentX, iconY, CLIENT_NAME_COLOR, true, iconFontSize);
        currentX += Fonts.icons.getWidth(clientIcon, iconFontSize);

        Fonts.opensans.render(stack, " ", currentX, y, CLIENT_NAME_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" ", fontSize);

        Fonts.opensans.render(stack, Naven.CLIENT_DISPLAY_NAME, currentX, y, CLIENT_NAME_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(Naven.CLIENT_DISPLAY_NAME, fontSize);

        Fonts.opensans.render(stack, " · ", currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" · ", fontSize);

        Fonts.icons.render(stack, userIcon, currentX, iconY, WHITE_COLOR, true, iconFontSize);
        currentX += Fonts.icons.getWidth(userIcon, iconFontSize);

        Fonts.opensans.render(stack, " ", currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" ", fontSize);

        Fonts.opensans.render(stack, username, currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(username, fontSize);

        Fonts.opensans.render(stack, " · ", currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" · ", fontSize);

        Fonts.icons.render(stack, connectIcon, currentX, iconY, PING_COLOR, true, iconFontSize);
        currentX += Fonts.icons.getWidth(connectIcon, iconFontSize);

        Fonts.opensans.render(stack, " ", currentX, y, PING_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" ", fontSize);

        Fonts.opensans.render(stack, latencyText, currentX, y, PING_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(latencyText, fontSize);

        Fonts.opensans.render(stack, " to ", currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" to ", fontSize);

        Fonts.opensans.render(stack, serverIP, currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(serverIP, fontSize);

        Fonts.opensans.render(stack, " · ", currentX, y, WHITE_COLOR, true, fontSize);
        currentX += Fonts.opensans.getWidth(" · ", fontSize);

        Fonts.opensans.render(stack, fpsText, currentX, y, WHITE_COLOR, true, fontSize);
    }

    public float getPosX() {
        return posX.value;
    }

    public float getPosY() {
        return posY.value;
    }

    private String getSessionUsername() {
        return mc.getSession() == null ? "Player" : mc.getSession().getUsername();
    }

    public static SmoothAnimationTimer getAnimW() {
        return animW;
    }

    public static SmoothAnimationTimer getAnimH() {
        return animH;
    }
}
