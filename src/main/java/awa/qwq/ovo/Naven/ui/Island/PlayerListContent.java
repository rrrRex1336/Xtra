package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRenderTabOverlay;
import awa.qwq.ovo.Naven.modules.impl.visual.Island;
import awa.qwq.ovo.Naven.ui.Island.TabOverlayState;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import org.mixin.PlayerTabOverlayAccessor;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class PlayerListContent implements IslandContent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Comparator<PlayerListEntry> PLAYER_COMPARATOR = Comparator.<PlayerListEntry>comparingInt((p) -> {
        return p.getGameMode() == GameMode.SPECTATOR ? 1 : 0;
    }).thenComparing((p) -> {
        return java.util.Optional.ofNullable(p.getScoreboardTeam()).map(Team::getName).orElse("");
    }).thenComparing((p) -> {
        return p.getProfile().getName();
    }, String::compareToIgnoreCase);
    /**
     * @author
     * @reason
     */
    @SuppressWarnings("removal")
    private final Identifier GUI_ICONS_LOCATION = new Identifier("minecraft", "textures/gui/icons.png");

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

        boolean tabPressed = mc.options.playerListKey.isPressed();

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
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        if (!isVisible || mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        List<PlayerListEntry> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return;
        }
        
        float padding = 10f;
        float contentX = x + padding;
        float contentY = y + padding;
        float contentWidth = getWidth() - padding * 2;

        float alpha = alphaAnimation.value;

        Text header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<OrderedText> headerLines = mc.textRenderer.wrapLines(headerEvent.getComponent(), (int)contentWidth);
            
            for (OrderedText line : headerLines) {
                int lineWidth = mc.textRenderer.getWidth(line);
                float lineX = contentX + (contentWidth - lineWidth) / 2f;
                int color = ((int)(255 * alpha) << 24) | 0xFFFFFF;
                graphics.drawText(mc.textRenderer, line, (int)lineX, (int)contentY, color, false);
                contentY += 9;
            }
            contentY += 2;
        }

        int playerCount = playerList.size();
        int columns = getColumnCount(playerCount);
        int rows = getRowCount(playerCount, columns);

        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        int avatarWidth = mc.isInSingleplayer() || (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection().isEncrypted()) ? 9 : 0;
        int columnPadding = 10;
        
        for (PlayerListEntry playerInfo : playerList) {
            Text displayName = getNameForDisplay(playerInfo);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, displayName, playerInfo);
            Naven.getInstance().getEventManager().call(nameEvent);
            displayName = nameEvent.getComponent();
            
            int nameWidth = mc.textRenderer.getWidth(displayName) + 2;
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
            
            PlayerListEntry currentPlayer = playerList.get(index);
            
            float currentPlayerX = playerX;

            if (avatarWidth > 0) {
                PlayerEntity entity = mc.world != null ? mc.world.getPlayerByUuid(currentPlayer.getProfile().getId()) : null;
                boolean upsideDown = entity != null && LivingEntityRenderer.shouldFlipUpsideDown(entity);
                boolean hasHat = entity != null && entity.isPartVisible(PlayerModelPart.HAT);
                SkinTextures skin = currentPlayer.getSkinTextures();
                Identifier skinLocation = skin.texture();

                PlayerSkinDrawer.draw(graphics, skinLocation,
                        (int)currentPlayerX, (int)playerY, 8, hasHat, upsideDown);
                currentPlayerX += avatarWidth;
            }

            Text name = getNameForDisplay(currentPlayer);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, name, currentPlayer);
            Naven.getInstance().getEventManager().call(nameEvent);
            name = nameEvent.getComponent();
            int nameColor = currentPlayer.getGameMode() == GameMode.SPECTATOR ? 
                ((int)(255 * alpha) << 24) | 0x4AFFFFFF : ((int)(255 * alpha) << 24) | 0xFFFFFFFF;
            graphics.drawText(mc.textRenderer, name, (int)currentPlayerX, (int)playerY, nameColor, false);


            renderPingIcon(stack, graphics, columnWidth, (int)(currentPlayerX - avatarWidth), (int)playerY, currentPlayer, alpha);
        }

        Text footer = getTabFooter();
        if (footer != null) {
            contentY = startY + rows * 9 + 2;
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<OrderedText> footerLines = mc.textRenderer.wrapLines(footerEvent.getComponent(), (int)contentWidth);
            
            for (OrderedText line : footerLines) {
                int lineWidth = mc.textRenderer.getWidth(line);
                float lineX = contentX + (contentWidth - lineWidth) / 2f;
                int color = ((int)(255 * alpha) << 24) | 0xFFFFFF;
                graphics.drawText(mc.textRenderer, line, (int)lineX, (int)contentY, color, false);
                contentY += 9;
            }
        }
    }
    
    private void renderPingIcon(MatrixStack stack, DrawContext graphics, int columnWidth, int x, int y, PlayerListEntry playerInfo, float alpha) {
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
        
        stack.push();
        stack.translate(0.0F, 0.0F, 100.0F);
        graphics.drawTexture(GUI_ICONS_LOCATION, x + columnWidth - 11, y, 0, 176 + iconIndex * 8, 10, 8);
        stack.pop();
    }
    
    private Text getTabHeader() {
        try {
            return ((PlayerTabOverlayAccessor) mc.inGameHud.getPlayerListHud()).getHeader();
        } catch (Exception ignored) {
            return TabOverlayState.getHeader();
        }
    }
    
    private Text getTabFooter() {
        try {
            return ((PlayerTabOverlayAccessor) mc.inGameHud.getPlayerListHud()).getFooter();
        } catch (Exception ignored) {
            return TabOverlayState.getFooter();
        }
    }
    
    private List<PlayerListEntry> getPlayerInfos() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return java.util.Collections.emptyList();
        }
        return mc.player.networkHandler.getListedPlayerListEntries().stream()
            .sorted(PLAYER_COMPARATOR)
            .limit(80L)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private Text getNameForDisplay(PlayerListEntry playerInfo) {
        MutableText name;
        if (playerInfo.getDisplayName() != null) {
            name = playerInfo.getDisplayName().copy();
        } else {
            name = Team.decorateName(playerInfo.getScoreboardTeam(),
                Text.literal(playerInfo.getProfile().getName()));
        }
        return decorateName(playerInfo, name);
    }
    
    private Text decorateName(PlayerListEntry playerInfo, MutableText name) {
        return playerInfo.getGameMode() == GameMode.SPECTATOR ? 
            name.formatted(net.minecraft.util.Formatting.ITALIC) : name;
    }

    private int getColumnCount(int playerCount) {
        int columns = 1;
        int rows = playerCount;
        while (rows > 20) {
            columns++;
            rows = getRowCount(playerCount, columns);
        }
        return columns;
    }

    private int getRowCount(int playerCount, int columns) {
        return Math.max(1, (playerCount + columns - 1) / columns);
    }
    

    @Override
    public float getWidth() {
        if (!isVisible || mc.player == null || mc.player.networkHandler == null) {
            return 200;
        }
        
        List<PlayerListEntry> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return 200;
        }
        
        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        int avatarWidth = mc.isInSingleplayer() || (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection().isEncrypted()) ? 9 : 0;
        int columnPadding = 10;
        
        for (PlayerListEntry playerInfo : playerList) {
            Text displayName = getNameForDisplay(playerInfo);
            EventRenderTabOverlay nameEvent = new EventRenderTabOverlay(EventType.NAME, displayName, playerInfo);
            Naven.getInstance().getEventManager().call(nameEvent);
            displayName = nameEvent.getComponent();
            
            int nameWidth = mc.textRenderer.getWidth(displayName) + 2;
            maxNameWidth = Math.max(maxNameWidth, nameWidth);
        }
        
        int playerCount = playerList.size();
        int columns = getColumnCount(playerCount);
        int rows = getRowCount(playerCount, columns);
        
        int columnWidth = maxNameWidth + maxScoreWidth + avatarWidth + columnPadding;
        int totalWidth = columns * columnWidth + (columns - 1) * 5;

        float headerWidth = 0;
        Text header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<OrderedText> headerLines = mc.textRenderer.wrapLines(headerEvent.getComponent(), (int)totalWidth);
            for (OrderedText line : headerLines) {
                headerWidth = Math.max(headerWidth, mc.textRenderer.getWidth(line));
            }
        }
        
        float footerWidth = 0;
        Text footer = getTabFooter();
        if (footer != null) {
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<OrderedText> footerLines = mc.textRenderer.wrapLines(footerEvent.getComponent(), (int)totalWidth);
            for (OrderedText line : footerLines) {
                footerWidth = Math.max(footerWidth, mc.textRenderer.getWidth(line));
            }
        }
        
        float finalWidth = Math.max(Math.max(totalWidth, headerWidth), footerWidth) + 20;
        int screenWidth = mc.getWindow().getScaledWidth();
        finalWidth = Math.max(finalWidth, 200f); // 最小宽度200
        finalWidth = Math.min(finalWidth, screenWidth * 0.8f); // 最大宽度为屏幕的80%
        return finalWidth;
    }
    
    @Override
    public float getHeight() {
        if (!isVisible || mc.player == null || mc.player.networkHandler == null) {
            return 40;
        }
        
        List<PlayerListEntry> playerList = getPlayerInfos();
        if (playerList.isEmpty()) {
            return 40;
        }
        
        float height = 20;
        Text header = getTabHeader();
        if (header != null) {
            EventRenderTabOverlay headerEvent = new EventRenderTabOverlay(EventType.HEADER, header, null);
            Naven.getInstance().getEventManager().call(headerEvent);
            List<OrderedText> headerLines = mc.textRenderer.wrapLines(headerEvent.getComponent(), 500);
            height += headerLines.size() * 9 + 2;
        }
        int playerCount = playerList.size();
        int columns = getColumnCount(playerCount);
        int rows = getRowCount(playerCount, columns);
        height += rows * 9;

        Text footer = getTabFooter();
        if (footer != null) {
            EventRenderTabOverlay footerEvent = new EventRenderTabOverlay(EventType.FOOTER, footer, null);
            Naven.getInstance().getEventManager().call(footerEvent);
            List<OrderedText> footerLines = mc.textRenderer.wrapLines(footerEvent.getComponent(), 500);
            height += footerLines.size() * 9 + 2;
        }
        
        return height;
    }
}



