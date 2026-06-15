package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author：LinYanLi1337
 * @Date：2025/12/26  10:55
 * @Filename：Piercing
 */

@ModuleInfo(
        name = "Piercing",
        description = "Allows interacting/breaking with blocks through walls.",
        category = Category.PLAYER
)
public class Piercing extends Module {

    private final AddonsValue containerSelect = ValueBuilder.create(this, "Container Select")
            .setAddonsModes("Chest", "Double Chest", "Ender Chest", "Brewing Stand", "Furnace", "Dispenser", "Hopper")
            .setDefaultSelectedAddons()
            .build()
            .getAddonsValue();

    private final AddonsValue entitySelect = ValueBuilder.create(this, "Entity Select")
            .setAddonsModes("Villager", "Armor Stand", "Named Entity")
            .setDefaultSelectedAddons()
            .build()
            .getAddonsValue();

    private final BooleanValue renderTags = ValueBuilder.create(this, "Render Tags")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue namedEntities = ValueBuilder.create(this, "Named Entities")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final Minecraft mc = Minecraft.getInstance();
    private final TickTimeHelper timer = new TickTimeHelper();
    private final List<RenderInfo> renderList = new CopyOnWriteArrayList<>();
    private boolean lastKeyUseState = false;

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (this.mc.player == null || this.mc.level == null) {
            return;
        }

        boolean currentKeyUse = this.mc.options.keyUse.isDown();

