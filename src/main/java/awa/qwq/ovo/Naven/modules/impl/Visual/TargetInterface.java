package awa.qwq.ovo.Naven.modules.impl.Visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;

import java.awt.*;

@ModuleInfo(name = "TargetInterface", description = "Display you target info.", category = Category.VISUAL)
public class TargetInterface extends Module {

    public static Entity target;
    private Vector4f blurMatrix;

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;
        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)target;
            e.getStack().pushPose();
            float x = mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
            float y = mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
            String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
            float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
            this.blurMatrix = new Vector4f(x, y, width, 30.0F);
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, width, 30.0F, 5.0F, WaterMark.headerColor);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, width, 30.0F, WaterMark.bodyColor);
            RenderUtils.fillBound(e.getStack(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, WaterMark.headerColor);
            StencilUtils.dispose();
            Fonts.harmony.render(e.getStack(), targetName, x + 5.0F, y + 6.0F, Color.WHITE, true, 0.35F);
            Fonts.harmony.render(e.getStack(), "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""), x + 5.0F, y + 17.0F, Color.WHITE, true, 0.35F);
            e.getStack().popPose();
        }
    }
    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }
}
