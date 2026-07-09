package awa.qwq.ovo.Naven.ui;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.FontIcons;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.SmoothAnimationTimer;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.Value;
import awa.qwq.ovo.Naven.values.ValueType;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import awa.qwq.ovo.Naven.values.impl.StringValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class AkarinClickGUI extends Screen {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float PANEL_RADIUS = 8.0F;
   private static final float CARD_RADIUS = 4.0F;
   private static final float CONTROL_RADIUS = 3.0F;
   private static final float HEADER_HEIGHT = 44.0F;
   private static final float CARD_GAP = 8.0F;
   private static final float COLUMN_GAP = 12.0F;
   private static final float MODULE_HEADER = 34.0F;
   private static final float SETTING_HEIGHT = 30.0F;
   private static final int SURFACE = 0xE8070809;
   private static final int CARD = 0xE81B1B1D;
   private static final int CARD_HIGH = 0xF0232326;
   private static final int TRACK = 0xFF333335;
   private static final int TEXT = 0xFFEAE8EC;
   private static final int MUTED = 0xFFAAA6AD;
   private static Category rememberedCategory = Category.VISUAL;
   private static float rememberedScroll;

   private final SmoothAnimationTimer scrollAnimation = new SmoothAnimationTimer(rememberedScroll, rememberedScroll, 0.28F);
   private final SmoothAnimationTimer pageAnimation = new SmoothAnimationTimer(1.0F, 1.0F, 0.12F);
   private final Map<Module, SmoothAnimationTimer> moduleAnimations = new IdentityHashMap<>();
   private final Map<ModeValue, SmoothAnimationTimer> modeAnimations = new IdentityHashMap<>();
   private final Map<Category, SmoothAnimationTimer> categoryAnimations = new HashMap<>();
   private final Map<Module, SmoothAnimationTimer> switchAnimations = new IdentityHashMap<>();
   private final Map<FloatValue, SmoothAnimationTimer> numberAnimations = new IdentityHashMap<>();

   private Category selectedCategory = rememberedCategory;
   private Category outgoingCategory;
   private Module bindingModule;
   private FloatValue draggingFloatValue;
   private Rect draggingFloatRow;
   private ModeValue expandedMode;
   private StringValue editingString;
   private String editingStringText = "";
   private float scroll = rememberedScroll;
   private float outgoingScroll;
   private int pageDirection = 1;
   private Rect panel = new Rect(0.0F, 0.0F, 0.0F, 0.0F);
   private Rect viewport = new Rect(0.0F, 0.0F, 0.0F, 0.0F);
   private ModeOverlay modeOverlay;

   public AkarinClickGUI() {
      super(Text.of("Akarin"));
   }

   @Override
   protected void init() {
      this.scroll = rememberedScroll;
      this.scrollAnimation.value = this.scroll;
      this.scrollAnimation.target = this.scroll;
      this.pageAnimation.value = 1.0F;
      this.pageAnimation.target = 1.0F;
      this.outgoingCategory = null;
      this.bindingModule = null;
      this.draggingFloatValue = null;
      this.draggingFloatRow = null;
      this.expandedMode = null;
      this.editingString = null;
      this.moduleAnimations.clear();
      this.modeAnimations.clear();
      this.categoryAnimations.clear();
      this.switchAnimations.clear();
      this.numberAnimations.clear();
   }

   @Override
   public void close() {
      rememberState();
      Naven.getInstance().getFileManager().save();
      super.close();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
      computeLayout();
      renderBackdrop(graphics);
      renderPanel(graphics, mouseX, mouseY, 1.0F);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.bindingModule != null) {
         if (button == 3 || button == 4) {
            this.bindingModule.setKey(-button);
            this.bindingModule = null;
            persist();
            return true;
         }
         return true;
      }

      if (clickCategory(mouseX, mouseY, button)) {
         return true;
      }
      if (handleExpandedModeClick(mouseX, mouseY, button)) {
         rememberState();
         return true;
      }
      return handleModuleClick(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingFloatValue = null;
         this.draggingFloatRow = null;
      }
      return true;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingFloatValue != null && this.draggingFloatRow != null) {
         updateFloat(this.draggingFloatValue, (float)mouseX, this.draggingFloatRow);
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.viewport.contains(mouseX, mouseY) || this.panel.contains(mouseX, mouseY)) {
         this.scroll -= (float)verticalAmount * 28.0F;
         rememberState();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.bindingModule != null) {
         if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            this.bindingModule.setKey(0);
         } else {
            this.bindingModule.setKey(keyCode);
         }
         this.bindingModule = null;
         persist();
         return true;
      }

      if (this.editingString != null) {
         if (keyCode == InputUtil.GLFW_KEY_ESCAPE || keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_KP_ENTER) {
            commitEditingString(keyCode != InputUtil.GLFW_KEY_ESCAPE);
            return true;
         }
         if (keyCode == InputUtil.GLFW_KEY_BACKSPACE && !this.editingStringText.isEmpty()) {
            this.editingStringText = this.editingStringText.substring(0, this.editingStringText.length() - 1);
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      if (this.editingString != null && !Character.isISOControl(codePoint)) {
         this.editingStringText += codePoint;
         return true;
      }
      return super.charTyped(codePoint, modifiers);
   }

   private void renderPanel(DrawContext graphics, int mouseX, int mouseY, float alpha) {
      MatrixStack stack = graphics.getMatrices();
      roundedRect(stack, this.panel.x, this.panel.y, this.panel.width, this.panel.height, PANEL_RADIUS, alpha(SURFACE, alpha));
      rect(stack, this.panel.x, this.panel.y + HEADER_HEIGHT - 1.0F, this.panel.width, 1.0F, alpha(0xFF222226, alpha));
      renderTabs(graphics, mouseX, mouseY, alpha);
      updateContentAnimations();

      float contentHeight = contentHeight(this.selectedCategory);
      float maxScroll = Math.max(0.0F, contentHeight - this.viewport.height);
      this.scroll = clamp(this.scroll, 0.0F, maxScroll);
      this.scrollAnimation.target = this.scroll;
      this.scrollAnimation.update(true);
      float visibleScroll = clamp(this.scrollAnimation.value, 0.0F, maxScroll);
      this.pageAnimation.target = 1.0F;
      this.pageAnimation.update(true);

      this.modeOverlay = null;
      float page = easeOut(this.pageAnimation.value);
      float pageSlide = pageSlideDistance();
      graphics.enableScissor(Math.round(this.viewport.x), Math.round(this.viewport.y), Math.round(this.viewport.right()), Math.round(this.viewport.bottom()));
      if (this.outgoingCategory != null && this.outgoingCategory != this.selectedCategory && page < 0.999F) {
         renderCards(graphics, this.outgoingCategory, mouseX, mouseY, alpha, this.outgoingScroll, -page * pageSlide * this.pageDirection);
      }
      renderCards(graphics, this.selectedCategory, mouseX, mouseY, alpha, visibleScroll, (1.0F - page) * pageSlide * this.pageDirection);
      graphics.disableScissor();

      renderScrollbar(stack, visibleScroll, maxScroll, contentHeight, alpha);
      renderModeOverlay(graphics);
      renderBindingOverlay(graphics, alpha);

      if (Math.abs(this.pageAnimation.value - this.pageAnimation.target) < 0.001F) {
         this.outgoingCategory = null;
      }
   }

   private void renderTabs(DrawContext graphics, int mouseX, int mouseY, float alpha) {
      MatrixStack stack = graphics.getMatrices();
      String icon = FontIcons.CLIENT;
      float iconScale = 0.48F;
      float iconWidth = Fonts.icons.getWidth(icon, iconScale);
      float iconHeight = (float)Fonts.icons.getHeight(false, iconScale);
      Fonts.icons.render(
              stack,
              icon,
              this.panel.x + 22.0F - iconWidth * 0.5F,
              this.panel.y + 19.0F - iconHeight * 0.5F,
              new Color(alpha(0xFFFFFFFF, alpha), true),
              true,
              iconScale
      );
      roundedOutline(stack, this.panel.x + 13.0F, this.panel.y + 10.0F, 18.0F, 18.0F, 4.0F, alpha(0x44FFFFFF, alpha));

      float x = tabsStartX();
      for (Category category : Category.values()) {
         float width = tabWidth(category);
         Rect tab = new Rect(x, this.panel.y + 8.0F, width, 27.0F);
         boolean selected = category == this.selectedCategory;
         boolean hovered = tab.contains(mouseX, mouseY);
         SmoothAnimationTimer timer = categoryAnimation(category);
         timer.target = selected ? 1.0F : hovered ? 0.45F : 0.0F;
         timer.update(true);
         float progress = timer.value;
         if (selected || hovered) {
            roundedRect(stack, tab.x, tab.y, tab.width, tab.height, 4.0F, alpha(mix(0x002A2A2D, 0xFF2A2A2D, progress), alpha));
         }
         int textColor = mix(MUTED, TEXT, progress);
         drawCenteredText(stack, category.getDisplayName(), tab, textColor, alpha, 0.36F);
         x += width + 2.0F;
      }
   }

   private void renderCards(DrawContext graphics, Category category, int mouseX, int mouseY, float alpha, float visibleScroll, float xOffset) {
      float columnWidth = (this.viewport.width - COLUMN_GAP - 6.0F) * 0.5F;
      float leftY = this.viewport.y - visibleScroll;
      float rightY = leftY;

      graphics.getMatrices().push();
      graphics.getMatrices().translate(xOffset, 0.0F, 0.0F);
      for (Module module : modules(category)) {
         float cardHeight = moduleHeight(module);
         boolean left = leftY <= rightY;
         float x = left ? this.viewport.x : this.viewport.x + columnWidth + COLUMN_GAP;
         float y = left ? leftY : rightY;
         renderModuleCard(graphics, module, new Rect(x, y, columnWidth, cardHeight), mouseX - xOffset, mouseY, alpha, xOffset);
         if (left) {
            leftY += cardHeight + CARD_GAP;
         } else {
            rightY += cardHeight + CARD_GAP;
         }
      }
      graphics.getMatrices().pop();
   }

   private void renderModuleCard(DrawContext graphics, Module module, Rect card, float mouseX, float mouseY, float alpha, float xOffset) {
      MatrixStack stack = graphics.getMatrices();
      SmoothAnimationTimer expandTimer = moduleAnimation(module);
      float expand = easeOut(expandTimer.value);
      boolean hovered = card.contains(mouseX, mouseY);
      roundedRect(stack, card.x, card.y, card.width, card.height, CARD_RADIUS, alpha(hovered ? CARD_HIGH : CARD, alpha));
      roundedOutline(stack, card.x, card.y, card.width, card.height, CARD_RADIUS, alpha(0x22FFFFFF, alpha));

      Rect header = new Rect(card.x, card.y, card.width, MODULE_HEADER);
      drawTextInRect(stack, module.getPrettyName(), new Rect(header.x + 12.0F, header.y, header.width - 88.0F, header.height), TEXT, alpha, 0.36F);
      String key = compactKeyName(module.getKey());
      if (!"NONE".equals(key)) {
         drawTextInRect(stack, key, new Rect(header.x + Math.min(header.width - 116.0F, 72.0F), header.y, 38.0F, header.height), MUTED, alpha, 0.30F);
      }
      renderSwitch(stack, module, moduleSwitch(header), alpha);

      if (expand <= 0.01F) {
         return;
      }

      int scissorX1 = Math.round(Math.max(card.x + xOffset, this.viewport.x));
      int scissorY1 = Math.round(Math.max(card.y + MODULE_HEADER, this.viewport.y));
      int scissorX2 = Math.round(Math.min(card.right() + xOffset, this.viewport.right()));
      int scissorY2 = Math.round(Math.min(card.bottom(), this.viewport.bottom()));
      if (scissorX2 <= scissorX1 || scissorY2 <= scissorY1) {
         return;
      }

      graphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
      float y = card.y + MODULE_HEADER;
      for (Value value : visibleValues(module)) {
         float valueHeight = valueHeight(value);
         Rect row = new Rect(card.x + 12.0F, y, card.width - 24.0F, valueHeight - 2.0F);
         renderValue(graphics, value, row, mouseX, mouseY, alpha * expand);
         if (value.getValueType() == ValueType.MODE) {
            ModeValue modeValue = value.getModeValue();
            if (this.expandedMode == modeValue || modeAnimation(modeValue).value > 0.01F) {
               captureModeOverlay(new ModeOverlay(modeValue, row, mouseX, mouseY, alpha * expand, xOffset));
            }
         }
         y += valueHeight;
      }
      graphics.disableScissor();
   }

   private void renderValue(DrawContext graphics, Value value, Rect row, float mouseX, float mouseY, float alpha) {
      MatrixStack stack = graphics.getMatrices();
      if (value.getValueType() == ValueType.BOOLEAN) {
         BooleanValue boolValue = value.getBooleanValue();
         drawTextInRect(stack, boolValue.getName(), new Rect(row.x, row.y, row.width - 24.0F, SETTING_HEIGHT), TEXT, alpha, 0.34F);
         renderCheckbox(stack, boolBox(row), boolValue.getCurrentValue(), alpha);
      } else if (value.getValueType() == ValueType.FLOAT) {
         renderFloatValue(stack, value.getFloatValue(), row, alpha);
      } else if (value.getValueType() == ValueType.MODE) {
         renderModeValue(stack, value.getModeValue(), row, alpha);
      } else if (value.getValueType() == ValueType.ADDONS) {
         renderAddonsValue(stack, value.getAddonsValue(), row, alpha);
      } else if (value.getValueType() == ValueType.STRING) {
         renderStringValue(stack, value.getStringValue(), row, alpha);
      }
   }

   private void renderFloatValue(MatrixStack stack, FloatValue floatValue, Rect row, float alpha) {
      drawText(stack, floatValue.getName(), row.x, row.y + 3.0F, TEXT, alpha, 0.34F);
      String value = formatFloat(floatValue.getCurrentValue());
      Rect valueRect = numberField(row);
      drawTextInRect(stack, value, valueRect, MUTED, alpha, 0.30F);
      Rect slider = numberSlider(row);
      float progress = animatedNumberProgress(floatValue);
      roundedRect(stack, slider.x, slider.y, slider.width, slider.height, CONTROL_RADIUS, alpha(TRACK, alpha));
      roundedRect(stack, slider.x, slider.y, slider.width * progress, slider.height, CONTROL_RADIUS, alpha(accent(), alpha));
      float knobX = slider.x + slider.width * progress;
      roundedRect(stack, knobX - 2.0F, slider.y - 1.0F, 5.0F, 5.0F, CONTROL_RADIUS, alpha(0xFFE6E6E8, alpha));
   }

   private void renderModeValue(MatrixStack stack, ModeValue modeValue, Rect row, float alpha) {
      drawTextInRect(stack, modeValue.getName(), new Rect(row.x, row.y, row.width - 92.0F, SETTING_HEIGHT), TEXT, alpha, 0.34F);
      Rect dropdown = modeDropdown(row);
      roundedRect(stack, dropdown.x, dropdown.y, dropdown.width, dropdown.height, 4.0F, alpha(0xFF2A2A2D, alpha));
      roundedOutline(stack, dropdown.x, dropdown.y, dropdown.width, dropdown.height, 4.0F, alpha(0x55FFFFFF, alpha));
      drawTextInRect(stack, clip(modeValue.getCurrentMode(), 48.0F, 0.30F), new Rect(dropdown.x + 6.0F, dropdown.y, dropdown.width - 18.0F, dropdown.height), TEXT, alpha, 0.30F);
      drawTextInRect(stack, this.expandedMode == modeValue ? "^" : "v", new Rect(dropdown.right() - 12.0F, dropdown.y, 8.0F, dropdown.height), MUTED, alpha, 0.30F);
   }

   private void renderAddonsValue(MatrixStack stack, AddonsValue addonsValue, Rect row, float alpha) {
      drawText(stack, addonsValue.getName(), row.x, row.y + 3.0F, TEXT, alpha, 0.34F);
      float y = row.y + 28.0F;
      String[] values = addonsValue.getValues();
      for (int i = 0; i < values.length; i++) {
         String option = values[i];
         if (option == null) {
            continue;
         }
         drawTextInRect(stack, option, new Rect(row.x + 8.0F, y, row.width - 26.0F, 20.0F), addonsValue.isSelected(i) ? TEXT : MUTED, alpha, 0.32F);
         renderCheckbox(stack, new Rect(row.right() - 18.0F, y + 4.0F, 12.0F, 12.0F), addonsValue.isSelected(i), alpha);
         y += 24.0F;
      }
   }

   private void renderStringValue(MatrixStack stack, StringValue stringValue, Rect row, float alpha) {
      drawTextInRect(stack, stringValue.getName(), new Rect(row.x, row.y, row.width - 120.0F, SETTING_HEIGHT), TEXT, alpha, 0.34F);
      String raw = this.editingString == stringValue
              ? this.editingStringText + ((System.currentTimeMillis() / 450L) % 2L == 0L ? "_" : "")
              : stringValue.getCurrentValue();
      if (raw == null) {
         raw = "";
      }
      Rect field = stringField(row);
      roundedRect(stack, field.x, field.y, field.width, field.height, CONTROL_RADIUS, alpha(0xFF2A2A2D, alpha));
      drawTextInRect(stack, clip(raw, field.width - 8.0F, 0.30F), new Rect(field.x + 5.0F, field.y, field.width - 10.0F, field.height), this.editingString == stringValue ? TEXT : MUTED, alpha, 0.30F);
   }

   private void renderModeOverlay(DrawContext graphics) {
      if (this.modeOverlay == null) {
         return;
      }
      graphics.getMatrices().push();
      graphics.getMatrices().translate(this.modeOverlay.xOffset, 0.0F, 0.0F);
      renderModeOptions(graphics.getMatrices(), this.modeOverlay.value, this.modeOverlay.row, this.modeOverlay.mouseX, this.modeOverlay.mouseY, this.modeOverlay.alpha);
      graphics.getMatrices().pop();
   }

   private void renderModeOptions(MatrixStack stack, ModeValue modeValue, Rect row, float mouseX, float mouseY, float alpha) {
      Rect bounds = modeOptionsBounds(modeValue, row);
      float open = easeOut(modeAnimation(modeValue).value);
      float visibleHeight = bounds.height * open;
      if (visibleHeight <= 0.5F) {
         return;
      }

      Rect visibleBounds = new Rect(bounds.x, bounds.y, bounds.width, visibleHeight);
      roundedRect(stack, visibleBounds.x, visibleBounds.y, visibleBounds.width, visibleBounds.height, CONTROL_RADIUS, alpha(0xFF242427, alpha * open));
      roundedOutline(stack, visibleBounds.x, visibleBounds.y, visibleBounds.width, visibleBounds.height, CONTROL_RADIUS, alpha(0x55FFFFFF, alpha * open));
      float y = bounds.y + 1.0F;
      for (int i = 0; i < modeValue.getValues().length; i++) {
         String mode = modeValue.getValues()[i];
         if (mode == null) {
            continue;
         }
         Rect option = new Rect(bounds.x + 2.0F, y, bounds.width - 4.0F, 16.0F);
         boolean selected = modeValue.isCurrentMode(mode);
         boolean hovered = option.contains(mouseX, mouseY);
         if (selected || hovered) {
            roundedRect(stack, option.x, option.y, option.width, option.height, CONTROL_RADIUS, alpha(selected ? accent() : 0xFF2D2D30, alpha * open));
         }
         drawTextInRect(stack, clip(mode, option.width - 16.0F, 0.30F), new Rect(option.x + 8.0F, option.y, option.width - 16.0F, option.height), selected ? 0xFFFFFFFF : MUTED, alpha * open, 0.30F);
         y += 18.0F;
      }
   }

   private void renderBindingOverlay(DrawContext graphics, float alpha) {
      if (this.bindingModule == null) {
         return;
      }

      MatrixStack stack = graphics.getMatrices();
      rect(stack, this.panel.x, this.panel.y, this.panel.width, this.panel.height, alpha(0x99000000, alpha));
      String line1 = "Press a key to bind " + this.bindingModule.getName();
      String line2 = "ESC clears, mouse side buttons are supported";
      drawCenteredText(stack, line1, new Rect(this.panel.x, this.panel.y + this.panel.height * 0.5F - 18.0F, this.panel.width, 18.0F), 0xFFFFFFFF, alpha, 0.45F);
      drawCenteredText(stack, line2, new Rect(this.panel.x, this.panel.y + this.panel.height * 0.5F + 8.0F, this.panel.width, 16.0F), MUTED, alpha, 0.32F);
   }

   private boolean clickCategory(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      float x = tabsStartX();
      for (Category category : Category.values()) {
         float width = tabWidth(category);
         Rect tab = new Rect(x, this.panel.y + 8.0F, width, 27.0F);
         if (tab.contains(mouseX, mouseY)) {
            if (category != this.selectedCategory) {
               this.outgoingCategory = this.selectedCategory;
               this.outgoingScroll = this.scrollAnimation.value;
               this.pageDirection = category.ordinal() > this.selectedCategory.ordinal() ? 1 : -1;
               this.selectedCategory = category;
               this.scroll = 0.0F;
               this.scrollAnimation.value = 0.0F;
               this.scrollAnimation.target = 0.0F;
               this.pageAnimation.value = 0.0F;
               this.pageAnimation.target = 1.0F;
               this.expandedMode = null;
               rememberState();
            }
            return true;
         }
         x += width + 2.0F;
      }
      return false;
   }

   private boolean handleModuleClick(double mouseX, double mouseY, int button) {
      if (!this.viewport.contains(mouseX, mouseY)) {
         return false;
      }

      float page = easeOut(this.pageAnimation.value);
      double contentMouseX = mouseX - (1.0F - page) * pageSlideDistance() * this.pageDirection;
      float visibleScroll = this.scrollAnimation.value;
      float columnWidth = (this.viewport.width - COLUMN_GAP - 6.0F) * 0.5F;
      float leftY = this.viewport.y - visibleScroll;
      float rightY = leftY;

      for (Module module : modules(this.selectedCategory)) {
         float cardHeight = moduleHeight(module);
         boolean left = leftY <= rightY;
         float x = left ? this.viewport.x : this.viewport.x + columnWidth + COLUMN_GAP;
         float y = left ? leftY : rightY;
         Rect card = new Rect(x, y, columnWidth, cardHeight);
         if (card.contains(contentMouseX, mouseY)) {
            Rect header = new Rect(card.x, card.y, card.width, MODULE_HEADER);
            if (header.contains(contentMouseX, mouseY)) {
               if (button == 0 && moduleSwitch(header).contains(contentMouseX, mouseY)) {
                  module.toggle();
                  persist();
                  rememberState();
                  return true;
               } else if (button == 2) {
                  this.bindingModule = module;
                  return true;
               }
               rememberState();
               return true;
            }

            if (handleValueClick(module, card, contentMouseX, mouseY, button)) {
               rememberState();
               return true;
            }
            return true;
         }

         if (left) {
            leftY += cardHeight + CARD_GAP;
         } else {
            rightY += cardHeight + CARD_GAP;
         }
      }
      return false;
   }

   private boolean handleValueClick(Module module, Rect card, double mouseX, double mouseY, int button) {
      if (handleExpandedModeClick(module, card, mouseX, mouseY, button)) {
         return true;
      }

      float y = card.y + MODULE_HEADER;
      for (Value value : visibleValues(module)) {
         float h = valueHeight(value);
         Rect row = new Rect(card.x + 12.0F, y, card.width - 24.0F, h - 2.0F);
         if (row.contains(mouseX, mouseY)) {
            clickValue(value, mouseX, mouseY, button, row);
            return true;
         }
         y += h;
      }
      return false;
   }

   private boolean handleExpandedModeClick(double mouseX, double mouseY, int button) {
      if (this.expandedMode == null || !this.viewport.contains(mouseX, mouseY)) {
         return false;
      }

      float page = easeOut(this.pageAnimation.value);
      double contentMouseX = mouseX - (1.0F - page) * pageSlideDistance() * this.pageDirection;
      float visibleScroll = this.scrollAnimation.value;
      float columnWidth = (this.viewport.width - COLUMN_GAP - 6.0F) * 0.5F;
      float leftY = this.viewport.y - visibleScroll;
      float rightY = leftY;

      for (Module module : modules(this.selectedCategory)) {
         float cardHeight = moduleHeight(module);
         boolean left = leftY <= rightY;
         float x = left ? this.viewport.x : this.viewport.x + columnWidth + COLUMN_GAP;
         float y = left ? leftY : rightY;
         Rect card = new Rect(x, y, columnWidth, cardHeight);
         if (handleExpandedModeClick(module, card, contentMouseX, mouseY, button)) {
            return true;
         }
         if (left) {
            leftY += cardHeight + CARD_GAP;
         } else {
            rightY += cardHeight + CARD_GAP;
         }
      }
      return false;
   }

   private boolean handleExpandedModeClick(Module module, Rect card, double mouseX, double mouseY, int button) {
      if (this.expandedMode == null) {
         return false;
      }

      float y = card.y + MODULE_HEADER;
      for (Value value : visibleValues(module)) {
         float h = valueHeight(value);
         Rect row = new Rect(card.x + 12.0F, y, card.width - 24.0F, h - 2.0F);
         if (value == this.expandedMode) {
            if (!modeOptionsBounds(this.expandedMode, row).contains(mouseX, mouseY)) {
               return false;
            }
            if (button == 0) {
               int selected = modeAt(this.expandedMode, row, mouseX, mouseY);
               if (selected >= 0) {
                  this.expandedMode.setCurrentValue(selected);
                  this.expandedMode = null;
                  persist();
               }
            } else if (button == 1) {
               this.expandedMode = null;
            }
            return true;
         }
         y += h;
      }
      return false;
   }

   private void clickValue(Value value, double mouseX, double mouseY, int button, Rect row) {
      if (value.getValueType() == ValueType.BOOLEAN) {
         if (button == 0 && boolBox(row).contains(mouseX, mouseY)) {
            BooleanValue boolValue = value.getBooleanValue();
            boolValue.setCurrentValue(!boolValue.getCurrentValue());
            persist();
         }
         return;
      }

      if (value.getValueType() == ValueType.FLOAT) {
         if (button != 0) {
            return;
         }
         FloatValue floatValue = value.getFloatValue();
         this.draggingFloatValue = floatValue;
         this.draggingFloatRow = row;
         updateFloat(floatValue, (float)mouseX, row);
         persist();
         return;
      }

      if (value.getValueType() == ValueType.MODE) {
         ModeValue modeValue = value.getModeValue();
         if (button == 0 && this.expandedMode == modeValue) {
            int selected = modeAt(modeValue, row, mouseX, mouseY);
            if (selected >= 0) {
               modeValue.setCurrentValue(selected);
               this.expandedMode = null;
               persist();
               return;
            }
         }
         if ((button == 0 && modeDropdown(row).contains(mouseX, mouseY)) || button == 1) {
            this.expandedMode = this.expandedMode == modeValue ? null : modeValue;
         }
         return;
      }

      if (value.getValueType() == ValueType.ADDONS) {
         if (button != 0) {
            return;
         }
         AddonsValue addonsValue = value.getAddonsValue();
         float optionY = row.y + 28.0F;
         for (int i = 0; i < addonsValue.getValues().length; i++) {
            Rect optionSwitch = new Rect(row.right() - 18.0F, optionY + 4.0F, 12.0F, 12.0F);
            if (optionSwitch.contains(mouseX, mouseY)) {
               addonsValue.toggleSelected(i);
               persist();
               return;
            }
            optionY += 24.0F;
         }
         return;
      }

      if (value.getValueType() == ValueType.STRING) {
         if (button == 0 && stringField(row).contains(mouseX, mouseY)) {
            this.editingString = value.getStringValue();
            this.editingStringText = this.editingString.getCurrentValue() == null ? "" : this.editingString.getCurrentValue();
         }
      }
   }

   private void computeLayout() {
      float panelWidth = Math.min(512.0F, this.width - 36.0F);
      float panelHeight = Math.min(340.0F, this.height - 48.0F);
      panelWidth = Math.max(404.0F, panelWidth);
      panelHeight = Math.max(272.0F, panelHeight);
      float x = (this.width - panelWidth) * 0.5F;
      float y = (this.height - panelHeight) * 0.5F;
      this.panel = new Rect(x, y, panelWidth, panelHeight);
      this.viewport = new Rect(this.panel.x + 14.0F, this.panel.y + 44.0F, this.panel.width - 28.0F, this.panel.height - 58.0F);
   }

   private void renderBackdrop(DrawContext graphics) {
      rect(graphics.getMatrices(), 0.0F, 0.0F, this.width, this.height, 0x66000000);
   }

   private void renderScrollbar(MatrixStack stack, float visibleScroll, float maxScroll, float contentHeight, float alpha) {
      if (maxScroll <= 0.5F || contentHeight <= 0.0F) {
         return;
      }

      float barTrack = this.viewport.height;
      float barHeight = Math.max(22.0F, barTrack * (this.viewport.height / contentHeight));
      float progress = maxScroll <= 0.0F ? 0.0F : visibleScroll / maxScroll;
      float y = this.viewport.y + progress * (barTrack - barHeight);
      roundedRect(stack, this.viewport.right() + 4.0F, this.viewport.y, 3.0F, this.viewport.height, 1.5F, alpha(0x332F2F35, alpha));
      roundedRect(stack, this.viewport.right() + 4.0F, y, 3.0F, barHeight, 1.5F, alpha(0xAAFFFFFF, alpha));
   }

   private void updateContentAnimations() {
      updateCategoryAnimations(this.selectedCategory);
      if (this.outgoingCategory != null && this.outgoingCategory != this.selectedCategory) {
         updateCategoryAnimations(this.outgoingCategory);
      }
   }

   private void updateCategoryAnimations(Category category) {
      for (Module module : modules(category)) {
         SmoothAnimationTimer moduleTimer = moduleAnimation(module);
         moduleTimer.target = visibleValues(module).isEmpty() ? 0.0F : 1.0F;
         moduleTimer.update(true);
         SmoothAnimationTimer switchTimer = switchAnimation(module);
         switchTimer.target = module.isEnabled() ? 1.0F : 0.0F;
         switchTimer.update(true);

         for (Value value : visibleValues(module)) {
            if (value.getValueType() == ValueType.MODE) {
               ModeValue modeValue = value.getModeValue();
               SmoothAnimationTimer modeTimer = modeAnimation(modeValue);
               modeTimer.target = this.expandedMode == modeValue ? 1.0F : 0.0F;
               modeTimer.update(true);
            }
         }
      }
   }

   private float contentHeight(Category category) {
      float left = 0.0F;
      float right = 0.0F;
      for (Module module : modules(category)) {
         float h = moduleHeight(module) + CARD_GAP;
         if (left <= right) {
            left += h;
         } else {
            right += h;
         }
      }
      return Math.max(left, right);
   }

   private float moduleHeight(Module module) {
      SmoothAnimationTimer timer = moduleAnimation(module);
      float expand = easeOut(timer.value);
      float expandedHeight = 4.0F;
      for (Value value : visibleValues(module)) {
         expandedHeight += valueHeight(value);
      }
      return MODULE_HEADER + expandedHeight * expand;
   }

   private float valueHeight(Value value) {
      if (value.getValueType() == ValueType.FLOAT) {
         return 42.0F;
      }
      if (value.getValueType() == ValueType.ADDONS) {
         return 30.0F + Math.max(0, value.getAddonsValue().getValues().length) * 24.0F;
      }
      return SETTING_HEIGHT;
   }

   private List<Value> visibleValues(Module module) {
      List<Value> result = new ArrayList<>();
      for (Value value : Naven.getInstance().getValueManager().getValuesByHasValue(module)) {
         if (value.isVisible()) {
            result.add(value);
         }
      }
      return result;
   }

   private List<Module> modules(Category category) {
      List<Module> result = new ArrayList<>(Naven.getInstance().getModuleManager().getModulesByCategory(category));
      result.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
      return result;
   }

   private void updateFloat(FloatValue value, float mouseX, Rect row) {
      Rect slider = numberSlider(row);
      float progress = clamp01((mouseX - slider.x) / slider.width);
      float raw = value.getMinValue() + progress * (value.getMaxValue() - value.getMinValue());
      float step = value.getStep();
      if (step > 0.0F) {
         raw = Math.round(raw / step) * step;
      }
      value.setCurrentValue(clamp(raw, value.getMinValue(), value.getMaxValue()));
   }

   private Rect numberSlider(Rect row) {
      return new Rect(row.x, row.y + 24.0F, row.width, 3.0F);
   }

   private Rect numberField(Rect row) {
      return new Rect(row.right() - 58.0F, row.y + 2.0F, 58.0F, 14.0F);
   }

   private Rect modeDropdown(Rect row) {
      return new Rect(row.right() - 66.0F, row.y + 7.0F, 64.0F, 16.0F);
   }

   private Rect stringField(Rect row) {
      return new Rect(row.right() - 112.0F, row.y + 6.0F, 110.0F, 18.0F);
   }

   private int modeAt(ModeValue value, Rect row, double mouseX, double mouseY) {
      Rect anchor = modeDropdown(row);
      float visibleHeight = modeOptionTotalHeight(value);
      if (mouseY < anchor.bottom() + 2.0F || mouseY > anchor.bottom() + 2.0F + visibleHeight) {
         return -1;
      }

      float y = anchor.bottom() + 2.0F;
      for (int i = 0; i < value.getValues().length; i++) {
         if (new Rect(anchor.x, y, anchor.width, 16.0F).contains(mouseX, mouseY)) {
            return i;
         }
         y += 18.0F;
      }
      return -1;
   }

   private Rect modeOptionsBounds(ModeValue value, Rect row) {
      Rect anchor = modeDropdown(row);
      return new Rect(anchor.x, anchor.bottom() + 2.0F, anchor.width, modeOptionTotalHeight(value));
   }

   private float modeOptionTotalHeight(ModeValue value) {
      return value.getValues().length * 18.0F;
   }

   private Rect moduleSwitch(Rect header) {
      return new Rect(header.right() - 42.0F, header.y + 8.0F, 26.0F, 16.0F);
   }

   private Rect boolBox(Rect row) {
      return new Rect(row.right() - 18.0F, row.y + 9.0F, 12.0F, 12.0F);
   }

   private float tabsStartX() {
      return this.panel.x + (this.panel.width - tabsWidth()) * 0.5F;
   }

   private float tabsWidth() {
      float width = 0.0F;
      for (Category category : Category.values()) {
         width += tabWidth(category) + 2.0F;
      }
      return Math.max(0.0F, width - 2.0F);
   }

   private float tabWidth(Category category) {
      return Math.max(58.0F, textWidth(category.getDisplayName(), 0.34F) + 22.0F);
   }

   private int accent() {
      if (this.selectedCategory == Category.COMBAT) {
         return 0xFF9B4772;
      }
      if (this.selectedCategory == Category.MOVEMENT) {
         return 0xFF2F8CC8;
      }
      if (this.selectedCategory == Category.PLAYER) {
         return 0xFF4B5D96;
      }
      if (this.selectedCategory == Category.WORLD) {
         return 0xFF8A6E3D;
      }
      if (this.selectedCategory == Category.MISC) {
         return 0xFF54A5A0;
      }
      return 0xFF7A56A8;
   }

   private float pageSlideDistance() {
      return this.viewport.width + COLUMN_GAP;
   }

   private SmoothAnimationTimer moduleAnimation(Module module) {
      return this.moduleAnimations.computeIfAbsent(module, ignored -> new SmoothAnimationTimer(visibleValues(module).isEmpty() ? 0.0F : 1.0F, visibleValues(module).isEmpty() ? 0.0F : 1.0F, 0.22F));
   }

   private SmoothAnimationTimer modeAnimation(ModeValue value) {
      return this.modeAnimations.computeIfAbsent(value, ignored -> new SmoothAnimationTimer(0.0F, 0.0F, 0.24F));
   }

   private SmoothAnimationTimer categoryAnimation(Category category) {
      return this.categoryAnimations.computeIfAbsent(category, ignored -> new SmoothAnimationTimer(category == this.selectedCategory ? 1.0F : 0.0F, category == this.selectedCategory ? 1.0F : 0.0F, 0.18F));
   }

   private SmoothAnimationTimer switchAnimation(Module module) {
      return this.switchAnimations.computeIfAbsent(module, ignored -> new SmoothAnimationTimer(module.isEnabled() ? 1.0F : 0.0F, module.isEnabled() ? 1.0F : 0.0F, 0.20F));
   }

   private float animatedNumberProgress(FloatValue value) {
      float target = numberProgress(value);
      SmoothAnimationTimer timer = this.numberAnimations.computeIfAbsent(value, ignored -> new SmoothAnimationTimer(target, target, 0.38F));
      timer.target = target;
      timer.update(true);
      return clamp01(timer.value);
   }

   private float numberProgress(FloatValue value) {
      return clamp01((value.getCurrentValue() - value.getMinValue()) / Math.max(0.0001F, value.getMaxValue() - value.getMinValue()));
   }

   private void captureModeOverlay(ModeOverlay overlay) {
      if (this.modeOverlay == null || overlay.value == this.expandedMode) {
         this.modeOverlay = overlay;
      }
   }

   private void commitEditingString(boolean apply) {
      if (this.editingString != null && apply) {
         this.editingString.setCurrentValue(this.editingStringText);
         persist();
      }
      this.editingString = null;
      this.editingStringText = "";
   }

   private void persist() {
      Naven.getInstance().getFileManager().save();
   }

   private void rememberState() {
      rememberedCategory = this.selectedCategory;
      rememberedScroll = this.scroll;
   }

   private String compactKeyName(int keyCode) {
      if (keyCode == 0 || keyCode == InputUtil.UNKNOWN_KEY.getCode()) {
         return "NONE";
      }
      if (keyCode < 0) {
         return "M" + Math.abs(keyCode);
      }
      String text = InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
      if (text == null || text.isBlank()) {
         return "KEY" + keyCode;
      }
      text = text.toUpperCase();
      if (text.startsWith("KEY.KEYBOARD.")) {
         text = text.substring("KEY.KEYBOARD.".length());
      }
      return text.length() > 6 ? text.substring(0, 6) : text;
   }

   private String formatFloat(float value) {
      float rounded = Math.round(value * 100.0F) / 100.0F;
      if (Math.abs(rounded - Math.round(rounded)) < 0.001F) {
         return String.valueOf(Math.round(rounded));
      }
      return String.valueOf(rounded);
   }

   private String clip(String text, float maxWidth, float scale) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      if (textWidth(text, scale) <= maxWidth) {
         return text;
      }
      String suffix = "...";
      String result = text;
      while (!result.isEmpty() && textWidth(result + suffix, scale) > maxWidth) {
         result = result.substring(0, result.length() - 1);
      }
      return result + suffix;
   }

   private void renderSwitch(MatrixStack stack, Module module, Rect rect, float alpha) {
      float enabled = easeOut(switchAnimation(module).value);
      int track = mix(0xFF303033, accent(), enabled);
      roundedRect(stack, rect.x, rect.y, rect.width, rect.height, rect.height * 0.5F, alpha(track, alpha));
      float knobSize = 12.0F;
      float knobX = rect.x + 2.0F + (rect.width - knobSize - 4.0F) * enabled;
      roundedRect(stack, knobX, rect.y + 2.0F, knobSize, knobSize, knobSize * 0.5F, alpha(0xFFE6E6E8, alpha));
   }

   private void renderCheckbox(MatrixStack stack, Rect rect, boolean enabled, float alpha) {
      roundedRect(stack, rect.x, rect.y, rect.width, rect.height, CONTROL_RADIUS, alpha(enabled ? accent() : 0xFF303033, alpha));
      roundedOutline(stack, rect.x, rect.y, rect.width, rect.height, CONTROL_RADIUS, alpha(enabled ? 0x66FFFFFF : 0x44FFFFFF, alpha));
      if (enabled) {
         String mark = "\u2713";
         float scale = 0.44F;
         float width = Fonts.misans.getWidth(mark, scale);
         float height = (float)Fonts.misans.getHeight(false, scale);
         Fonts.misans.render(
                 stack,
                 mark,
                 rect.x + (rect.width - width) * 0.5F,
                 rect.y + (rect.height - height) * 0.5F - 0.5F,
                 new Color(alpha(0xFFFFFFFF, alpha), true),
                 false,
                 scale
         );
      }
   }

   private void drawText(MatrixStack stack, String text, float x, float y, int color, float alpha, float scale) {
      if (text == null || text.isEmpty()) {
         return;
      }
      Fonts.opensans.render(stack, text, x, y, new Color(alpha(color, alpha), true), false, scale);
   }

   private void drawTextInRect(MatrixStack stack, String text, Rect rect, int color, float alpha, float scale) {
      float y = rect.y + (rect.height - textHeight(scale)) * 0.5F;
      drawText(stack, text, rect.x, y, color, alpha, scale);
   }

   private void drawCenteredText(MatrixStack stack, String text, Rect rect, int color, float alpha, float scale) {
      float x = rect.x + (rect.width - textWidth(text, scale)) * 0.5F;
      float y = rect.y + (rect.height - textHeight(scale)) * 0.5F - 1.0F;
      drawText(stack, text, x, y, color, alpha, scale);
   }

   private float textWidth(String text, float scale) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      return Fonts.opensans.getWidth(text, scale);
   }

   private float textHeight(float scale) {
      return (float)Fonts.opensans.getHeight(false, scale);
   }

   private void rect(MatrixStack stack, float x, float y, float width, float height, int color) {
      if ((color >>> 24) == 0 || width <= 0.0F || height <= 0.0F) {
         return;
      }
      RenderUtils.fill(stack, x, y, x + width, y + height, color);
   }

   private void roundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, int color) {
      if ((color >>> 24) == 0 || width <= 0.0F || height <= 0.0F) {
         return;
      }
      RenderUtils.drawRoundedRect(stack, x, y, width, height, radius, color);
   }

   private void roundedOutline(MatrixStack stack, float x, float y, float width, float height, float radius, int color) {
      if ((color >>> 24) == 0 || width <= 1.0F || height <= 1.0F) {
         return;
      }
      float r = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
      rect(stack, x + r, y, width - r * 2.0F, 1.0F, color);
      rect(stack, x + r, y + height - 1.0F, width - r * 2.0F, 1.0F, color);
      rect(stack, x, y + r, 1.0F, height - r * 2.0F, color);
      rect(stack, x + width - 1.0F, y + r, 1.0F, height - r * 2.0F, color);
   }

   private int alpha(int color, float alpha) {
      int baseAlpha = color >>> 24;
      return (color & 0x00FFFFFF) | (clamp255(Math.round(baseAlpha * clamp01(alpha))) << 24);
   }

   private int mix(int from, int to, float progress) {
      float t = clamp01(progress);
      int a = lerp(from >>> 24, to >>> 24, t);
      int r = lerp((from >>> 16) & 255, (to >>> 16) & 255, t);
      int g = lerp((from >>> 8) & 255, (to >>> 8) & 255, t);
      int b = lerp(from & 255, to & 255, t);
      return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
   }

   private int lerp(int from, int to, float progress) {
      return Math.round(from + (to - from) * progress);
   }

   private int clamp255(int value) {
      return Math.max(0, Math.min(255, value));
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private float easeOut(float value) {
      float t = clamp01(value);
      return 1.0F - (float)Math.pow(1.0F - t, 3.0);
   }

   private static class Rect {
      private final float x;
      private final float y;
      private final float width;
      private final float height;

      private Rect(float x, float y, float width, float height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      private float right() {
         return this.x + this.width;
      }

      private float bottom() {
         return this.y + this.height;
      }

      private boolean contains(double mouseX, double mouseY) {
         return mouseX >= this.x && mouseX <= this.right() && mouseY >= this.y && mouseY <= this.bottom();
      }
   }

   private static class ModeOverlay {
      private final ModeValue value;
      private final Rect row;
      private final float mouseX;
      private final float mouseY;
      private final float alpha;
      private final float xOffset;

      private ModeOverlay(ModeValue value, Rect row, float mouseX, float mouseY, float alpha, float xOffset) {
         this.value = value;
         this.row = row;
         this.mouseX = mouseX;
         this.mouseY = mouseY;
         this.alpha = alpha;
         this.xOffset = xOffset;
      }
   }
}
