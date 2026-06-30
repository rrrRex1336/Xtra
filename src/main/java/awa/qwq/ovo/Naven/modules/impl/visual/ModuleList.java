package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.ModuleManager;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;

@ModuleInfo(
        name = "ModuleList",
        description = "Displays enabled modules on your screen",
        category = Category.VISUAL
)
public class ModuleList extends Module {

    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();

    public final ModeValue listMode = ValueBuilder.create(this, "List Mode")
            .setModes("IconList", "Adjust", "Naven")
            .setDefaultModeIndex(2)
            .build()
            .getModeValue();

    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
            .setOnUpdate(value -> Module.update = true)
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();

    public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
            .setOnUpdate(value -> Module.update = true)
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();

    public ModeValue colorMode = ValueBuilder.create(this, "Color Mode")
            .setDefaultModeIndex(1)
            .setModes("White", "Rainbow", "Water")
            .setVisibility(() -> !listMode.isCurrentMode("Adjust"))
            .build().getModeValue();

    public FloatValue colorSpeed = ValueBuilder.create(this, "Color Speed")
            .setVisibility(() -> !listMode.isCurrentMode("White") && !listMode.isCurrentMode("Adjust"))
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(10.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    public FloatValue colorOffset = ValueBuilder.create(this, "Color Offset")
            .setVisibility(() -> !listMode.isCurrentMode("White") && !listMode.isCurrentMode("Adjust"))
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    public ModeValue direction = ValueBuilder.create(this, "Direction")
            .setDefaultModeIndex(0)
            .setModes("Right", "Left")
            .setVisibility(() -> !listMode.isCurrentMode("Adjust"))
            .build().getModeValue();

    public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
            .setMinFloatValue(-10000.0F)
            .setMaxFloatValue(10000.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> false)
            .build()
            .getFloatValue();

    public FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
            .setMinFloatValue(-10000.0F)
            .setMaxFloatValue(10000.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> false)
            .build()
            .getFloatValue();

    public FloatValue fontSize = ValueBuilder.create(this, "Font Size")
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .setVisibility(() -> !listMode.isCurrentMode("Adjust"))
            .build()
            .getFloatValue();

    private List<Module> renderModules;
    private List<Vector4f> blurMatrices = new ArrayList<>();
    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);

    public String getModuleDisplayName(Module module) {
        if (listMode.isCurrentMode("Adjust")) {
            String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
            return name;
        }
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() != EventType.BLUR) return;

