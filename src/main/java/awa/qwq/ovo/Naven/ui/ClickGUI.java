package awa.qwq.ovo.Naven.ui;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.managers.theme.ThemeStyle;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.Colors;
import awa.qwq.ovo.Naven.utils.FontIcons;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.StencilUtils;
import awa.qwq.ovo.Naven.utils.StringUtils;
import awa.qwq.ovo.Naven.utils.TimeHelper;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueType;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import awa.qwq.ovo.Naven.values.impl.StringValue;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ClickGUI extends Screen {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static float windowX = 100.0F;
   public static float windowY = 100.0F;
   public static float windowWidth = 400.0F;
   public static float windowHeight = 250.0F;
   Category selectedCategory = null;
   Module selectedModule = null;
   int[] dragMousePosition = new int[]{-1, -1};
   boolean hoveringBack = false;
   SmoothAnimationTimer widthAnimation = new SmoothAnimationTimer(100.0F);
   SmoothAnimationTimer heightAnimation = new SmoothAnimationTimer(185.0F);
   SmoothAnimationTimer titleAnimation = new SmoothAnimationTimer(100.0F);
   SmoothAnimationTimer titleHoverAnimation = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer categoryMotionY = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer themeMotionY = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer moduleValuesMotionY = new SmoothAnimationTimer(0.0F);
   HashMap<Category, SmoothAnimationTimer> categoryXAnimation = new HashMap<Category, SmoothAnimationTimer>() {
      {
         for (Category value : Category.values()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Category, SmoothAnimationTimer> categoryYAnimation = new HashMap<Category, SmoothAnimationTimer>() {
      {
         for (Category value : Category.values()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Category, List<Module>> modules = new HashMap<Category, List<Module>>() {
      {
         for (Category value : Category.values()) {
            this.put(value, Naven.getInstance().getModuleManager().getModulesByCategory(value));
         }
      }
   };
   HashMap<Module, SmoothAnimationTimer> modulesAnimation = new HashMap<Module, SmoothAnimationTimer>() {
      {
         for (Module value : Naven.getInstance().getModuleManager().getModules()) {
            this.put(value, new SmoothAnimationTimer(0.0F, 255.0F));
         }
      }
   };
   HashMap<Module, SmoothAnimationTimer> modulesToggleAnimation = new HashMap<Module, SmoothAnimationTimer>() {
      {
         for (Module value : Naven.getInstance().getModuleManager().getModules()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Value, SmoothAnimationTimer> valuesAnimation = new HashMap<Value, SmoothAnimationTimer>() {
      {
         for (Value value : Naven.getInstance().getValueManager().getValues()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<ThemeStyle, SmoothAnimationTimer> themeAnimations = new HashMap<ThemeStyle, SmoothAnimationTimer>() {
      {
         for (ThemeStyle style : ThemeStyle.VALUES) {
            this.put(style, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   String titleDisplayName = "";
   float finalModuleHeight;
   float finalValueHeight;
   boolean clickReturnModules = false;
   boolean clickReturnCategories = false;
   boolean clickOpenCategoryModules = false;
   boolean clickResizeWindow = false;
   boolean clickDragWindow = false;
   Category hoveringCategory = null;
   Module hoveringModule = null;
   Module bindingModule = null;
   SmoothAnimationTimer moduleSwapAnimation = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer bindingAnimation = new SmoothAnimationTimer(0.0F);
   List<Module> categoryModules;
   List<Value> renderValues;
   BooleanValue hoveringBooleanValue;
   FloatValue hoveringFloatValue;
   FloatValue draggingFloatValue;
   StringValue hoveringStringValue;
   StringValue editingStringValue;
   String editingStringText = "";
   ModeValue hoveringModeValue;
   AddonsValue hoveringAddonsValue;
   ThemeStyle hoveringTheme;
   int targetAddonIndex = -1;
   int targetModeValueIndex;
   String bindingModuleName;
   private boolean mouseDown = false;
   private float minCatY = 0.0F;
   private SmoothAnimationTimer moduleAlphaAnimation = new SmoothAnimationTimer(0.0F);
   private TimeHelper moduleAlphaTimer = new TimeHelper();
   private float minValueY = 0.0F;
   private SmoothAnimationTimer valuesAlphaAnimation = new SmoothAnimationTimer(0.0F);
   private TimeHelper valuesAlphaTimer = new TimeHelper();

   public ClickGUI() {
      super(Text.of("Naven"));
   }

   public void close() {
      if (this.editingStringValue != null) {
         this.commitEditingString(true);
      }
      Naven.getInstance().getFileManager().save();
      Naven.getInstance().getEventManager().unregister(this);
      super.close();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (mouseButton == 0) {
         this.mouseDown = true;
      }

      if (this.bindingModule == null || mouseButton != 3 && mouseButton != 4) {
         if (mouseButton != 2 && this.bindingModule == null) {
            if (this.hoveringModule != null) {
               if (mouseButton == 0) {
                  this.hoveringModule.toggle();
               } else if (mouseButton == 1) {
                  this.selectedModule = this.hoveringModule;
                  this.renderValues = Naven.getInstance().getValueManager().getValuesByHasValue(this.hoveringModule);
                  this.moduleValuesMotionY.target = this.moduleValuesMotionY.value = 0.0F;
               }
            }

            if (mouseButton == 0) {
               if (this.selectedCategory == Category.THEME && this.hoveringTheme != null) {
                  Naven.getInstance().getThemeManager().setStyle(this.hoveringTheme);
                  Naven.getInstance().getFileManager().save();
               }

               if (this.hoveringBack && !this.clickResizeWindow && !this.clickDragWindow) {
                  this.selectedCategory = null;
                  this.selectedModule = null;
                  this.renderValues = null;
               }

               if (this.clickOpenCategoryModules && this.hoveringCategory != null) {
                  this.selectedCategory = this.hoveringCategory;
                  this.selectedModule = null;
                  this.renderValues = null;
                  this.categoryMotionY.value = this.categoryMotionY.target = 0.0F;
                  this.themeMotionY.value = this.themeMotionY.target = 0.0F;
                  this.moduleSwapAnimation.value = 5.0F;
                  this.moduleSwapAnimation.target = 255.0F;
                  this.clickOpenCategoryModules = false;
               }

               boolean doDragWindow = this.selectedCategory != null
                  ? RenderUtils.isHovering((int)mouseX, (int)mouseY, windowX, windowY, windowX + windowWidth, windowY + 25.0F)
                  : RenderUtils.isHovering((int)mouseX, (int)mouseY, windowX, windowY, windowX + 100.0F, windowY + 40.0F);
               if ((this.selectedCategory == null || !this.hoveringBack) && doDragWindow) {
                  this.SetDragPosition(mouseX, mouseY);
                  this.clickDragWindow = true;
               }

               if (RenderUtils.isHovering(
                  (int)mouseX, (int)mouseY, windowX + windowWidth - 10.0F, windowY + windowHeight - 10.0F, windowX + windowWidth, windowY + windowHeight
               )) {
                  this.SetDragPosition(mouseX, mouseY);
                  this.clickResizeWindow = true;
               }

               if (this.hoveringBooleanValue != null) {
                  this.hoveringBooleanValue.setCurrentValue(!this.hoveringBooleanValue.getCurrentValue());
               }

               if (this.hoveringFloatValue != null) {
                  this.draggingFloatValue = this.hoveringFloatValue;
               }

               if (this.hoveringStringValue != null) {
                  if (this.editingStringValue != null && this.editingStringValue != this.hoveringStringValue) {
                     this.commitEditingString(true);
                  }
                  this.editingStringValue = this.hoveringStringValue;
                  this.editingStringText = this.editingStringValue.getCurrentValue() == null ? "" : this.editingStringValue.getCurrentValue();
               } else if (this.editingStringValue != null) {
                  this.commitEditingString(true);
               }

               if (this.hoveringModeValue != null) {
                  this.hoveringModeValue.setCurrentValue(this.targetModeValueIndex);
                  SmoothAnimationTimer animation = this.valuesAnimation.get(this.hoveringModeValue);
                  animation.value = 0.0F;
                  animation.target = 255.0F;
               }
            }
            if (this.hoveringAddonsValue != null && this.targetAddonIndex != -1) {
               this.hoveringAddonsValue.toggleSelected(this.targetAddonIndex);
               SmoothAnimationTimer animation = this.valuesAnimation.get(this.hoveringAddonsValue);
               animation.value = 0.0F;
               animation.target = 255.0F;
            }
         } else if (mouseButton == 2 && this.hoveringModule != null) {
            this.bindingModule = this.hoveringModule;
         }

         return true;
      } else {
         this.bindingModule.setKey(-mouseButton);
         this.bindingModule = null;
         return true;
      }
   }

   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         this.mouseDown = false;
         this.targetAddonIndex = -1;
      }

      if (this.clickResizeWindow) {
         this.clickResizeWindow = false;
         this.SetDragPosition(-1, -1);
      }

      if (this.clickDragWindow) {
         this.clickDragWindow = false;
         this.SetDragPosition(-1, -1);
      }

      if (this.draggingFloatValue != null) {
         this.draggingFloatValue = null;
      }

      return true;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.editingStringValue != null) {
         if (pKeyCode == InputUtil.GLFW_KEY_ESCAPE || pKeyCode == InputUtil.GLFW_KEY_ENTER || pKeyCode == InputUtil.GLFW_KEY_KP_ENTER) {
            this.commitEditingString(pKeyCode != InputUtil.GLFW_KEY_ESCAPE);
            return true;
         }

         if (pKeyCode == InputUtil.GLFW_KEY_BACKSPACE && !this.editingStringText.isEmpty()) {
            this.editingStringText = this.editingStringText.substring(0, this.editingStringText.length() - 1);
            return true;
         }
      }

      if (this.bindingModule != null) {
         if (pKeyCode == 256) {
            this.bindingModule.setKey(0);
            this.bindingModule = null;
            return true;
         }

         this.bindingModule.setKey(pKeyCode);
         this.bindingModule = null;
      }

      return super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      if (this.editingStringValue != null && !Character.isISOControl(codePoint)) {
         this.editingStringText += codePoint;
         return true;
      }

      return super.charTyped(codePoint, modifiers);
   }

   protected void init() {
      Naven.getInstance().getEventManager().register(this);
      this.valuesAnimation.forEach((value, animation) -> {
         if (value.getValueType() == ValueType.MODE || value.getValueType() == ValueType.ADDONS) {
            animation.value = 0.0F;
            animation.target = 255.0F;
         }
      });
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.selectedCategory == Category.THEME
              && this.bindingModule == null
              && RenderUtils.isHoveringBound((int)mouseX, (int)mouseY, windowX + 5.0F, windowY + 20.0F, this.widthAnimation.value - 10.0F, this.heightAnimation.value - 25.0F)) {
         this.themeMotionY.target = (float)(this.themeMotionY.target + verticalAmount * 24.0);
         return true;
      }

      if (this.bindingModule == null && RenderUtils.isHoveringBound((int)mouseX, (int)mouseY, windowX + 5.0F, windowY + 20.0F, 100.0F, windowHeight - 5.0F)) {
         this.categoryMotionY.target = (float)(this.categoryMotionY.target + verticalAmount * 15.0);
         this.moduleAlphaTimer.reset();
         return true;
      }

      if (this.renderValues != null
              && this.bindingModule == null
              && RenderUtils.isHoveringBound((int)mouseX, (int)mouseY, windowX + 140.0F, windowY + 20.0F, windowWidth - 155.0F, windowHeight - 25.0F)) {
         this.moduleValuesMotionY.target = (float)(this.moduleValuesMotionY.target + verticalAmount * 15.0);
         this.valuesAlphaTimer.reset();
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (mc.currentScreen == this) {
         RenderUtils.drawRoundedRect(e.getStack(), windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, 1073741824);
      }
   }

   public void render(DrawContext g, int mouseX, int mouseY, float pPartialTick) {
      MatrixStack stack = g.getMatrices();
      this.hoveringModule = null;
      this.hoveringTheme = null;
      this.clickReturnModules = this.clickReturnCategories = this.clickOpenCategoryModules = false;
      CustomTextRenderer opensans = Fonts.opensans;
      if (this.selectedCategory == null) {
         this.widthAnimation.target = 100.0F;
         this.heightAnimation.target = 185.0F;
      } else {
         this.widthAnimation.target = windowWidth;
         this.heightAnimation.target = windowHeight;
      }

      this.widthAnimation.update(true);
      this.heightAnimation.update(true);
      RenderUtils.drawRoundedRect(stack, windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, Colors.getColor(0, 0, 0, 40));

      for (Category value : Category.values()) {
         SmoothAnimationTimer xAnimation = this.categoryXAnimation.get(value);
         SmoothAnimationTimer yAnimation = this.categoryYAnimation.get(value);
         if (this.selectedCategory == null) {
            yAnimation.target = 255.0F;
         } else {
            yAnimation.target = 0.0F;
         }

         xAnimation.update(true);
         yAnimation.update(true);
         float height = (float)(value.ordinal() * 25) * (yAnimation.value / 255.0F);
         if (yAnimation.target >= 4.0F) {
            opensans.setAlpha(yAnimation.value / 255.0F);
            Fonts.icons.render(stack, value.getIcon(), (double)(windowX + 8.0F + xAnimation.value), (double)(windowY + 41.0F + height), Color.WHITE, true, 0.4);
            opensans.render(
               stack, value.getDisplayName(), (double)(windowX + 25.0F + xAnimation.value), (double)(windowY + 40.0F + height), Color.WHITE, true, 0.4
            );
            opensans.setAlpha(1.0F);
         }

         boolean hovering = RenderUtils.isHovering(mouseX, mouseY, windowX, windowY + 40.0F + height, windowX + 100.0F, windowY + 40.0F + height + 20.0F);
         if (hovering) {
            xAnimation.target = 5.0F;
         } else {
            xAnimation.target = 0.0F;
         }

         if (yAnimation.value >= 250.0F && hovering) {
            this.hoveringCategory = value;
            this.clickOpenCategoryModules = true;
         }
      }

      this.titleAnimation.update(true);
      this.titleHoverAnimation.update(true);
      if (this.titleAnimation.value > 5.0F) {
         opensans.setAlpha(this.titleAnimation.value / 255.0F);
         opensans.render(
            stack, this.titleDisplayName, (double)(windowX + 6.0F + this.titleHoverAnimation.value), (double)(windowY + 3.0F), Color.WHITE, true, 0.4
         );
      }

      opensans.setAlpha((255.0F - this.titleAnimation.value) / 255.0F);
      opensans.render(stack, Naven.CLIENT_DISPLAY_NAME, (double)(windowX + 50.0F - opensans.getWidth(Naven.CLIENT_DISPLAY_NAME, 0.75) / 2.0F), (double)(windowY + 5.0F), Color.WHITE, true, 0.75);
      opensans.setAlpha(1.0F);
      if (this.selectedCategory != null) {
         this.titleAnimation.target = 255.0F;
         this.titleDisplayName = "< "
            + this.selectedCategory.getDisplayName()
            + (this.selectedModule != null ? " / " + this.selectedModule.getName() + " - " + this.selectedModule.getDescription() : "");
         this.hoveringBack = RenderUtils.isHovering(
            mouseX,
            mouseY,
            windowX + 8.0F,
            windowY + 5.0F,
            windowX + 5.0F + opensans.getWidth(this.titleDisplayName, 0.4),
            (float)((double)(windowY + 5.0F) + opensans.getHeight(true, 0.4F))
         );
         if (this.hoveringBack && !this.clickResizeWindow && !this.clickDragWindow) {
            this.titleHoverAnimation.target = -2.0F;
         } else {
            this.titleHoverAnimation.target = 0.0F;
         }

         if (this.categoryMotionY.target < -this.finalModuleHeight) {
            this.categoryMotionY.target = -this.finalModuleHeight;
         }

         if (this.categoryMotionY.target > 0.0F) {
            this.categoryMotionY.target = 0.0F;
         }

         this.categoryMotionY.update(true);
      } else {
         this.titleAnimation.target = 4.0F;
      }

      StencilUtils.write(false);
      RenderUtils.fill(stack, windowX, windowY + 20.0F, windowX + this.widthAnimation.value, windowY + this.heightAnimation.value - 5.0F, Integer.MIN_VALUE);
      StencilUtils.erase(true);
      if (this.selectedCategory == Category.THEME) {
         this.categoryModules = null;
         this.renderThemePage(stack, mouseX, mouseY, opensans);
      } else {
      List<Module> inList = this.modules.get(this.selectedCategory);
      if (inList != null) {
         this.categoryModules = inList;
         this.moduleSwapAnimation.target = 255.0F;
      } else {
         this.moduleSwapAnimation.target = 5.0F;
      }

      this.moduleSwapAnimation.update(true);
      if (inList == null && this.moduleSwapAnimation.value < 8.0F) {
         this.categoryModules = null;
      }

      if (this.categoryModules != null) {
         float renderModuleHeight = 0.0F;
         this.minCatY = windowHeight - 25.0F;

         for (Module categoryModule : this.categoryModules) {
            boolean isHovering = RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 5.0F, windowY + 20.0F, 120.0F, windowHeight - 25.0F)
               && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F)
               && this.moduleSwapAnimation.value > 250.0F
               && this.bindingModule == null;
            SmoothAnimationTimer moduleToggleAnimation = this.modulesToggleAnimation.get(categoryModule);
            if (categoryModule.isEnabled()) {
               moduleToggleAnimation.target = this.moduleSwapAnimation.value;
            } else {
               moduleToggleAnimation.target = 6.0F;
            }

            moduleToggleAnimation.update(true);
            int alpha = (int)this.moduleSwapAnimation.value;
            int enabledColor = Colors.getColor(54, 98, 236, (int)moduleToggleAnimation.value);
            int disabledColor = Colors.getColor(25, 25, 25, alpha);
            RenderUtils.drawRoundedRect(
               stack, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, disabledColor
            );
            RenderUtils.drawRoundedRect(
               stack, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, enabledColor
            );
            SmoothAnimationTimer moduleAnimation = this.modulesAnimation.get(categoryModule);
            if (isHovering) {
               moduleAnimation.target = 150.0F;
               this.hoveringModule = categoryModule;
            } else {
               moduleAnimation.target = 5.0F;
            }

            moduleAnimation.update(true);
            int hoveringColor = Colors.getColor(255, 255, 255, (int)moduleAnimation.value / 3);
            RenderUtils.drawRoundedRect(
               stack, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, hoveringColor
            );
            opensans.setAlpha((float)alpha / 255.0F);
            opensans.render(
               stack,
               categoryModule.getName(),
               (double)(windowX + 13.0F),
               (double)(windowY + 25.0F + renderModuleHeight + this.categoryMotionY.value),
               Color.WHITE,
               true,
               0.4
            );
            opensans.setAlpha(1.0F);
            renderModuleHeight += 30.0F;
         }

         this.finalModuleHeight = renderModuleHeight + 20.0F - windowHeight;
         this.minCatY -= renderModuleHeight - 5.0F;
         float totalHeight = this.finalModuleHeight + windowHeight;
         if (totalHeight > windowHeight - 25.0F) {
            this.moduleAlphaAnimation.update(true);
            if (this.moduleAlphaTimer.delay(1000.0)) {
               this.moduleAlphaAnimation.target = 0.0F;
            } else {
               this.moduleAlphaAnimation.target = 255.0F;
            }

            float viewable = windowHeight - 25.0F;
            float progress = (float)MathUtils.clamp((double)(-this.categoryMotionY.value / -this.minCatY), 0.0, 1.0);
            float ratio = viewable / totalHeight * viewable;
            float barHeight = Math.max(ratio, 20.0F);
            float position = progress * (viewable - barHeight);
            RenderUtils.drawRoundedRect(
               stack,
               windowX + 127.0F,
               windowY + 20.0F + position,
               3.0F,
               barHeight,
               1.5F,
               RenderUtils.reAlpha(3630060, this.moduleAlphaAnimation.value / 255.0F)
            );
         }
      }

      if (this.renderValues != null) {
         boolean isValueInBound = RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F, windowY + 20.0F, windowWidth - 155.0F, windowHeight - 25.0F);
         float motion = this.moduleValuesMotionY.value;
         if (this.moduleValuesMotionY.target < -this.finalValueHeight) {
            this.moduleValuesMotionY.target = -this.finalValueHeight;
         }

         if (this.moduleValuesMotionY.target > 0.0F) {
            this.moduleValuesMotionY.target = 0.0F;
         }

         this.moduleValuesMotionY.update(true);
         float x = 0.0F;
         float valueHeight = 0.0F;
         this.minValueY = windowHeight - 25.0F;
         this.hoveringBooleanValue = null;

         for (Value value : this.renderValues) {
            if (value.isVisible() && value.getValueType() == ValueType.BOOLEAN) {
               BooleanValue booleanValue = value.getBooleanValue();
               SmoothAnimationTimer animation = this.valuesAnimation.get(booleanValue);
               if (booleanValue.getCurrentValue()) {
                  animation.target = 255.0F;
               } else {
                  animation.target = 0.0F;
               }

               animation.update(true);
               float scale = 0.4F;
               CustomTextRenderer font = Fonts.opensans;
               int heightOffset = 0;
               if (StringUtils.containChinese(value.getName())) {
                  font = Fonts.harmony;
                  scale = 0.325F;
                  heightOffset = 2;
               }

               float currentLength = font.getWidth(value.getName(), (double)scale) + 23.0F;
               if (x + currentLength + 20.0F > windowWidth - 155.0F) {
                  x = 0.0F;
                  valueHeight += 20.0F;
               }

               if (isValueInBound
                  && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 130.0F + x, windowY + valueHeight + motion + 20.0F, currentLength, 13.0F)) {
                  this.hoveringBooleanValue = booleanValue;
               }

               int enabledColor = Colors.getColor(54, 98, 236, (int)animation.value);
               RenderUtils.drawRoundedRect(
                  stack, windowX + 140.0F + x, windowY + valueHeight + motion + 20.0F, 12.0F, 12.0F, 2.0F, Colors.getColor(0, 0, 0, 150)
               );
               RenderUtils.drawRoundedRect(stack, windowX + 142.0F + x, windowY + valueHeight + motion + 22.0F, 8.0F, 8.0F, 2.0F, enabledColor);
               font.render(
                  stack,
                  value.getName(),
                  (double)(windowX + 155.0F + x),
                  (double)(windowY + valueHeight + motion + 19.0F + (float)heightOffset),
                  Color.WHITE,
                  true,
                  (double)scale
               );
               x += currentLength;
            }
         }

         valueHeight += 10.0F;

         this.hoveringFloatValue = null;

         for (Value valuex : this.renderValues) {
            if (valuex.isVisible() && valuex.getValueType() == ValueType.FLOAT) {
               FloatValue floatValue = valuex.getFloatValue();
               SmoothAnimationTimer animationx = this.valuesAnimation.get(floatValue);
               if (isValueInBound
                  && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F, windowY + valueHeight + motion + 39.5F, windowWidth - 155.0F, 10.0F)) {
                  this.hoveringFloatValue = floatValue;
               }

               opensans.render(stack, valuex.getName(), (double)(windowX + 140.0F), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
               String currentValue = (float)Math.round(floatValue.getCurrentValue() * 100.0F) / 100.0F + " / " + floatValue.getMaxValue();
               opensans.render(
                  stack,
                  currentValue,
                  (double)(windowX + windowWidth - opensans.getWidth(currentValue, 0.4) - 15.0F),
                  (double)(windowY + valueHeight + motion + 25.0F),
                  Color.WHITE,
                  true,
                  0.4
               );
               float stage = (floatValue.getCurrentValue() - floatValue.getMinValue()) / (floatValue.getMaxValue() - floatValue.getMinValue());
               int enabledColor = Colors.getColor(54, 98, 236, 255);
               RenderUtils.drawRoundedRect(
                  stack, windowX + 140.0F, windowY + valueHeight + motion + 42.0F, windowWidth - 155.0F, 5.0F, 3.0F, Colors.getColor(0, 0, 0, 150)
               );
               animationx.target = (windowWidth - 155.0F) * stage;
               animationx.update(true);
               RenderUtils.drawRoundedRect(stack, windowX + 140.0F, windowY + valueHeight + motion + 42.0F, animationx.value, 5.0F, 3.0F, enabledColor);
               RenderUtils.drawRoundedRect(
                  stack, windowX + 135.0F + animationx.value, windowY + valueHeight + motion + 39.5F, 10.0F, 10.0F, 5.0F, Colors.getColor(255, 255, 255, 255)
               );
               valueHeight += 25.0F;
            }
         }

         this.hoveringAddonsValue = null;
         this.hoveringStringValue = null;

         for (Value valueString : this.renderValues) {
            if (valueString.isVisible() && valueString.getValueType() == ValueType.STRING) {
               StringValue stringValue = valueString.getStringValue();
               float baseY = windowY + valueHeight + motion + 25.0F;
               float lineY = baseY + 20.0F;
               float fieldWidth = windowWidth - 155.0F;
               if (isValueInBound
                  && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F, lineY - 13.0F, fieldWidth, 16.0F)) {
                  this.hoveringStringValue = stringValue;
               }

               opensans.render(stack, valueString.getName(), (double)(windowX + 140.0F), (double)baseY, Color.WHITE, true, 0.4);
               String currentValue = this.editingStringValue == stringValue
                  ? this.editingStringText + ((System.currentTimeMillis() / 450L) % 2L == 0L ? "_" : "")
                  : stringValue.getCurrentValue();
               if (currentValue == null) {
                  currentValue = "";
               }

               int lineColor = this.editingStringValue == stringValue || this.hoveringStringValue == stringValue
                  ? Colors.getColor(54, 98, 236, 255)
                  : Colors.getColor(0, 0, 0, 160);
               RenderUtils.drawRoundedRect(stack, windowX + 140.0F, lineY, fieldWidth, 3.0F, 2.0F, Colors.getColor(0, 0, 0, 150));
               RenderUtils.drawRoundedRect(stack, windowX + 140.0F, lineY, fieldWidth, 3.0F, 2.0F, lineColor);
               opensans.render(
                  stack,
                  trimToWidth(opensans, currentValue, fieldWidth - 6.0F, 0.4),
                  (double)(windowX + 142.0F),
                  (double)(lineY - 13.0F),
                  this.editingStringValue == stringValue ? Color.WHITE : new Color(190, 190, 190),
                  true,
                  0.4
               );
               valueHeight += 35.0F;
            }
         }

         for (Value valuexx : this.renderValues) {
            if (valuexx.isVisible() && valuexx.getValueType() == ValueType.ADDONS) {
               AddonsValue addonsValue = valuexx.getAddonsValue();
               SmoothAnimationTimer animationx = this.valuesAnimation.get(addonsValue);
               animationx.update(true);
               opensans.render(stack, valuexx.getName(), (double)(windowX + 140.0F), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
               x = 0.0F;
               valueHeight += 15.0F;

               for (int addonIndex = 0; addonIndex < addonsValue.getValues().length; addonIndex++) {
                  String addon = addonsValue.getValues()[addonIndex];
                  float currentLengthx = opensans.getWidth(addon, 0.4) + 20.0F;
                  if (x + currentLengthx + 20.0F > windowWidth - 155.0F) {
                     x = 0.0F;
                     valueHeight += 20.0F;
                  }

                  if (isValueInBound
                          && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F + x, windowY + valueHeight + motion + 25.0F, currentLengthx, 13.0F)) {
                     this.hoveringAddonsValue = addonsValue;
                     this.targetAddonIndex = addonIndex;  // 添加这一行
                  }

                  boolean isSelected = addonsValue.isSelected(addonIndex);
                  int enabledColor = Colors.getColor(54, 98, 236, isSelected ? (int)animationx.value : 10);
                  RenderUtils.drawRoundedRect(
                          stack, windowX + 140.0F + x, windowY + valueHeight + motion + 27.0F, 10.0F, 10.0F, 5.0F, Colors.getColor(0, 0, 0, 150)
                  );
                  RenderUtils.drawRoundedRect(stack, windowX + 141.0F + x, windowY + valueHeight + motion + 28.0F, 8.0F, 8.0F, 5.0F, enabledColor);
                  opensans.render(stack, addon, (double)(windowX + 152.0F + x), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
                  x += currentLengthx;
               }

               valueHeight += 20.0F;
            }
         }

         this.hoveringModeValue = null;

         for (Value valuexx : this.renderValues) {
            if (valuexx.isVisible() && valuexx.getValueType() == ValueType.MODE) {
               ModeValue modeValue = valuexx.getModeValue();
               SmoothAnimationTimer animationx = this.valuesAnimation.get(modeValue);
               animationx.update(true);
               opensans.render(stack, valuexx.getName(), (double)(windowX + 140.0F), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
               x = 0.0F;
               valueHeight += 15.0F;

               for (int modeIndex = 0; modeIndex < modeValue.getValues().length; modeIndex++) {
                  String mode = modeValue.getValues()[modeIndex];
                  float currentLengthx = opensans.getWidth(mode, 0.4) + 20.0F;
                  if (x + currentLengthx + 20.0F > windowWidth - 155.0F) {
                     x = 0.0F;
                     valueHeight += 20.0F;
                  }

                  if (isValueInBound
                     && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F + x, windowY + valueHeight + motion + 25.0F, currentLengthx, 13.0F)) {
                     this.hoveringModeValue = modeValue;
                     this.targetModeValueIndex = modeIndex;
                  }

                  int enabledColor = Colors.getColor(54, 98, 236, modeValue.isCurrentMode(mode) ? (int)animationx.value : 10);
                  RenderUtils.drawRoundedRect(
                     stack, windowX + 140.0F + x, windowY + valueHeight + motion + 27.0F, 10.0F, 10.0F, 5.0F, Colors.getColor(0, 0, 0, 150)
                  );
                  RenderUtils.drawRoundedRect(stack, windowX + 141.0F + x, windowY + valueHeight + motion + 28.0F, 8.0F, 8.0F, 5.0F, enabledColor);
                  opensans.render(stack, mode, (double)(windowX + 152.0F + x), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
                  x += currentLengthx;
               }

               valueHeight += 20.0F;
            }
         }

         this.finalValueHeight = valueHeight - windowHeight + 25.0F;
         this.minValueY -= valueHeight;
         float valTotalHeight = this.finalValueHeight + windowHeight;
         if (valTotalHeight > windowHeight - 25.0F) {
            this.valuesAlphaAnimation.update(true);
            if (this.valuesAlphaTimer.delay(1000.0)) {
               this.valuesAlphaAnimation.target = 0.0F;
            } else {
               this.valuesAlphaAnimation.target = 255.0F;
            }

            float viewable = windowHeight - 25.0F;
            float progress = (float)MathUtils.clamp((double)(-this.moduleValuesMotionY.value / -this.minValueY), 0.0, 1.0);
            float ratio = viewable / valTotalHeight * viewable;
            float barHeight = Math.max(ratio, 20.0F);
            float position = progress * (viewable - barHeight);
            RenderUtils.drawRoundedRect(
               stack,
               windowX + windowWidth - 8.0F,
               windowY + 20.0F + position,
               3.0F,
               barHeight,
               1.5F,
               RenderUtils.reAlpha(3630060, this.valuesAlphaAnimation.value / 255.0F)
            );
         }
      }
      }

      if (this.draggingFloatValue != null) {
         float stage = ((float)mouseX - windowX - 140.0F) / (windowWidth - 160.0F);
         float valuexxx = this.draggingFloatValue.getMinValue() + (this.draggingFloatValue.getMaxValue() - this.draggingFloatValue.getMinValue()) * stage;
         if (valuexxx < this.draggingFloatValue.getMinValue()) {
            valuexxx = this.draggingFloatValue.getMinValue();
         }

         if (valuexxx > this.draggingFloatValue.getMaxValue()) {
            valuexxx = this.draggingFloatValue.getMaxValue();
         }

         valuexxx = (float)Math.round(valuexxx / this.draggingFloatValue.getStep()) * this.draggingFloatValue.getStep();
         this.draggingFloatValue.setCurrentValue(valuexxx);
      }

      if (this.clickDragWindow && this.mouseDown) {
         windowX = windowX + (float)(mouseX - this.dragMousePosition[0]);
         windowY = windowY + (float)(mouseY - this.dragMousePosition[1]);
         this.SetDragPosition(mouseX, mouseY);
      }

      if ((this.categoryModules != null || this.selectedCategory == Category.THEME) && !this.hoveringBack && this.clickResizeWindow && this.mouseDown) {
         windowWidth = windowWidth + (float)(mouseX - this.dragMousePosition[0]);
         windowHeight = windowHeight + (float)(mouseY - this.dragMousePosition[1]);
         if (windowWidth < 500.0F) {
            windowWidth = 500.0F;
         }

         if (windowHeight < 300.0F) {
            windowHeight = 300.0F;
         }

         this.SetDragPosition(mouseX, mouseY);
      }

      if (this.bindingModule != null) {
         this.bindingAnimation.target = 250.0F;
         this.bindingModuleName = this.bindingModule.getName();
      } else {
         this.bindingAnimation.target = 5.0F;
      }

      this.bindingAnimation.update(true);
      if (this.bindingAnimation.value > 6.0F) {
         RenderUtils.fill(
            stack,
            windowX,
            windowY,
            windowX + this.widthAnimation.value,
            windowY + this.heightAnimation.value,
            Colors.getColor(0, 0, 0, (int)this.bindingAnimation.value / 2)
         );
         opensans.setAlpha(this.bindingAnimation.value / 255.0F);
         String line1 = "Press a key to bind " + this.bindingModuleName;
         String line2 = "(Press ESC to remove/cancel key bind)";
         opensans.render(
            stack,
            line1,
            (double)(windowX + this.widthAnimation.value / 2.0F - opensans.getWidth(line1, 0.6) / 2.0F),
            (double)windowY + ((double)this.heightAnimation.value - opensans.getHeight(true, 0.6)) / 2.0 - 10.0,
            Color.WHITE,
            true,
            0.6
         );
         opensans.render(
            stack,
            line2,
            (double)(windowX + this.widthAnimation.value / 2.0F - opensans.getWidth(line2, 0.4) / 2.0F),
            (double)windowY + ((double)this.heightAnimation.value - opensans.getHeight(true, 0.4)) / 2.0 + 15.0,
            Color.WHITE,
            true,
            0.4
         );
         opensans.setAlpha(1.0F);
      }

      if (this.selectedCategory != null) {
         Fonts.icons.setAlpha(0.5F);
         Fonts.icons
            .render(
               stack,
               FontIcons.RESIZE,
               (double)(windowX + this.widthAnimation.value - 10.0F),
               (double)(windowY + this.heightAnimation.value - 10.0F),
               Color.WHITE,
               false,
               0.3
            );
         Fonts.icons.setAlpha(1.0F);
      }

      StencilUtils.dispose();
   }

   private void renderThemePage(MatrixStack stack, int mouseX, int mouseY, CustomTextRenderer font) {
      ThemeStyle selected = Naven.getInstance().getThemeManager().getCurrentStyle();
      float contentX = windowX + 10.0F;
      float contentY = windowY + 26.0F;
      float contentWidth = Math.max(80.0F, this.widthAnimation.value - 20.0F);
      float viewHeight = Math.max(40.0F, this.heightAnimation.value - 36.0F);
      float gap = 10.0F;
      int columns = Math.max(1, Math.min(4, (int)((contentWidth + gap) / 150.0F)));
      float cardWidth = (contentWidth - gap * (columns - 1)) / columns;
      float cardHeight = 54.0F;
      int rows = (int)Math.ceil((double)ThemeStyle.VALUES.length / (double)columns);
      float totalHeight = rows * cardHeight + Math.max(0, rows - 1) * gap;
      float minScroll = Math.min(0.0F, viewHeight - totalHeight - 4.0F);

      if (this.themeMotionY.target < minScroll) {
         this.themeMotionY.target = minScroll;
      }

      if (this.themeMotionY.target > 0.0F) {
         this.themeMotionY.target = 0.0F;
      }

      this.themeMotionY.update(true);

      for (int i = 0; i < ThemeStyle.VALUES.length; i++) {
         ThemeStyle style = ThemeStyle.VALUES[i];
         int column = i % columns;
         int row = i / columns;
         float x = contentX + column * (cardWidth + gap);
         float y = contentY + this.themeMotionY.value + row * (cardHeight + gap);

         if (y + cardHeight < contentY - 6.0F || y > contentY + viewHeight + 6.0F) {
            continue;
         }

         boolean hovered = RenderUtils.isHovering(mouseX, mouseY, x, y, x + cardWidth, y + cardHeight)
                 && RenderUtils.isHovering(mouseX, mouseY, contentX, contentY, contentX + contentWidth, contentY + viewHeight)
                 && this.bindingModule == null;
         if (hovered) {
            this.hoveringTheme = style;
         }

         SmoothAnimationTimer animation = this.themeAnimations.get(style);
         animation.target = style == selected ? 255.0F : hovered ? 120.0F : 0.0F;
         animation.update(true);

         if (animation.value > 1.0F) {
            int outlineColor = style == selected ? style.getColor(i * 40, 1.0F) : 0xFFFFFF;
            RenderUtils.drawRoundedRect(stack, x - 1.0F, y - 1.0F, cardWidth + 2.0F, cardHeight + 2.0F, 6.0F, RenderUtils.reAlpha(outlineColor, animation.value / 255.0F));
         }

         RenderUtils.drawRoundedRect(stack, x, y, cardWidth, cardHeight, 5.0F, Colors.getColor(10, 12, 18, 230));
         this.drawThemeGradient(stack, x, y, cardWidth, 34.0F, style);
         RenderUtils.fillBound(stack, x, y + 34.0F, cardWidth, 1.0F, Colors.getColor(255, 255, 255, 12));

         String name = trimToWidth(font, style.getName(), cardWidth - 10.0F, 0.38);
         float textX = x + (cardWidth - font.getWidth(name, 0.38)) / 2.0F;
         font.render(stack, name, (double)textX, (double)(y + 38.0F), Color.WHITE, true, 0.38);
      }

      if (totalHeight > viewHeight) {
         float progress = minScroll == 0.0F ? 0.0F : this.themeMotionY.value / minScroll;
         float barHeight = Math.max(20.0F, viewHeight * (viewHeight / totalHeight));
         float barY = contentY + progress * (viewHeight - barHeight);
         RenderUtils.drawRoundedRect(stack, contentX + contentWidth - 4.0F, contentY, 3.0F, viewHeight, 1.5F, Colors.getColor(255, 255, 255, 24));
         RenderUtils.drawRoundedRect(stack, contentX + contentWidth - 4.0F, barY, 3.0F, barHeight, 1.5F, Colors.getColor(255, 255, 255, 120));
      }
   }

   private void drawThemeGradient(MatrixStack stack, float x, float y, float width, float height, ThemeStyle style) {
      int steps = Math.max(8, Math.min(36, (int)(width / 4.0F)));
      for (int i = 0; i < steps; i++) {
         float start = (float)i / (float)steps;
         float end = (float)(i + 1) / (float)steps;
         int color = 0xFF000000 | style.sample((start + end) * 0.5F);
         RenderUtils.fillBound(stack, x + width * start, y, width * (end - start) + 0.75F, height, color);
      }
   }

   public void SetDragPosition(double x, double y) {
      this.SetDragPosition((int)x, (int)y);
   }

   public void SetDragPosition(int x, int y) {
      this.dragMousePosition[0] = x;
      this.dragMousePosition[1] = y;
   }

   private void commitEditingString(boolean save) {
      if (this.editingStringValue != null && save) {
         this.editingStringValue.setCurrentValue(this.editingStringText);
         Naven.getInstance().getFileManager().save();
      }

      this.editingStringValue = null;
      this.editingStringText = "";
   }

   private String trimToWidth(CustomTextRenderer font, String text, float maxWidth, double scale) {
      if (text == null || font.getWidth(text, scale) <= maxWidth) {
         return text == null ? "" : text;
      }

      String suffix = "...";
      int end = text.length();
      while (end > 0 && font.getWidth(text.substring(0, end) + suffix, scale) > maxWidth) {
         end--;
      }

      return text.substring(0, end) + suffix;
   }
}
