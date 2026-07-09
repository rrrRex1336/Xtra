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
import awa.qwq.ovo.Naven.utils.renderer.Framebuffer;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.GL;
import awa.qwq.ovo.Naven.utils.renderer.PostProcessRenderer;
import awa.qwq.ovo.Naven.utils.renderer.Shader;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

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

    public final ModeValue animationScale = ValueBuilder.create(this, "Animation Scale")
            .setModes("Move", "Zoom", "Direct")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public BooleanValue glowShader = ValueBuilder.create(this, "Glow Shader")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> !listMode.isCurrentMode("Adjust"))
            .build().getBooleanValue();

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
    private final List<GlowRect> glowRects = new ArrayList<>();
    private final DragManager dragManager = new DragManager(this.xOffset, this.yOffset);
    private Shader moduleListGlowShader;
    private Framebuffer glowMaskBuffer;
    private Framebuffer glowCutoutBuffer;
    private Framebuffer glowBlurBuffer;
    private boolean moduleListGlowFailed;

    public String getModuleDisplayName(Module module) {
        if (listMode.isCurrentMode("Adjust")) {
            String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
            return name;
        }
        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " \u00a7f" + module.getSuffix());
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
        this.renderCustomGlow(e);
        this.glowRects.clear();
        e.getStack().push();

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
                float progress = this.updateModuleAnimation(module);
                if (progress <= 0.0F) continue;

                String displayName = this.getModuleDisplayName(module);
                float stringWidth = adjustFont.getWidth(displayName, fontSize);
                float renderFontSize = this.animationScale.isCurrentMode("Zoom") ? fontSize * progress : fontSize;
                float renderHeight = (float) adjustFont.getHeight(true, renderFontSize);
                float moveX = this.animationScale.isCurrentMode("Move") ? -stringWidth * (1.0F - progress) : 0.0F;
                float textY = yOffset + (lineHeight - renderHeight) / 2.0F;

                adjustFont.setAlpha(progress);
                adjustFont.render(e.getStack(), displayName, xOffset + moveX, textY, Color.WHITE, true, renderFontSize);
                yOffset += this.getAnimatedAdvance(lineHeight, lineSpacing, progress);
            }

            adjustFont.setAlpha(1.0F);
        } else if (listMode.isCurrentMode("Naven")) {
            float maxWidth = this.renderModules.isEmpty() ? 0.0F :
                    font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), this.fontSize.getCurrentValue());
            float height = 0.0F;
            double fontHeight = font.getHeight(true, this.fontSize.getCurrentValue());
            float baseX = this.direction.isCurrentMode("Right") ?
                    (float) mc.getWindow().getScaledWidth() - maxWidth - 6.0F :
                    3.0F;
            float baseY = 0.0F;

            this.dragManager.update(baseX, baseY, maxWidth + 3.0F, this.getEnabledModuleListHeight((float) fontHeight, 0.0F));
            float moduleListX = this.dragManager.getX(baseX);
            float moduleListY = this.dragManager.getY(baseY);

            for (Module module : this.renderModules) {
                float progress = this.updateModuleAnimation(module);
                if (progress > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float fontSizeVal = this.fontSize.getCurrentValue();
                    float zoom = this.getZoomScale(progress);
                    float stringWidth = font.getWidth(displayName, fontSizeVal);
                    float renderFontSize = fontSizeVal * zoom;
                    float renderStringWidth = font.getWidth(displayName, renderFontSize);
                    float alignX = this.direction.isCurrentMode("Left") ? 0.0F : maxWidth - renderStringWidth;
                    float moveX = this.getMoveOffset(stringWidth, progress);
                    float innerX = alignX + moveX;
                    float moduleHeight = this.getAnimatedSize((float) fontHeight, progress);
                    float moduleWidth = renderStringWidth + 3.0F * zoom;

                    RenderUtils.fillBound(e.getStack(),
                            moduleListX + innerX,
                            moduleListY + height + 2.0F,
                            moduleWidth,
                            moduleHeight,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight));

                    int color = Naven.getInstance().getThemeManager().getColor(height);
                    this.addGlowRect(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight, color);

                    font.setAlpha(progress);
                    font.render(e.getStack(), displayName,
                            moduleListX + innerX + 1.5F * zoom,
                            moduleListY + height + 1.0F * zoom,
                            new Color(color),
                            true,
                            renderFontSize
                    );
                    height += this.getAnimatedAdvance((float) fontHeight, 0.0F, progress);
                }
            }

            font.setAlpha(1.0F);
        } else if (listMode.isCurrentMode("IconList")){
            CustomTextRenderer iconFont = Fonts.icons;

            float maxWidth = this.renderModules.isEmpty() ? 0.0F :
                    font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double) this.fontSize.getCurrentValue());
            float height = 0.0F;
            double fontHeight = font.getHeight(true, (double) this.fontSize.getCurrentValue());
            float lineSpacing = 4.0F;
            float baseX = this.direction.isCurrentMode("Right") ?
                    (float) mc.getWindow().getScaledWidth() - maxWidth - 6.0F :
                    3.0F;
            float baseY = 0.0F;

            this.dragManager.update(baseX, baseY, maxWidth + (float) fontHeight + 10.0F, this.getEnabledModuleListHeight((float) fontHeight, lineSpacing));
            float moduleListX = this.dragManager.getX(baseX);
            float moduleListY = this.dragManager.getY(baseY);

            for (Module module : this.renderModules) {
                float progress = this.updateModuleAnimation(module);
                if (progress > 0.0F) {
                    String displayName = this.getModuleDisplayName(module);
                    float fontSizeVal = this.fontSize.getCurrentValue();
                    float zoom = this.getZoomScale(progress);
                    float stringWidth = font.getWidth(displayName, (double) fontSizeVal);
                    float renderFontSize = fontSizeVal * zoom;
                    float renderStringWidth = font.getWidth(displayName, (double) renderFontSize);
                    float alignX = this.direction.isCurrentMode("Left") ? 0.0F : maxWidth - renderStringWidth;
                    float moveX = this.getMoveOffset(stringWidth, progress);
                    float innerX = alignX + moveX;
                    float moduleHeight = this.getAnimatedSize((float) fontHeight, progress);
                    float moduleWidth = renderStringWidth + 6.0F * zoom;

                    RenderUtils.drawRoundedRect(e.getStack(),
                            moduleListX + innerX,
                            moduleListY + height + 2.0F,
                            moduleWidth,
                            moduleHeight,
                            3.0F,
                            backgroundColor
                    );
                    this.blurMatrices.add(new Vector4f(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight));

                    int color = Naven.getInstance().getThemeManager().getColor(height);
                    this.addGlowRect(moduleListX + innerX, moduleListY + height + 2.0F, moduleWidth, moduleHeight, color);

                    float iconBoxHeight = moduleHeight;
                    float iconBoxWidth = iconBoxHeight;

                    float iconBoxY = moduleListY + height + 2.0F;
                    float iconBoxX;

                    if (this.direction.isCurrentMode("Right")) {
                        iconBoxX = moduleListX + innerX + moduleWidth + 1.5F * zoom;
                    } else {
                        iconBoxX = moduleListX + innerX - iconBoxWidth - 1.5F * zoom;
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
                    this.addGlowRect(iconBoxX, iconBoxY, iconBoxWidth, iconBoxHeight, color);

                    String iconChar = getCategoryIcon(module.getCategory());

                    float iconSize = fontSizeVal * 0.65F * zoom;
                    float iconCharHeightSmall = (float) font.getHeight(true, iconSize);
                    float iconWidth = iconFont.getWidth(iconChar, iconSize);
                    float iconRenderX = iconBoxX + (iconBoxWidth - iconWidth) / 2.0F - 0.2F;
                    float iconRenderY = iconBoxY + (iconBoxHeight - iconCharHeightSmall) / 2.0F - 0.0F;

                    iconFont.setAlpha(progress);
                    iconFont.render(e.getStack(), iconChar,
                            (double) iconRenderX,
                            (double) iconRenderY,
                            new Color(color),
                            true,
                            (double) iconSize);

                    font.setAlpha(progress);
                    float textX = moduleListX + innerX + (moduleWidth - renderStringWidth) / 2.0F;
                    float textY = moduleListY + height + 2.0F + (moduleHeight - (float) font.getHeight(true, (double) renderFontSize)) / 2.0F;

                    font.render(e.getStack(), displayName, (double) textX, (double) textY, new Color(color), true, (double) renderFontSize);
                    height += this.getAnimatedAdvance((float) fontHeight, lineSpacing, progress);
                }
            }

            font.setAlpha(1.0F);
            if (iconFont != null) {
                iconFont.setAlpha(1.0F);
            }
        }

        e.getStack().pop();
    }

    private float getFontSize() {
        if (listMode.isCurrentMode("Adjust")) return 0.65F;
        if (listMode.isCurrentMode("Naven")) return this.fontSize.getCurrentValue();
        return this.fontSize.getCurrentValue();
    }

    private float updateModuleAnimation(Module module) {
        SmoothAnimationTimer animation = module.getAnimation();
        boolean visible = module.isEnabled() && !module.isHidden();
        animation.target = visible ? 100.0F : 0.0F;

        if (this.animationScale.isCurrentMode("Direct")) {
            animation.value = animation.target;
        } else {
            animation.update(true);
        }

        return animation.value / 100.0F;
    }

    private float getMoveOffset(float width, float progress) {
        if (!this.animationScale.isCurrentMode("Move")) {
            return 0.0F;
        }

        float offset = width * (1.0F - progress);
        return this.direction.isCurrentMode("Right") ? offset : -offset;
    }

    private float getAnimatedSize(float size, float progress) {
        if (this.animationScale.isCurrentMode("Zoom")) {
            return size * progress;
        }

        return size;
    }

    private float getZoomScale(float progress) {
        return this.animationScale.isCurrentMode("Zoom") ? progress : 1.0F;
    }

    private float getAnimatedAdvance(float lineHeight, float spacing, float progress) {
        if (this.animationScale.isCurrentMode("Direct")) {
            return progress > 0.0F ? lineHeight + spacing : 0.0F;
        }

        return (lineHeight + spacing) * progress;
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

    private void addGlowRect(float x, float y, float width, float height, int color) {
        if (!this.glowShader.getCurrentValue() || this.listMode.isCurrentMode("Adjust") || width <= 0.0F || height <= 0.0F) {
            return;
        }

        this.glowRects.add(new GlowRect(x, y, width, height, color));
    }

    private void renderCustomGlow(EventRender2D event) {
        if (this.moduleListGlowFailed || !this.glowShader.getCurrentValue() || this.listMode.isCurrentMode("Adjust") || this.glowRects.isEmpty()) {
            return;
        }

        try {
            this.ensureGlowResources();
            boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

            try {
                this.glowMaskBuffer.bind();
                this.glowMaskBuffer.setViewport();
                GL11.glDisable(GL11.GL_STENCIL_TEST);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                for (GlowRect rect : this.glowRects) {
                    this.drawGlowSource(event.getStack(), rect);
                }

                this.glowCutoutBuffer.bind();
                this.glowCutoutBuffer.setViewport();
                GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                for (GlowRect rect : this.glowRects) {
                    this.drawGlowCutout(event.getStack(), rect);
                }

                this.renderGlowLayer(event, 4, 3.4F, 0.94F);
                this.renderGlowLayer(event, 9, 2.05F, 0.62F);
            } finally {
                if (stencilWasEnabled) {
                    GL11.glEnable(GL11.GL_STENCIL_TEST);
                } else {
                    GL11.glDisable(GL11.GL_STENCIL_TEST);
                }

                if (depthWasEnabled) {
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                } else {
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                }

                if (blendWasEnabled) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                } else {
                    RenderSystem.disableBlend();
                }

                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                GL.resetTextureSlot();
            }
        } catch (Exception exception) {
            this.moduleListGlowFailed = true;
            exception.printStackTrace();
        }
    }

    private void ensureGlowResources() {
        if (this.moduleListGlowShader == null) {
            this.moduleListGlowShader = new Shader("modulelist_glow.vert", "modulelist_glow.frag");
        }

        if (this.glowMaskBuffer == null) {
            this.glowMaskBuffer = new Framebuffer();
            this.glowCutoutBuffer = new Framebuffer();
            this.glowBlurBuffer = new Framebuffer();
            return;
        }

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (this.glowMaskBuffer.width != width || this.glowMaskBuffer.height != height) {
            this.glowMaskBuffer.resize();
            this.glowCutoutBuffer.resize();
            this.glowBlurBuffer.resize();
        }
    }

    private void drawGlowSource(MatrixStack stack, GlowRect rect) {
        int glowColor = mixColor(rect.color(), 0xFFFFFF, 0.08F);
        RenderUtils.drawRoundedRect(stack, rect.x() - 5.5F, rect.y() - 2.0F, rect.width() + 11.0F, rect.height() + 4.0F, 5.0F, withAlpha(glowColor, 70));
        RenderUtils.drawRoundedRect(stack, rect.x() - 3.75F, rect.y() - 1.35F, rect.width() + 7.5F, rect.height() + 2.7F, 4.5F, withAlpha(glowColor, 120));
        RenderUtils.drawRoundedRect(stack, rect.x() - 2.0F, rect.y() - 0.75F, rect.width() + 4.0F, rect.height() + 1.5F, 3.75F, withAlpha(glowColor, 185));
        RenderUtils.drawRoundedRect(stack, rect.x() - 0.75F, rect.y() - 0.25F, rect.width() + 1.5F, rect.height() + 0.5F, 3.0F, withAlpha(glowColor, 235));
    }

    private void drawGlowCutout(MatrixStack stack, GlowRect rect) {
        RenderUtils.drawRoundedRect(stack, rect.x(), rect.y(), rect.width(), rect.height(), 3.0F, 0xFFFFFFFF);
    }

    private void renderGlowLayer(EventRender2D event, int radius, float softness, float intensity) {
        this.glowBlurBuffer.bind();
        this.glowBlurBuffer.setViewport();
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        this.renderGlowTexture(event.getStack(), this.glowMaskBuffer.texture, this.glowCutoutBuffer.texture, this.glowMaskBuffer.width, this.glowMaskBuffer.height, 1.0F, 0.0F, 1.0F, radius, softness, false);

        mc.getFramebuffer().beginWrite(false);
        GL.viewport(0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.renderGlowTexture(event.getStack(), this.glowBlurBuffer.texture, this.glowCutoutBuffer.texture, this.glowBlurBuffer.width, this.glowBlurBuffer.height, 0.0F, 1.0F, intensity, radius, softness, true);
    }

    private void renderGlowTexture(MatrixStack stack, int texture, int maskTexture, int width, int height, float directionX, float directionY, float intensity, int radius, float softness, boolean cutoutSource) {
        this.moduleListGlowShader.bind();
        GL.bindTexture(texture, 0);
        GL.bindTexture(maskTexture, 1);
        this.moduleListGlowShader.set("u_Texture", 0);
        this.moduleListGlowShader.set("u_MaskTexture", 1);
        this.moduleListGlowShader.set("u_Size", (double) width, (double) height);
        this.moduleListGlowShader.set("u_Direction", directionX, directionY);
        this.moduleListGlowShader.set("u_Intensity", intensity);
        this.moduleListGlowShader.set("u_Radius", radius);
        this.moduleListGlowShader.set("u_Softness", softness);
        this.moduleListGlowShader.set("u_CutoutSource", cutoutSource);
        PostProcessRenderer.beginRender(stack);
        PostProcessRenderer.render(stack);
        PostProcessRenderer.endRender();
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int mixColor(int c1, int c2, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * clamped);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * clamped);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * clamped);
        return (r << 16) | (g << 8) | b;
    }

    private static final int[] WATER_COLORS = {
            0x0CE8C7,  // RGB(12, 232, 199) 青绿色
            0x0CA3E8   // RGB(12, 163, 232) 蓝色
    };

    private static final int[] SNOW_COLORS = {
            0xF7FCFF,
            0xDEEFFF,
            0xC0D8F5,
            0x9EBBEA,
            0x78A8E8
    };

    private String getCategoryIcon(Category category) {
        if (category == null) return "?";
        return category.getIcon();
    }

    private record GlowRect(float x, float y, float width, float height, int color) {
    }
}