        if (currentKeyUse && !this.lastKeyUseState) {
            double range = 0;
            if (this.mc.gameMode != null) {
                range = this.mc.gameMode.getPickRange();
            }
            Vec3 eyePos = this.mc.player.getEyePosition(1.0f);
            Vec3 lookVec = this.mc.player.getLookAngle();
            Vec3 reachEnd = eyePos.add(lookVec.scale(range));

            this.findAndInteractWithTarget(eyePos, reachEnd);
        }
    }

    @EventTarget
    public void onRender(EventRender event) {
        this.renderList.clear();

        if (!this.renderTags.getCurrentValue() || this.mc.player == null || this.mc.level == null) {
            return;
        }

        double blockRenderRange = 6.0;
        double entityRenderRange = 8.0;
        float partialTicks = event.getRenderPartialTicks();
        Vec3 cameraPos = this.mc.gameRenderer.getMainCamera().getPosition();
        BlockPos playerPos = this.mc.player.blockPosition();
        for (Entity entity : this.mc.level.getEntities(this.mc.player,
                this.mc.player.getBoundingBox().inflate(entityRenderRange))) {
            if (!this.isTargetEntity(entity)) continue;

            Vec3 renderPos = this.getEntityRenderPosition(entity, partialTicks);
            if (renderPos.distanceToSqr(cameraPos) > entityRenderRange * entityRenderRange) continue;

            Vector2f screenPos = ProjectionUtils.project(renderPos.x, renderPos.y, renderPos.z, partialTicks);
            if (screenPos == null) continue;

            this.renderList.add(new RenderInfo(screenPos, "[Interact]", Color.CYAN));
        }

        int rangeInt = (int) Math.ceil(blockRenderRange);
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-rangeInt, -rangeInt, -rangeInt),
                playerPos.offset(rangeInt, rangeInt, rangeInt))) {
            BlockState state = this.mc.level.getBlockState(pos);
            if (!this.isTargetContainer(pos, state)) continue;

            Vec3 topCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            if (topCenter.distanceToSqr(cameraPos) > blockRenderRange * blockRenderRange) continue;

            Vector2f screenPos = ProjectionUtils.project(topCenter.x, topCenter.y, topCenter.z, partialTicks);
            if (screenPos == null) continue;

            if (state.getBlock() instanceof EnderChestBlock) {
                this.renderList.add(new RenderInfo(screenPos, "[Ender Chest]", Color.MAGENTA));
            } else if (state.getBlock() instanceof ChestBlock) {
                if (this.isDoubleChest(state)) {
                    this.renderList.add(new RenderInfo(screenPos, "[Double Chest]", Color.ORANGE));
                } else {
                    this.renderList.add(new RenderInfo(screenPos, "[Chest]", Color.YELLOW));
                }
            } else if (state.getBlock() instanceof BrewingStandBlock) {
                this.renderList.add(new RenderInfo(screenPos, "[Brewing Stand]", Color.PINK));
            } else if (state.getBlock() instanceof FurnaceBlock) {
                this.renderList.add(new RenderInfo(screenPos, "[Furnace]", Color.GRAY));
            } else if (state.getBlock() instanceof DispenserBlock) {
                this.renderList.add(new RenderInfo(screenPos, "[Dispenser]", Color.GREEN));
            } else if (state.getBlock() instanceof HopperBlock) {
                this.renderList.add(new RenderInfo(screenPos, "[Hopper]", Color.DARK_GRAY));
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!this.renderTags.getCurrentValue()) {
            return;
        }

        for (RenderInfo info : this.renderList) {
            Fonts.harmony.render(event.getStack(), info.tag, info.screenPos.x, info.screenPos.y, info.color, true, 0.4f);
        }
    }

    private Vec3 getEntityRenderPosition(Entity entity, float partialTicks) {
        double x = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double y = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double z = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
        return new Vec3(x, y + (entity.getBbHeight() / 2.0f), z);
    }

    private void findAndInteractWithTarget(Vec3 eyePos, Vec3 reachEnd) {
        Entity closestEntity = null;
        BlockHitResult closestBlockHit = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Entity entity : this.mc.level.getEntities(this.mc.player,
                this.mc.player.getBoundingBox().inflate(reachEnd.distanceTo(eyePos)))) {
            if (!this.isTargetEntity(entity)) continue;

            Optional<Vec3> hitOpt = entity.getBoundingBox().inflate(0.1).clip(eyePos, reachEnd);
            if (!hitOpt.isPresent()) continue;

            double distSq = eyePos.distanceToSqr(hitOpt.get());
            if (!(distSq < closestDistSq)) continue;

            closestDistSq = distSq;
            closestEntity = entity;
            closestBlockHit = null;
        }

        for (BlockEntity be : ChunkUtils.getLoadedBlockEntities().toList()) {
            BlockState state = be.getBlockState();
            BlockPos pos = be.getBlockPos();

            if (!this.isTargetContainer(pos, state)) continue;

            AABB box = this.getBlockBoundingBox(be);
            if (box == null) continue;

            Optional<Vec3> hitOpt = box.clip(eyePos, reachEnd);
            if (!hitOpt.isPresent()) continue;

            double distSq = eyePos.distanceToSqr(hitOpt.get());
            if (!(distSq < closestDistSq)) continue;

            closestDistSq = distSq;
            closestBlockHit = new BlockHitResult(hitOpt.get(), Direction.UP, be.getBlockPos(), false);
            closestEntity = null;
        }

        if (closestEntity != null) {
            this.interactWithEntity(closestEntity);
            this.timer.reset();
        } else if (closestBlockHit != null) {
            this.interactWithBlock(closestBlockHit);
            this.timer.reset();
        }
    }

    private boolean isTargetContainer(BlockPos pos, BlockState state) {
        List<String> selected = this.containerSelect.getSelectedValues();

        if (selected.contains("Chest") && state.getBlock() instanceof ChestBlock && !this.isDoubleChest(state)) {
            return true;
        }
        if (selected.contains("Double Chest") && state.getBlock() instanceof ChestBlock && this.isDoubleChest(state)) {
            return true;
        }
        if (selected.contains("Ender Chest") && state.getBlock() instanceof EnderChestBlock) {
            return true;
        }
        if (selected.contains("Brewing Stand") && state.getBlock() instanceof BrewingStandBlock) {
            return true;
        }
        if (selected.contains("Furnace") && state.getBlock() instanceof FurnaceBlock) {
            return true;
        }
        if (selected.contains("Dispenser") && state.getBlock() instanceof DispenserBlock) {
            return true;
        }
        if (selected.contains("Hopper") && state.getBlock() instanceof HopperBlock) {
            return true;
        }
        return false;
    }

    private boolean isDoubleChest(BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock)) return false;
        return state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }

    private AABB getBlockBoundingBox(BlockEntity be) {
        if (be instanceof ChestBlockEntity) {
            BlockState state = be.getBlockState();
            if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.LEFT) {
                return null;
            }

            BlockPos pos = be.getBlockPos();
            AABB box = new AABB(pos);

            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                Direction connectedDir = ChestBlock.getConnectedDirection(state);
                if (connectedDir != null) {
                    box = box.minmax(new AABB(pos.relative(connectedDir)));
                }
            }
            return box;
        }
        return new AABB(be.getBlockPos());
    }

    private boolean isTargetEntity(Entity entity) {
        List<String> selected = this.entitySelect.getSelectedValues();

        if (selected.contains("Villager") && entity instanceof Villager) {
            return true;
        }
        if (selected.contains("Armor Stand") && entity instanceof ArmorStand) {
            return true;
        }
        if (selected.contains("Named Entity") && this.namedEntities.getCurrentValue() && entity.hasCustomName()) {
            String name = entity.getCustomName().getString().toUpperCase();
            return name.contains("SHOP") || name.contains("CLICK") ||
                    name.contains("UPGRADES") || name.contains("QUEST");
        }
        return false;
    }

    private void interactWithEntity(Entity entity) {
        this.mc.gameMode.interact(this.mc.player, entity, InteractionHand.MAIN_HAND);
        ChatUtils.addChatMessage("Send Interact Packet");
        this.mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void interactWithBlock(BlockHitResult hitResult) {
        this.mc.gameMode.useItemOn(this.mc.player, InteractionHand.MAIN_HAND, hitResult);
        ChatUtils.addChatMessage("Send UseItemOn Packet");
        this.mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static class RenderInfo {
        final Vector2f screenPos;
        final String tag;
        final Color color;

        RenderInfo(Vector2f screenPos, String tag, Color color) {
            this.screenPos = screenPos;
            this.tag = tag;
            this.color = color;
        }
    }
}