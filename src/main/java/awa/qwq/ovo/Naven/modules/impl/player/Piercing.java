package awa.qwq.ovo.Naven.modules.impl.player;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.Priority;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.utils.renderer.Fonts;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.AddonsValue;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
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

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<RenderInfo> renderList = new CopyOnWriteArrayList<>();
    private boolean lastKeyUseState = false;

    @EventTarget(Priority.LOWEST)
    public void onClick(EventClick event) {
        if (this.mc.options == null) {
            return;
        }

        boolean currentKeyUse = this.mc.options.useKey.isPressed();

        if (this.mc.player == null || this.mc.world == null || this.mc.interactionManager == null || this.mc.currentScreen != null) {
            this.lastKeyUseState = currentKeyUse;
            return;
        }

        if (event.isCancelled()) {
            this.lastKeyUseState = currentKeyUse;
            return;
        }

        if (currentKeyUse && !this.lastKeyUseState) {
            double range = this.mc.interactionManager.getReachDistance();
            Vec3d eyePos = this.mc.player.getCameraPosVec(1.0f);
            Vec3d lookVec = this.mc.player.getRotationVector();
            Vec3d reachEnd = eyePos.add(lookVec.multiply(range));

            if (this.findAndInteractWithTarget(eyePos, reachEnd)) {
                event.setCancelled(true);
            }
        }

        this.lastKeyUseState = currentKeyUse;
    }

    @EventTarget
    public void onRender(EventRender event) {
        this.renderList.clear();

        if (!this.renderTags.getCurrentValue() || this.mc.player == null || this.mc.world == null) {
            return;
        }

        double blockRenderRange = 6.0;
        double entityRenderRange = 8.0;
        float partialTicks = event.getRenderPartialTicks();
        Vec3d cameraPos = this.mc.gameRenderer.getCamera().getPos();
        BlockPos playerPos = this.mc.player.getBlockPos();
        for (Entity entity : this.mc.world.getOtherEntities(this.mc.player,
                this.mc.player.getBoundingBox().expand(entityRenderRange))) {
            if (!this.isTargetEntity(entity)) continue;

            Vec3d renderPos = this.getEntityRenderPosition(entity, partialTicks);
            if (renderPos.squaredDistanceTo(cameraPos) > entityRenderRange * entityRenderRange) continue;

            Vector2f screenPos = ProjectionUtils.project(renderPos.x, renderPos.y, renderPos.z, partialTicks);
            if (screenPos == null) continue;

            this.renderList.add(new RenderInfo(screenPos, "[Interact]", Color.CYAN));
        }

        int rangeInt = (int) Math.ceil(blockRenderRange);
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-rangeInt, -rangeInt, -rangeInt),
                playerPos.add(rangeInt, rangeInt, rangeInt))) {
            BlockState state = this.mc.world.getBlockState(pos);
            if (!this.isTargetContainer(pos, state)) continue;

            Vec3d topCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            if (topCenter.squaredDistanceTo(cameraPos) > blockRenderRange * blockRenderRange) continue;

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

    private Vec3d getEntityRenderPosition(Entity entity, float partialTicks) {
        double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks;
        double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        return new Vec3d(x, y + (entity.getHeight() / 2.0f), z);
    }

    private boolean findAndInteractWithTarget(Vec3d eyePos, Vec3d reachEnd) {
        Entity closestEntity = null;
        Vec3d closestEntityHit = null;
        BlockHitResult closestBlockHit = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Entity entity : this.mc.world.getOtherEntities(this.mc.player,
                this.mc.player.getBoundingBox().expand(reachEnd.distanceTo(eyePos)))) {
            if (!this.isTargetEntity(entity)) continue;

            Optional<Vec3d> hitOpt = entity.getBoundingBox().expand(0.1).raycast(eyePos, reachEnd);
            if (!hitOpt.isPresent()) continue;

            double distSq = eyePos.squaredDistanceTo(hitOpt.get());
            if (!(distSq < closestDistSq)) continue;

            closestDistSq = distSq;
            closestEntity = entity;
            closestEntityHit = this.getStableEntityHit(entity);
            closestBlockHit = null;
        }

        for (BlockEntity be : ChunkUtils.getLoadedBlockEntities().toList()) {
            BlockState state = be.getCachedState();
            BlockPos pos = be.getPos();

            if (!this.isTargetContainer(pos, state)) continue;

            Box box = this.getBlockBoundingBox(be);
            if (box == null) continue;

            Optional<Vec3d> hitOpt = box.raycast(eyePos, reachEnd);
            if (!hitOpt.isPresent()) continue;

            double distSq = eyePos.squaredDistanceTo(hitOpt.get());
            if (!(distSq < closestDistSq)) continue;

            closestDistSq = distSq;
            closestBlockHit = this.getStableBlockHit(be.getPos(), box, hitOpt.get(), eyePos);
            closestEntity = null;
            closestEntityHit = null;
        }

        if (closestEntity != null) {
            this.interactWithEntity(closestEntity, closestEntityHit);
            return true;
        } else if (closestBlockHit != null) {
            this.interactWithBlock(closestBlockHit);
            return true;
        }

        return false;
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
        return state.contains(ChestBlock.CHEST_TYPE) && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE;
    }

    private Box getBlockBoundingBox(BlockEntity be) {
        if (be instanceof ChestBlockEntity) {
            BlockState state = be.getCachedState();
            if (!state.contains(ChestBlock.CHEST_TYPE) || state.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) {
                return null;
            }

            BlockPos pos = be.getPos();
            Box box = new Box(pos);

            if (state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                Direction connectedDir = ChestBlock.getFacing(state);
                if (connectedDir != null) {
                    box = box.union(new Box(pos.offset(connectedDir)));
                }
            }
            return box;
        }
        return new Box(be.getPos());
    }

    private Vec3d getStableEntityHit(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
    }

    private BlockHitResult getStableBlockHit(BlockPos pos, Box box, Vec3d hitPos, Vec3d eyePos) {
        Direction face = this.getHitFace(box, hitPos, eyePos);
        Vec3d stableHit = switch (face) {
            case DOWN -> new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            case UP -> new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            case NORTH -> new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ());
            case SOUTH -> new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1.0);
            case WEST -> new Vec3d(pos.getX(), pos.getY() + 0.5, pos.getZ() + 0.5);
            case EAST -> new Vec3d(pos.getX() + 1.0, pos.getY() + 0.5, pos.getZ() + 0.5);
        };

        return new BlockHitResult(stableHit, face, pos, false);
    }

    private Direction getHitFace(Box box, Vec3d hitPos, Vec3d eyePos) {
        double west = Math.abs(hitPos.x - box.minX);
        double east = Math.abs(hitPos.x - box.maxX);
        double down = Math.abs(hitPos.y - box.minY);
        double up = Math.abs(hitPos.y - box.maxY);
        double north = Math.abs(hitPos.z - box.minZ);
        double south = Math.abs(hitPos.z - box.maxZ);

        double min = Math.min(Math.min(Math.min(west, east), Math.min(down, up)), Math.min(north, south));
        if (min == west) return Direction.WEST;
        if (min == east) return Direction.EAST;
        if (min == down) return Direction.DOWN;
        if (min == up) return Direction.UP;
        if (min == north) return Direction.NORTH;
        if (min == south) return Direction.SOUTH;

        if (eyePos.y < box.minY) return Direction.DOWN;
        if (eyePos.y > box.maxY) return Direction.UP;
        if (eyePos.x < box.minX) return Direction.WEST;
        if (eyePos.x > box.maxX) return Direction.EAST;
        if (eyePos.z < box.minZ) return Direction.NORTH;
        return Direction.SOUTH;
    }

    private boolean isTargetEntity(Entity entity) {
        List<String> selected = this.entitySelect.getSelectedValues();

        if (selected.contains("Villager") && entity instanceof VillagerEntity) {
            return true;
        }
        if (selected.contains("Armor Stand") && entity instanceof ArmorStandEntity) {
            return true;
        }
        if (selected.contains("Named Entity") && entity.hasCustomName()) {
            String name = entity.getCustomName().getString().toUpperCase();
            return name.contains("SHOP") || name.contains("CLICK") ||
                    name.contains("UPGRADES") || name.contains("QUEST");
        }
        return false;
    }

    private void interactWithEntity(Entity entity, Vec3d hitPos) {
        if (this.mc.interactionManager == null || this.mc.player == null || hitPos == null) {
            return;
        }

        EntityHitResult hitResult = new EntityHitResult(entity, hitPos);
        this.mc.interactionManager.interactEntityAtLocation(this.mc.player, entity, hitResult, Hand.MAIN_HAND);
        this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
        ChatUtils.addChatMessage("Send InteractAt + Interact Packet");
        this.mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void interactWithBlock(BlockHitResult hitResult) {
        this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
        ChatUtils.addChatMessage("Send UseItemOn Packet");
        this.mc.player.swingHand(Hand.MAIN_HAND);
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
