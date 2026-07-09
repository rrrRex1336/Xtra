package awa.qwq.ovo.Naven.utils;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ImageRendererUtils {

    private int width;
    private int height;
    private int[] pixels;

    public ImageRendererUtils(Identifier location) {
        try {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            try (InputStream inputStream = resourceManager.open(location)) {
                NativeImage image = NativeImage.read(inputStream);
                this.width = image.getWidth();
                this.height = image.getHeight();
                this.pixels = new int[width * height];

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        pixels[y * width + x] = image.getColor(x, y);
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
    public void render(DrawContext guiGraphics, int x, int y) {
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