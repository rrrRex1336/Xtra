package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(
        name = "TNTWarning",
        description = "Show tnts distance",
        category = Category.VISUAL
)
public class TNTWarning extends Module {
    public static BlockPos nearestTntPos = null;
    public static TNTWarning instance;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public TNTWarning() {
        instance = this;
    }

    @EventTarget
    public void on2D(EventRender2D event) {
        this.onRender(event.getStack());
    }

    public void onRender(MatrixStack poseStack) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        CustomTextRenderer font = Fonts.harmony;
        List<TntEntity> tnts = mc.world.getNonSpectatingEntities(TntEntity.class, mc.player.getBoundingBox().expand(10.0));
        if (tnts.isEmpty()) {
            return;
        }
        double closestDist = Double.MAX_VALUE;
        TntEntity closestTnt = null;
        nearestTntPos = null;
        for (TntEntity tnt : tnts) {
            double dist = mc.player.distanceTo(tnt);
            if (!(dist < closestDist)) continue;
            closestDist = dist;
            closestTnt = tnt;
        }
        if (closestTnt != null && closestDist <= 10.0) {
            nearestTntPos = closestTnt.getBlockPos();
            Color color = this.getGradientColor(closestDist);
            String text = String.format("TNT Distance: %.1f", closestDist);
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            int progressY = screenHeight / 2 + 35;
            int progressHeight = 6;
            float textWidth = mc.textRenderer.getWidth(String.format("TNT Distance: %.1f", closestDist)) * 0.4f;
            float progressTextX = (float) screenWidth / 2.0f - textWidth / 2.0f;
            float progressTextY = progressY + progressHeight + 6;
            font.render(poseStack, text, progressTextX - 2.0f, progressTextY, color, true, 0.4f);
        }
    }

    private Color getGradientColor(double distance) {
        float ratio = (float) MathHelper.clamp(distance / 10.0, 0.0, 1.0);
        int red = (int) (255.0f * (1.0f - ratio));
        int green = (int) (255.0f * ratio);
        return new Color(red, green, 0);
    }
}