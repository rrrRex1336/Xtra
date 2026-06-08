package awa.qwq.ovo.Naven.utils;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

public class ImageRendererUtils {

    private int width;
    private int height;
    private int[] pixels;

    public ImageRendererUtils(ResourceLocation location) {
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            try (InputStream inputStream = resourceManager.open(location)) {
                NativeImage image = NativeImage.read(inputStream);
                this.width = image.getWidth();
                this.height = image.getHeight();
                this.pixels = new int[width * height];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        pixels[y * width + x] = image.getPixelRGBA(x, y);
                    }
                }
                image.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.width = 0;
            this.height = 0;
            this.pixels = new int[0];
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 用 GuiGraphics.fill 手动绘制每个像素
     * 完全绕过纹理缩放，直接画点
     */
    public void render(GuiGraphics guiGraphics, int x, int y) {
        if (pixels.length == 0) return;

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int color = pixels[py * width + px];
                int alpha = (color >> 24) & 0xFF;
                if (alpha == 0) continue;
                guiGraphics.fill(x + px, y + py, x + px + 1, y + py + 1, color);
            }
        }
    }
}