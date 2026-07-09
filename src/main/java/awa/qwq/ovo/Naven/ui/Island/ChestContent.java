package awa.qwq.ovo.Naven.ui.Island;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.impl.player.ContainerStealer;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ChestContent implements IslandContent {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    @Override
    public int getPriority() {
        return 120;
    }
    
    @Override
    public boolean shouldDisplay() {
        if (mc.player == null) {
            return false;
        }
        ContainerStealer containerStealer = (ContainerStealer) Naven.getInstance().getModuleManager().getModule(ContainerStealer.class);
        if (containerStealer != null && containerStealer.isEnabled() && containerStealer.motionMode.isCurrentMode("Silent")) {
            return mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
        }
        return false;
    }

    @Override
    public void render(DrawContext graphics, MatrixStack stack, float x, float y) {
        if (mc.player == null || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu)) {
            return;
        }

        DiffuseLighting.disableGuiDepthLighting();
        graphics.getMatrices().push();
        graphics.getMatrices().translate(x + 8, y + 4, 0);
        
        boolean isEmpty = true;
        int containerSlotCount = menu.getRows() * 9;

        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = menu.getSlot(i);
            if (slot == null) continue;

            int slotX = i % 9;
            int slotY = i / 9;
            float slotRenderX = slotX * 18.0f + 3.0f;
            float slotRenderY = slotY * 18.0f + 3.0f;
            
            ItemStack stack2 = slot.getStack();

            if (!stack2.isEmpty()) {
                isEmpty = false;
                RenderSystem.enableDepthTest();
                graphics.drawItem(stack2, (int)(slotRenderX - 2), (int)(slotRenderY - 3));
                graphics.drawItemInSlot(mc.textRenderer, stack2, (int)(slotRenderX - 2), (int)(slotRenderY - 3));
                RenderSystem.disableDepthTest();
            }
        }
        
        graphics.getMatrices().pop();

        if (isEmpty) {
            graphics.getMatrices().push();
            graphics.getMatrices().translate(x + getWidth() / 2.0f, y + getHeight() / 2.0f, 0);
            String emptyText = "Empty...";
            float textWidth = Fonts.harmony.getWidth(emptyText, 0.5f);
            Fonts.harmony.render(graphics.getMatrices(), emptyText, -textWidth / 2.0f, -Fonts.harmony.getHeight(false, 0.5f) / 2.0f, new Color(-1),true, 0.5f);
            graphics.getMatrices().pop();
        }

        DiffuseLighting.enableGuiDepthLighting();
    }
    
    @Override
    public float getWidth() {
        return 180.0f;
    }
    
    @Override
    public float getHeight() {
        if (mc.player == null || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu)) {
            return 50.0f;
        }

        int rows = menu.getRows();
        return rows * 18.0f + 6.0f;
    }
}