        for (Vector4f rect : this.blurMatrices) {
            if (listMode.isCurrentMode("Naven")) {
                RenderUtils.fillBound(e.getStack(), rect.x(), rect.y(), rect.z(), rect.w(), -1);
            } else {
                RenderUtils.drawRoundedRect(e.getStack(), rect.x(), rect.y(), rect.z(), rect.w(), 3.0F, -1);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        CustomTextRenderer font = Fonts.opensans;

        this.blurMatrices.clear();
        e.getStack().pushPose();

        ModuleManager moduleManager = Naven.getInstance().getModuleManager();
        List<Module> allModules = new ArrayList<>(moduleManager.getModules());
        if (this.hideRenderModules.getCurrentValue()) {
            allModules.removeIf((modulex) -> modulex.getCategory() == Category.VISUAL);
        }
        allModules.removeIf(Module::isHidden);

        if (update || this.renderModules == null) {
            this.renderModules = new ArrayList<>(allModules);
            this.renderModules.sort((o1, o2) -> {
                float o1Width = font.getWidth(this.getModuleDisplayName(o1), this.getFontSize());
                float o2Width = font.getWidth(this.getModuleDisplayName(o2), this.getFontSize());
                return Float.compare(o2Width, o1Width);
            });
        }

        if (listMode.isCurrentMode("Adjust")) {
            CustomTextRenderer adjustFont = Fonts.misans;
            float fontSize = 0.65F;
            float lineHeight = (float) adjustFont.getHeight(true, fontSize);
            float lineSpacing = 1.0F;
            float xOffset = 4.0F;
            float yOffset = 4.0F;

            WaterMark waterMark = (WaterMark) Naven.getInstance().getModuleManager().getModule(WaterMark.class);
            if (waterMark != null && waterMark.isEnabled() && waterMark.mode.isCurrentMode("Adjust")) {
                yOffset = 4.0F + (float) adjustFont.getHeight(true, 0.65F) + lineSpacing;
            }

            for (Module module : this.renderModules) {
                if (!module.isEnabled() || module.isHidden()) continue;

                String displayName = this.getModuleDisplayName(module);
                adjustFont.render(e.getStack(), displayName, xOffset, yOffset, Color.WHITE, true, fontSize);
                yOffset += lineHeight + lineSpacing;
            }
        } else if (listMode.isCurrentMode("Naven")) {
            float maxWidth = this.renderModules.isEmpty() ? 0.0F :
                    font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), this.fontSize.getCurrentValue());
            float height = 0.0F;
            double fontHeight = font.getHeight(true, this.fontSize.getCurrentValue());
            float baseX = this.direction.isCurrentMode("Right") ?
                    (float) mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F :
                    3.0F;
            float baseY = 0.0F;

            this.dragManager.update(baseX, baseY, maxWidth + 3.0F, this.getEnabledModuleListHeight((float) fontHeight, 0.0F));
            float moduleListX = this.dragManager.getX(baseX);
            float moduleListY = this.dragManager.getY(baseY);

            for (Module module : this.renderModules) {
                SmoothAnimationTimer animation = module.getAnimation();
                if (module.isEnabled() && !module.isHidden()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }

                animation.update(true);
                if (animation.value > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float stringWidth = font.getWidth(displayName, this.fontSize.getCurrentValue());
                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
                    float innerX = this.direction.isCurrentMode("Left") ? left : right;
                    float moduleHeight = (float) ((double) (animation.value / 100.0F) * fontHeight);
                    float moduleWidth = stringWidth + 3.0F;

                    RenderUtils.fillBound(e.getStack(),
                            moduleListX + innerX,
                            moduleListY + height + 2.0F,
                            moduleWidth,
                            moduleHeight,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight));

                    int color = -1;
                    if (this.colorMode.isCurrentMode("Rainbow")) {
                        float mappedSpeed = 21.0F - (this.colorSpeed.getCurrentValue() * 1.9F);
                        color = RenderUtils.getRainbowOpaque(
                                (int) (-height * this.colorOffset.getCurrentValue()),
                                1.0F,
                                1.0F,
                                mappedSpeed * 1000.0F
                        );
                    } else if (this.colorMode.isCurrentMode("Water")) {
                        color = getWaterColor((int) (-height * this.colorOffset.getCurrentValue()), this.colorSpeed.getCurrentValue());
                    }

                    float alpha = animation.value / 100.0F;
                    font.setAlpha(alpha);
                    font.render(e.getStack(), displayName,
                            moduleListX + innerX + 1.5F,
                            moduleListY + height + 1.0F,
                            new Color(color),
                            true,
                            this.fontSize.getCurrentValue()
                    );
                    height += (float) ((double) (animation.value / 100.0F) * fontHeight);
                }
            }

