package awa.qwq.ovo.Naven.ui.Island;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public interface IslandContent {
    int getPriority();

    boolean shouldDisplay();

    void render(DrawContext graphics, MatrixStack stack, float x, float y);

    float getWidth();

    float getHeight();
}

