package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.modules.impl.visual.Island;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Comparator;
import java.util.List;

public class PlayerListContent implements IslandContent {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt((p) -> {
        return p.getGameMode() == GameType.SPECTATOR ? 1 : 0;
    }).thenComparing((p) -> {
        return java.util.Optional.ofNullable(p.getTeam()).map(PlayerTeam::getName).orElse("");
    }).thenComparing((p) -> {
        return p.getProfile().getName();
    }, String::compareToIgnoreCase);
    /**
     * @author
     * @reason
     */
    @SuppressWarnings("removal")
    private final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("minecraft", "textures/gui/icons.png");

    private boolean isVisible = false;
    private final SmoothAnimationTimer alphaAnimation = new SmoothAnimationTimer(0.0f, 0.3f);

    @Override
    public int getPriority() {
        return 180;
    }
    
    @Override
    public boolean shouldDisplay() {
        Island islandModule =
            (Island) Naven.getInstance()
                .getModuleManager().getModule(Island.class);

        if (islandModule == null || !islandModule.isEnabled()) {
            isVisible = false;
            return false;
        }

        boolean tabPressed = mc.options.keyPlayerList.isDown();

        alphaAnimation.target = tabPressed ? 1.0f : 0.0f;
        alphaAnimation.update(true);

        if (alphaAnimation.value < 0.01f) {
            isVisible = false;
            return false;
        }
        
        isVisible = true;
        return true;
    }
    
    @Override
    public void render(GuiGraphics graphics, PoseStack stack, float x, float y) {
        if (!isVisible || mc.player == null || mc.player.connection == null) {
            return;
        }

        List<PlayerInfo> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return;
        }
        
        float padding = 10f;
        float contentX = x + padding;
        float contentY = y + padding;
        float contentWidth = getWidth() - padding * 2;

        float alpha = alphaAnimation.value;

        Component header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<FormattedCharSequence> headerLines = mc.font.split(headerEvent.getComponent(), (int)contentWidth);
            
