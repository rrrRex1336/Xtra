package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.Version;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
        name = "WaterMark",
        description = "Displays a watermark on your screen",
        category = Category.VISUAL
)
public class WaterMark extends Module {

    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

    public final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Naven", "Adjust", "Akarin")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public FloatValue watermarkSize = ValueBuilder.create(this, "Watermark Size")
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build().getFloatValue();

    private final FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
    private final FloatValue yOffset = DragManager.createHiddenPositionValue(this, "Drag Y", 0.0F);
    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);

    private float width;
    private float watermarkHeight;

    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() == EventType.SHADOW && mode.isCurrentMode("Naven")) {
            float x = this.dragManager.getX(5.0F);
            float y = this.dragManager.getY(5.0F);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        e.getStack().push();

        if (mode.isCurrentMode("Naven")) {
            CustomTextRenderer font = Fonts.opensans;

            String userDisplay = mc.getSession() == null ? "Player" : mc.getSession().getUsername();

            String text = "Naven | " + Version.getVersion() + " | " + userDisplay + "§r | " +
                    StringUtils.split(mc.fpsDebugString, " ")[0] + " FPS | " + format.format(new Date());

            this.width = font.getWidth(text, this.watermarkSize.getCurrentValue()) + 14.0F;
            this.watermarkHeight = (float) font.getHeight(true, this.watermarkSize.getCurrentValue());
            this.dragManager.update(5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F);
            float x = this.dragManager.getX(5.0F);
            float y = this.dragManager.getY(5.0F);

            boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMaskWasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            prepareHudRenderState();
            try {
                StencilUtils.write(false);
                RenderUtils.drawRoundedRect(e.getStack(), x, y, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
                StencilUtils.erase(true);
                RenderUtils.fill(e.getStack(), x, y, x + 4.0F + this.width, y + 3.0F, headerColor);
                RenderUtils.fill(e.getStack(), x, y + 3.0F, x + 4.0F + this.width, y + 11.0F + this.watermarkHeight, bodyColor);
                font.setAlpha(1.0F);
                font.render(e.getStack(), text, x + 7.0F, y + 5.0F, Color.WHITE, true, this.watermarkSize.getCurrentValue());
            } finally {
                StencilUtils.dispose();
                restoreHudRenderState(depthWasEnabled, depthMaskWasEnabled, blendWasEnabled);
            }
        } else {
            CustomTextRenderer font = Fonts.misans;
            float fontSize = 0.65F;

            String fps = StringUtils.split(mc.fpsDebugString, " ")[0];
            String clientName = Naven.CLIENT_DISPLAY_NAME;
            String firstLetter = clientName.substring(0, 1);
            String restLetters = clientName.substring(1);

            String fpsText = " (" + fps + " FPS)";
            float xOffset = 4.0F;
            float yOffset = 4.0F;

            if (!mode.isCurrentMode("Adjust")) {
                this.width = font.getWidth(firstLetter, fontSize) + font.getWidth(restLetters, fontSize) + font.getWidth(fpsText, fontSize);
                this.watermarkHeight = (float) font.getHeight(true, fontSize);
                this.dragManager.update(4.0F, 4.0F, this.width, this.watermarkHeight);
                xOffset = this.dragManager.getX(4.0F);
                yOffset = this.dragManager.getY(4.0F);
            }

            int rainbowColor = RenderUtils.getRainbowOpaque(5, 1.0F, 1.0F, 5000.0F);
            font.render(e.getStack(), firstLetter, xOffset, yOffset, new Color(rainbowColor), true, fontSize);
            xOffset += font.getWidth(firstLetter, fontSize);
            font.render(e.getStack(), restLetters, xOffset, yOffset, Color.WHITE, true, fontSize);
            xOffset += font.getWidth(restLetters, fontSize);
            font.render(e.getStack(), fpsText, xOffset, yOffset, Color.WHITE, true, fontSize);
        }

        e.getStack().pop();
    }

    private void prepareHudRenderState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void restoreHudRenderState(boolean depthWasEnabled, boolean depthMaskWasEnabled, boolean blendWasEnabled) {
        RenderSystem.colorMask(true, true, true, true);
        if (depthWasEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GL11.glDepthMask(depthMaskWasEnabled);
        if (blendWasEnabled) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.disableBlend();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
