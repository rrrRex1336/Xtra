package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import java.awt.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class ErrorMessageContent implements IslandContent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static ErrorMessageContent instance;
    
    private String errorMessage = "";
    private long displayStartTime = 0;
    private boolean isSuccess = false;
    private static final long DISPLAY_DURATION = 1500;
    
    public ErrorMessageContent() {
        instance = this;
    }
    
    public static ErrorMessageContent getInstance() {
        return instance;
    }

    public void showError(String message) {
        this.errorMessage = message;
        this.isSuccess = false;
        this.displayStartTime = System.currentTimeMillis();
    }

    public void showSuccess(String message) {
        this.errorMessage = message;
        this.isSuccess = true;
        this.displayStartTime = System.currentTimeMillis();
    }
    
    @Override
    public int getPriority() {
        return 150;
    }
    
    @Override
    public boolean shouldDisplay() {
        if (errorMessage.isEmpty()) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - displayStartTime >= DISPLAY_DURATION) {
            errorMessage = "";
            displayStartTime = 0;
            return false;
        }
        return true;
    }
    
    @Override
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        if (errorMessage.isEmpty()) {
            return;
        }
        
        float padding = 10f;
        float scale = 0.4f;
        Color textColor = isSuccess ? new Color(100, 255, 100, 255) : new Color(255, 100, 100, 255);
        Fonts.harmony.render(stack, errorMessage, x + padding, y + padding, textColor, true, scale);
    }
    
    @Override
    public float getWidth() {
        if (errorMessage.isEmpty()) {
            return 200;
        }
        
        float padding = 10f * 2;
        float scale = 0.4f;
        float textWidth = Fonts.harmony.getWidth(errorMessage, scale);
        
        return textWidth + padding;
    }
    
    @Override
    public float getHeight() {
        if (errorMessage.isEmpty()) {
            return 40;
        }
        
        float padding = 10f * 2;
        float scale = 0.4f;
        float textHeight = (float) Fonts.harmony.getHeight(true, scale);
        
        return textHeight + padding;
    }
}

