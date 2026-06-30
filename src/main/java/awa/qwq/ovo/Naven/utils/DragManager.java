package awa.qwq.ovo.Naven.utils;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.values.HasValue;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class DragManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static DragManager activeDrag;

    private final FloatValue xOffset;
    private final FloatValue yOffset;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean wasMouseDown;

    public DragManager(FloatValue xOffset, FloatValue yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public static FloatValue createHiddenPositionValue(HasValue owner, String name, float defaultValue) {
        return ValueBuilder.create(owner, name)
                .setDefaultFloatValue(defaultValue)
                .setMinFloatValue(-10000.0F)
                .setMaxFloatValue(10000.0F)
                .setFloatStep(1.0F)
                .setVisibility(() -> false)
                .build()
                .getFloatValue();
    }

    public void update(float baseX, float baseY, float width, float height) {
        boolean mouseDown = isLeftMouseDown();
        if (!canDrag() || width <= 0.0F || height <= 0.0F) {
            releaseIfNeeded(mouseDown);
            this.wasMouseDown = mouseDown;
            return;
        }

        float mouseX = getMouseX();
        float mouseY = getMouseY();
        float x = getX(baseX);
        float y = getY(baseY);

        if (mouseDown) {
            if (activeDrag == null && !this.wasMouseDown && isHovering(mouseX, mouseY, x, y, width, height)) {
                activeDrag = this;
                this.dragOffsetX = mouseX - x;
                this.dragOffsetY = mouseY - y;
            }

            if (activeDrag == this) {
                this.xOffset.setCurrentValue(mouseX - this.dragOffsetX - baseX);
                this.yOffset.setCurrentValue(mouseY - this.dragOffsetY - baseY);
            }
        } else {
            releaseIfNeeded(false);
        }

        this.wasMouseDown = mouseDown;
    }

    public float getX(float baseX) {
        return baseX + this.xOffset.getCurrentValue();
    }

    public float getY(float baseY) {
        return baseY + this.yOffset.getCurrentValue();
    }

    private void releaseIfNeeded(boolean mouseDown) {
        if (activeDrag == this && !mouseDown) {
            activeDrag = null;
            if (Naven.getInstance() != null && Naven.getInstance().getFileManager() != null) {
                Naven.getInstance().getFileManager().save();
            }
        }
    }

    private static boolean canDrag() {
        return mc.player != null
                && mc.level != null
                && mc.screen != null
                && !mc.screen.isPauseScreen()
                && mc.mouseHandler != null
                && mc.getWindow() != null;
    }

    private static boolean isLeftMouseDown() {
        if (mc.getWindow() == null) {
            return false;
        }

        return GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private static float getMouseX() {
        int screenWidth = mc.getWindow().getScreenWidth();
        if (screenWidth <= 0) {
            return 0.0F;
        }

        return (float) mc.mouseHandler.xpos() * (float) mc.getWindow().getGuiScaledWidth() / (float) screenWidth;
    }

    private static float getMouseY() {
        int screenHeight = mc.getWindow().getScreenHeight();
        if (screenHeight <= 0) {
            return 0.0F;
        }

        return (float) mc.mouseHandler.ypos() * (float) mc.getWindow().getGuiScaledHeight() / (float) screenHeight;
    }

    private static boolean isHovering(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