            for (FormattedCharSequence line : headerLines) {
                int lineWidth = mc.font.width(line);
                float lineX = contentX + (contentWidth - lineWidth) / 2f;
                int color = ((int)(255 * alpha) << 24) | 0xFFFFFF;
                graphics.drawString(mc.font, line, (int)lineX, (int)contentY, color, false);
                contentY += 9;
            }
            contentY += 2;
        }

        int playerCount = playerList.size();
        int rows = Math.min(playerCount, 20);
        int columns;
        for (columns = 1; rows > 20; rows = (playerCount + columns - 1) / columns) {
            ++columns;
        }

        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        int avatarWidth = mc.isLocalServer() || (mc.getConnection() != null && mc.getConnection().getConnection().isEncrypted()) ? 9 : 0;
        int columnPadding = 10;
        
        for (PlayerInfo playerInfo : playerList) {
            Component displayName = getNameForDisplay(playerInfo);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, displayName, playerInfo);
            Naven.getInstance().getEventManager().call(nameEvent);
            displayName = nameEvent.getComponent();
            
            int nameWidth = mc.font.width(displayName) + 2;
            maxNameWidth = Math.max(maxNameWidth, nameWidth);
        }
        
        int columnWidth = maxNameWidth + maxScoreWidth + avatarWidth + columnPadding;
        float startX = contentX;
        float startY = contentY;

        for (int index = 0; index < playerCount; ++index) {
            int col = index / rows;
            int row = index % rows;
            float playerX = startX + col * columnWidth + col * 5;
            float playerY = startY + row * 9;
            
            if (playerY + 8 > y + getHeight() - padding) {
                continue;
            }
            
            PlayerInfo currentPlayer = playerList.get(index);
            
            float currentPlayerX = playerX;

            if (avatarWidth > 0) {
                Player entity = mc.level != null ? mc.level.getPlayerByUUID(currentPlayer.getProfile().getId()) : null;
                boolean upsideDown = entity != null && LivingEntityRenderer.isEntityUpsideDown(entity);
                boolean hasHat = entity != null && entity.isModelPartShown(PlayerModelPart.HAT);
                PlayerSkin skin = currentPlayer.getSkin();
                ResourceLocation skinLocation = skin.texture();

                PlayerFaceRenderer.draw(graphics, skinLocation,
                        (int)currentPlayerX, (int)playerY, 8, hasHat, upsideDown);
                currentPlayerX += avatarWidth;
            }

            Component name = getNameForDisplay(currentPlayer);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, name, currentPlayer);
            Naven.getInstance().getEventManager().call(nameEvent);
            name = nameEvent.getComponent();
            int nameColor = currentPlayer.getGameMode() == GameType.SPECTATOR ? 
                ((int)(255 * alpha) << 24) | 0x4AFFFFFF : ((int)(255 * alpha) << 24) | 0xFFFFFFFF;
            graphics.drawString(mc.font, name, (int)currentPlayerX, (int)playerY, nameColor, false);


            renderPingIcon(stack, graphics, columnWidth, (int)(currentPlayerX - avatarWidth), (int)playerY, currentPlayer, alpha);
        }

        Component footer = getTabFooter();
        if (footer != null) {
            contentY = startY + rows * 9 + 2;
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<FormattedCharSequence> footerLines = mc.font.split(footerEvent.getComponent(), (int)contentWidth);
            
            for (FormattedCharSequence line : footerLines) {
                int lineWidth = mc.font.width(line);
                float lineX = contentX + (contentWidth - lineWidth) / 2f;
                int color = ((int)(255 * alpha) << 24) | 0xFFFFFF;
                graphics.drawString(mc.font, line, (int)lineX, (int)contentY, color, false);
                contentY += 9;
            }
        }
    }
    
    private void renderPingIcon(PoseStack stack, GuiGraphics graphics, int columnWidth, int x, int y, PlayerInfo playerInfo, float alpha) {
        int latency = playerInfo.getLatency();
        int iconIndex;
        if (latency < 0) {
            iconIndex = 5;
        } else if (latency < 150) {
            iconIndex = 0;
        } else if (latency < 300) {
            iconIndex = 1;
        } else if (latency < 600) {
            iconIndex = 2;
        } else if (latency < 1000) {
            iconIndex = 3;
        } else {
            iconIndex = 4;
        }
        
        stack.pushPose();
        stack.translate(0.0F, 0.0F, 100.0F);
        graphics.blit(GUI_ICONS_LOCATION, x + columnWidth - 11, y, 0, 176 + iconIndex * 8, 10, 8);
        stack.popPose();
    }
    
    private Component getTabHeader() {
        try {
            java.lang.reflect.Field field = net.minecraft.client.gui.components.PlayerTabOverlay.class.getDeclaredField("header");
            field.setAccessible(true);
            return (Component) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Component getTabFooter() {
        try {
            java.lang.reflect.Field field = net.minecraft.client.gui.components.PlayerTabOverlay.class.getDeclaredField("footer");
            field.setAccessible(true);
            return (Component) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<PlayerInfo> getPlayerInfos() {
        if (mc.player == null || mc.player.connection == null) {
            return java.util.Collections.emptyList();
        }
        return mc.player.connection.getListedOnlinePlayers().stream()
            .sorted(PLAYER_COMPARATOR)
            .limit(80L)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private Component getNameForDisplay(PlayerInfo playerInfo) {
        MutableComponent name;
        if (playerInfo.getTabListDisplayName() != null) {
            name = playerInfo.getTabListDisplayName().copy();
        } else {
            name = PlayerTeam.formatNameForTeam(playerInfo.getTeam(),
                Component.literal(playerInfo.getProfile().getName()));
        }
        return decorateName(playerInfo, name);
    }
    
    private Component decorateName(PlayerInfo playerInfo, MutableComponent name) {
        return playerInfo.getGameMode() == GameType.SPECTATOR ? 
            name.withStyle(net.minecraft.ChatFormatting.ITALIC) : name;
    }
    

    @Override
    public float getWidth() {
        if (!isVisible || mc.player == null || mc.player.connection == null) {
            return 200;
        }
        
        List<PlayerInfo> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return 200;
        }
        
        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        int avatarWidth = mc.isLocalServer() || (mc.getConnection() != null && mc.getConnection().getConnection().isEncrypted()) ? 9 : 0;
        int columnPadding = 10;
        
        for (PlayerInfo playerInfo : playerList) {
            Component displayName = getNameForDisplay(playerInfo);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, displayName, playerInfo);
            Naven.getInstance().getEventManager().call(nameEvent);
            displayName = nameEvent.getComponent();
            
            int nameWidth = mc.font.width(displayName) + 2;
            maxNameWidth = Math.max(maxNameWidth, nameWidth);
        }
        
        int playerCount = playerList.size();
        int rows = Math.min(playerCount, 20);
        int columns;
        for (columns = 1; rows > 20; rows = (playerCount + columns - 1) / columns) {
            ++columns;
        }
        
        int columnWidth = maxNameWidth + maxScoreWidth + avatarWidth + columnPadding;
        int totalWidth = columns * columnWidth + (columns - 1) * 5;

        float headerWidth = 0;
        Component header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<FormattedCharSequence> headerLines = mc.font.split(headerEvent.getComponent(), (int)totalWidth);
            for (FormattedCharSequence line : headerLines) {
                headerWidth = Math.max(headerWidth, mc.font.width(line));
            }
        }
        
        float footerWidth = 0;
        Component footer = getTabFooter();
        if (footer != null) {
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<FormattedCharSequence> footerLines = mc.font.split(footerEvent.getComponent(), (int)totalWidth);
            for (FormattedCharSequence line : footerLines) {
                footerWidth = Math.max(footerWidth, mc.font.width(line));
            }
        }
        
        float finalWidth = Math.max(Math.max(totalWidth, headerWidth), footerWidth) + 20;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        finalWidth = Math.max(finalWidth, 200f); // 最小宽度200
        finalWidth = Math.min(finalWidth, screenWidth * 0.8f); // 最大宽度为屏幕的80%
        return finalWidth;
    }
    
    @Override
    public float getHeight() {
        if (!isVisible || mc.player == null || mc.player.connection == null) {
            return 40;
        }
        
        List<PlayerInfo> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return 40;
        }
        
        float height = 20;
        Component header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<FormattedCharSequence> headerLines = mc.font.split(headerEvent.getComponent(), 500);
            height += headerLines.size() * 9 + 2;
        }
        int playerCount = playerList.size();
        int rows = Math.min(playerCount, 20);
        int columns;
        for (columns = 1; rows > 20; rows = (playerCount + columns - 1) / columns) {
            ++columns;
        }
        height += rows * 9;

        Component footer = getTabFooter();
        if (footer != null) {
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<FormattedCharSequence> footerLines = mc.font.split(footerEvent.getComponent(), 500);
            height += footerLines.size() * 9 + 2;
        }
        
        return height;
    }
}



