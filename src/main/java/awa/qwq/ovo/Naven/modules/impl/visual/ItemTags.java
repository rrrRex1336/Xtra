package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRender2D;
import awa.qwq.ovo.Naven.events.impl.EventShader;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.player.ContainerStealer;
import awa.qwq.ovo.Naven.utils.Colors;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathUtils;
import awa.qwq.ovo.Naven.utils.ProjectionUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.utils.renderer.text.CustomTextRenderer;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnchantedGoldenAppleItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SnowballItem;
import org.joml.Vector4f;

@ModuleInfo(
   name = "ItemTags",
   description = "Show item tags.",
   category = Category.VISUAL
)
public class ItemTags extends Module {
   private final int color = Colors.getColor(0, 0, 0, 40);
   private final ConcurrentHashMap<ItemEntity, Vector2f> entityPositions = new ConcurrentHashMap<>();
   private final List<Vector4f> blurMatrices = new ArrayList<>();
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(0.25F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(0.5F)
      .build()
      .getFloatValue();
   BooleanValue allItems = ValueBuilder.create(this, "All Items").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue godItems = ValueBuilder.create(this, "God Items")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue diamond = ValueBuilder.create(this, "Diamond")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue gold = ValueBuilder.create(this, "Gold")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue iron = ValueBuilder.create(this, "Iron")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue enderPearl = ValueBuilder.create(this, "Ender Pearl")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue goldenApple = ValueBuilder.create(this, "Golden Apple")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue usefulItem = ValueBuilder.create(this, "Useful Item")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();

   private static String getDisplayName(ItemEntity ent) {
      ItemStack item = ent.getStack();
      return item.toHoverableText().getString() + " * " + item.getCount();
   }

   private boolean isValidItem(ItemStack stack) {
      if (stack == null) {
         return false;
      } else if (stack.isEmpty()) {
         return false;
      } else if (this.allItems.getCurrentValue()) {
         return true;
      } else {
         if (this.godItems.getCurrentValue()) {
            if (InventoryUtils.isKBBall(stack)) {
               return true;
            }

            if (stack.getItem() instanceof EnchantedGoldenAppleItem) {
               return true;
            }

            if (InventoryUtils.isGodAxe(stack)) {
               return true;
            }
         }

         if (this.diamond.getCurrentValue() && stack.getItem() == Items.DIAMOND) {
            return true;
         } else if (this.gold.getCurrentValue() && stack.getItem() == Items.GOLD_INGOT) {
            return true;
         } else if (this.iron.getCurrentValue() && stack.getItem() == Items.IRON_INGOT) {
            return true;
         } else if (this.enderPearl.getCurrentValue() && stack.getItem() == Items.ENDER_PEARL) {
            return true;
         } else if (this.goldenApple.getCurrentValue() && stack.getItem() == Items.GOLDEN_APPLE) {
            return true;
         } else {
            if (this.usefulItem.getCurrentValue()) {
               if (stack.getItem() instanceof BlockItem && stack.getCount() < 8) {
                  return false;
               }

               if ((stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem) && stack.getCount() < 3) {
                  return false;
               }

               if (ContainerStealer.isItemUseful(stack)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean isGodItem(ItemStack stack) {
      if (InventoryUtils.isKBBall(stack)) {
         return true;
      } else if (stack.getItem() instanceof EnchantedGoldenAppleItem) {
         return true;
      } else {
         return stack.getItem() instanceof EndCrystalItem ? true : InventoryUtils.isGodAxe(stack);
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (this.isValidItem(itemEntity.getStack())) {
               double x = MathUtils.interpolate(renderPartialTicks, entity.prevX, entity.getX());
               double y = MathUtils.interpolate(renderPartialTicks, entity.prevY, entity.getY()) + (double)entity.getHeight() + 0.5;
               double z = MathUtils.interpolate(renderPartialTicks, entity.prevZ, entity.getZ());
               Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
               vector.setY(vector.getY() - 2.0F);
               this.entityPositions.put(itemEntity, vector);
            }
         }
      }
   }

   @EventTarget
   public void update(EventRender event) {
      try {
         this.updatePositions(event.getRenderPartialTicks());
      } catch (Exception var3) {
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      for (Vector4f blurMatrix : this.blurMatrices) {
         RenderUtils.fill(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), this.color);
      }
   }

   @EventTarget
   public void on2DRender(EventRender2D e) {
      try {
         MatrixStack stack = e.getStack();
         this.blurMatrices.clear();

         for (ItemEntity ent : this.entityPositions.keySet()) {
            if (ent != null) {
               Vector2f renderPositions = this.entityPositions.get(ent);
               stack.push();
               CustomTextRenderer harmony = Fonts.harmony;
               String str = getDisplayName(ent);
               float allWidth = harmony.getWidth(str, (double)this.scale.getCurrentValue()) + 8.0F;
               this.blurMatrices
                  .add(new Vector4f(renderPositions.x - allWidth / 2.0F, renderPositions.y - 14.0F, renderPositions.x + allWidth / 2.0F, renderPositions.y));
               if (this.isGodItem(ent.getStack())) {
                  harmony.render(
                     stack,
                     str,
                     (double)(renderPositions.x - allWidth / 2.0F + 4.0F),
                     (double)(renderPositions.y - 12.0F),
                     Color.RED,
                     true,
                     (double)this.scale.getCurrentValue()
                  );
               } else {
                  harmony.render(
                     stack,
                     str,
                     (double)(renderPositions.x - allWidth / 2.0F + 4.0F),
                     (double)(renderPositions.y - 12.0F),
                     Color.WHITE,
                     true,
                     (double)this.scale.getCurrentValue()
                  );
               }

               stack.pop();
            }
         }
      } catch (Exception var9) {
      }
   }
}
