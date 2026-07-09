package awa.qwq.ovo.Naven.modules.impl.visual;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRespawn;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.BlockUtils;
import awa.qwq.ovo.Naven.utils.ChunkUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@ModuleInfo(
   name = "ChestESP",
   description = "Highlights chests",
   category = Category.VISUAL
)
public class ChestESP extends Module {
   private static final float[] chestColor = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] openedChestColor = new float[]{1.0F, 0.0F, 0.0F};
   public final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
   private final List<Box> renderBoundingBoxes = new CopyOnWriteArrayList<>();

   @Override
   public void onDisable() {
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.openedChests.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof BlockEventS2CPacket) {
         BlockEventS2CPacket packet = (BlockEventS2CPacket)e.getPacket();
         if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getType() == 1 && packet.getData() == 1) {
            this.openedChests.add(packet.getPos());
         }
      }
   }

   @EventTarget
   public void onTick(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
         this.renderBoundingBoxes.clear();

         for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity instanceof ChestBlockEntity) {
               ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
               Box box = this.getChestBox(chestBE);
               if (box != null) {
                  this.renderBoundingBoxes.add(box);
               }
            }
         }
      }
   }

   private Box getChestBox(ChestBlockEntity chestBE) {
      BlockState state = chestBE.getCachedState();
      if (!state.contains(ChestBlock.CHEST_TYPE)) {
         return null;
      } else {
         ChestType chestType = (ChestType)state.get(ChestBlock.CHEST_TYPE);
         if (chestType == ChestType.LEFT) {
            return null;
         } else {
            BlockPos pos = chestBE.getPos();
            Box box = BlockUtils.getBoundingBox(pos);
            if (chestType != ChestType.SINGLE) {
               BlockPos pos2 = pos.offset(ChestBlock.getFacing(state));
               if (BlockUtils.canBeClicked(pos2)) {
                  Box box2 = BlockUtils.getBoundingBox(pos2);
                  box = box.union(box2);
               }
            }

            return box;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      MatrixStack stack = e.getPMatrixStack();
      stack.push();
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      Tessellator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuffer();

      for (Box box : this.renderBoundingBoxes) {
         BlockPos pos = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
         float[] color = this.openedChests.contains(pos) ? openedChestColor : chestColor;
         RenderSystem.setShaderColor(color[0], color[1], color[2], 0.25F);
         RenderUtils.drawBoxWithCameraOffset(bufferBuilder, stack.peek().getPositionMatrix(), box);
      }

      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      stack.pop();
   }
}
