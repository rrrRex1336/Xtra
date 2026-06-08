package awa.qwq.ovo.Naven.ui.Island;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public interface IslandContent {
    int getPriority();

    boolean shouldDisplay();

    void render(GuiGraphics graphics, PoseStack stack, float x, float y);

    float getWidth();

    float getHeight();
}

