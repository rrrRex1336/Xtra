package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.DragManager;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;

@ModuleInfo(
   name = "Scoreboard",
   description = "Modifies the scoreboard",
   category = Category.VISUAL
)
public class Scoreboard extends Module {
   public BooleanValue hideScore = ValueBuilder.create(this, "Hide Red Score").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue modern = ValueBuilder.create(this, "Modern Style").setDefaultBooleanValue(false).build().getBooleanValue();
   public FloatValue xOffset = DragManager.createHiddenPositionValue(this, "Drag X", 0.0F);
   public FloatValue down = ValueBuilder.create(this, "Down")
      .setDefaultFloatValue(120.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(-10000.0F)
      .setMaxFloatValue(10000.0F)
      .setVisibility(() -> false)
      .build()
      .getFloatValue();
   private final DragManager dragManager = new DragManager(this.xOffset, this.down);
   private Vector4f shaderRect;
   private long shaderRectTime;
   private Consumer<DrawContext> modernRenderer;
   private long modernRendererTime;

   public void updateDrag(float baseX, float baseY, float width, float height) {
      this.dragManager.update(baseX, baseY, width, height);
   }

   public float getRenderX(float baseX) {
      return this.dragManager.getX(baseX);
   }

   public float getRenderY(float baseY) {
      return this.dragManager.getY(baseY);
   }

   public void setShaderRect(float x, float y, float width, float height) {
      this.shaderRect = new Vector4f(x, y, width, height);
      this.shaderRectTime = System.currentTimeMillis();
   }

   public void setModernRenderer(Consumer<DrawContext> modernRenderer) {
      this.modernRenderer = modernRenderer;
      this.modernRendererTime = System.currentTimeMillis();
   }

   public void clearModernRenderer() {
      this.modernRenderer = null;
   }

   @EventTarget(1)
   public void onRender(EventRender2D event) {
      if (!this.modern.getCurrentValue() || this.modernRenderer == null || System.currentTimeMillis() - this.modernRendererTime > 100L) {
         return;
      }

      this.modernRenderer.accept(event.getGuiGraphics());
   }

   @EventTarget
   public void onShader(EventShader event) {
      if (!this.modern.getCurrentValue() || this.shaderRect == null || System.currentTimeMillis() - this.shaderRectTime > 100L) {
         return;
      }

      if (event.getType() == EventType.BLUR) {
         RenderUtils.drawRoundedRect(event.getStack(), this.shaderRect.x(), this.shaderRect.y(), this.shaderRect.z(), this.shaderRect.w(), 3.0F, 1073741824);
      } else if (event.getType() == EventType.SHADOW) {
         RenderUtils.drawRoundedRect(event.getStack(), this.shaderRect.x(), this.shaderRect.y(), this.shaderRect.z(), this.shaderRect.w(), 3.0F, Integer.MIN_VALUE);
      }
   }
}
