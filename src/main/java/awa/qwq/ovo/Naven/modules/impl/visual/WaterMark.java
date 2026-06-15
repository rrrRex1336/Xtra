package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.Version;
import awa.qwq.ovo.Naven.auth.VerifyClient;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
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

import org.apache.commons.lang3.StringUtils;

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

    private float width;
    private float watermarkHeight;

    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() == EventType.SHADOW) {
            RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        e.getStack().pushPose();

        if (mode.isCurrentMode("Naven")) {
            renderNavenMode(e);
        } else {
            renderAdjustMode(e);
        }

        e.getStack().popPose();
    }

    private void renderNavenMode(EventRender2D e) {
        CustomTextRenderer font = Fonts.opensans;

        String userName = "Shiroko";
        String userRole = "User";
        try {
            userName = VerifyClient.getUserName();
            userRole = VerifyClient.getUserRole();
        } catch (Exception ex) {
        }

        String userDisplay = userName + "[" + userRole + "]";

        String text = "Naven | " + Version.getVersion() + " | " + userDisplay + "§r | " +
                StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());

        this.width = font.getWidth(text, this.watermarkSize.getCurrentValue()) + 14.0F;
        this.watermarkHeight = (float) font.getHeight(true, this.watermarkSize.getCurrentValue());

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        RenderUtils.fill(e.getStack(), 5.0F, 5.0F, 9.0F + this.width, 8.0F, headerColor);
        RenderUtils.fill(e.getStack(), 5.0F, 8.0F, 9.0F + this.width, 16.0F + this.watermarkHeight, bodyColor);
        font.render(e.getStack(), text, 12.0, 10.0, Color.WHITE, true, this.watermarkSize.getCurrentValue());
        StencilUtils.dispose();
    }

    private void renderAkarinMode(EventRender2D e) {

    }

    private void renderAdjustMode(EventRender2D e) {
        CustomTextRenderer font = Fonts.misans;
        float fontSize = 0.65F;

        String fps = StringUtils.split(mc.fpsString, " ")[0];
        String clientName = Naven.CLIENT_DISPLAY_NAME;
        String firstLetter = clientName.substring(0, 1);
        String restLetters = clientName.substring(1);

        float xOffset = 4.0F;
        float yOffset = 4.0F;
        int rainbowColor = RenderUtils.getRainbowOpaque(5, 1.0F, 1.0F, 5000.0F);
        font.render(e.getStack(), firstLetter, xOffset, yOffset, new Color(rainbowColor), true, fontSize);
        xOffset += font.getWidth(firstLetter, fontSize);
        font.render(e.getStack(), restLetters, xOffset, yOffset, Color.WHITE, true, fontSize);
        xOffset += font.getWidth(restLetters, fontSize);
        font.render(e.getStack(), " (" + fps + " FPS)", xOffset, yOffset, Color.WHITE, true, fontSize);
    }
}