            font.setAlpha(1.0F);
        } else if (listMode.isCurrentMode("IconList")){
            CustomTextRenderer iconFont = Fonts.icons;

            float maxWidth = this.renderModules.isEmpty() ? 0.0F :
                    font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double) this.fontSize.getCurrentValue());
            float height = 0.0F;
            double fontHeight = font.getHeight(true, (double) this.fontSize.getCurrentValue());
            float baseX = this.direction.isCurrentMode("Right") ?
                    (float) mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F :
                    3.0F;
            float baseY = 0.0F;

            this.dragManager.update(baseX, baseY, maxWidth + (float) fontHeight + 10.0F, this.getEnabledModuleListHeight((float) fontHeight, 2.0F));
            float moduleListX = this.dragManager.getX(baseX);
            float moduleListY = this.dragManager.getY(baseY);

            for (Module module : this.renderModules) {
                SmoothAnimationTimer animation = module.getAnimation();
                if (module.isEnabled() && !module.isHidden()) {
                    animation.target = 100.0F;
                } else {
                    animation.target = 0.0F;
                }

                animation.update(true);
                if (animation.value > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float stringWidth = font.getWidth(displayName, (double) this.fontSize.getCurrentValue());
                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
                    float innerX = this.direction.isCurrentMode("Left") ? left : right;
                    float moduleHeight = (float) ((double) (animation.value / 100.0F) * fontHeight);
                    float moduleWidth = stringWidth + 6.0F;

                    RenderUtils.drawRoundedRect(e.getStack(),
                            moduleListX + innerX,
                            moduleListY + height + 2.0F,
                            moduleWidth,
                            moduleHeight,
                            3.0F,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight));

                    int color = -1;

                    if (this.colorMode.isCurrentMode("Rainbow")) {
                        float mappedSpeed = 21.0F - (this.colorSpeed.getCurrentValue() * 1.9F);
                        color = RenderUtils.getRainbowOpaque(
                                (int) (-height * this.colorOffset.getCurrentValue()),
                                1.0F,
                                1.0F,
                                mappedSpeed * 1000.0F
                        );
                    } else if (this.colorMode.isCurrentMode("Water")) {
                        color = getWaterColor((int) (-height * this.colorOffset.getCurrentValue()), this.colorSpeed.getCurrentValue());
                    }

                    float fontSizeVal = this.fontSize.getCurrentValue();
                    float iconBoxHeight = moduleHeight;
                    float iconBoxWidth = iconBoxHeight;

                    float iconBoxY = moduleListY + height + 2.0F;
                    float iconBoxX;

                    if (this.direction.isCurrentMode("Right")) {
                        iconBoxX = moduleListX + innerX + moduleWidth + 1.5F;
                    } else {
                        iconBoxX = moduleListX + innerX - iconBoxWidth - 1.5F;
                    }

                    RenderUtils.drawRoundedRect(e.getStack(),
                            iconBoxX,
                            iconBoxY,
                            iconBoxWidth,
                            iconBoxHeight,
                            3.0F,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(iconBoxX, iconBoxY, iconBoxWidth, iconBoxHeight));

                    String iconChar = getCategoryIcon(module.getCategory());

                    float iconSize = fontSizeVal * 0.65F;
                    float iconCharHeightSmall = (float) font.getHeight(true, iconSize);
                    float iconWidth = iconFont.getWidth(iconChar, iconSize);
                    float iconRenderX = iconBoxX + (iconBoxWidth - iconWidth) / 2.0F - 0.2F;
                    float iconRenderY = iconBoxY + (iconBoxHeight - iconCharHeightSmall) / 2.0F - 0.0F;

                    float alpha = animation.value / 100.0F;
                    iconFont.setAlpha(alpha);
                    iconFont.render(e.getStack(), iconChar,
                            (double) iconRenderX,
                            (double) iconRenderY,
                            new Color(color),
                            true,
                            (double) iconSize);

                    font.setAlpha(alpha);
                    float textX = moduleListX + innerX + (moduleWidth - stringWidth) / 2.0F;
                    float textY = moduleListY + height + 2.0F + (moduleHeight - (float) fontHeight) / 2.0F;

                    font.render(e.getStack(), displayName, (double) textX, (double) textY, new Color(color), true, (double) fontSizeVal);
                    height += (float) ((double) (animation.value / 100.0F) * fontHeight) + 2.0F;
                }
            }

            font.setAlpha(1.0F);
            if (iconFont != null) {
                iconFont.setAlpha(1.0F);
            }
        }

        e.getStack().popPose();
    }

    private float getFontSize() {
        if (listMode.isCurrentMode("Adjust")) return 0.65F;
        if (listMode.isCurrentMode("Naven")) return this.fontSize.getCurrentValue();
        return this.fontSize.getCurrentValue();
    }

    private float getEnabledModuleListHeight(float lineHeight, float spacing) {
        if (this.renderModules == null) {
            return 0.0F;
        }

        int count = 0;
        for (Module module : this.renderModules) {
            if (module.isEnabled() && !module.isHidden()) {
                count++;
            }
        }

        if (count == 0) {
            return 0.0F;
        }

        return count * lineHeight + Math.max(0, count - 1) * spacing + 4.0F;
    }

    private static final int[] WATER_COLORS = {
            0x0CE8C7,  // RGB(12, 232, 199) 青绿色
            0x0CA3E8   // RGB(12, 163, 232) 蓝色
    };

    private int getWaterColor(int index, float speed) {
        long time = System.currentTimeMillis();
        float period = speed * 300;
        float progress = (float)((time + index * 50L) % (long)period) / period;
        float t = (float)((Math.cos(progress * Math.PI * 2) + 1) / 2);
        int c1 = WATER_COLORS[0];
        int c2 = WATER_COLORS[1];

        int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);

        return (r << 16) | (g << 8) | b;
    }

    private String getCategoryIcon(Category category) {
        if (category == null) return "?";
        return category.getIcon();
    }
}
