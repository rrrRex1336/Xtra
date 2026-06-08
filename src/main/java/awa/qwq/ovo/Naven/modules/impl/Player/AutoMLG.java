package awa.qwq.ovo.Naven.modules.impl.Player;

import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.impl.Misc.Helper;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.ui.notification.Notification;
import awa.qwq.ovo.Naven.ui.notification.NotificationLevel;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import lombok.Getter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@ModuleInfo(
        name = "AutoMLG",
        description = "Automatically places water when falling",
        category = Category.PLAYER
)
public class AutoMLG extends Module {
    FloatValue distance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();
    BooleanValue antiFire = ValueBuilder.create(this, "Anti Fire")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    BooleanValue antiWeb = ValueBuilder.create(this, "Anti Web")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private boolean waitingForSkipTicks = false;
    public boolean rotation = false;
    public BlockPos above;
    private boolean placeWater = false;
    public int originalSlot;
    private int timeout;

    public AutoMLG() {
        collectingWater = false;
    }

    public static boolean isOnGround(double height) {
        Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, height, 0.0));
        return collisions.iterator().hasNext();
    }

    @EventTarget
    public void onPre(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            if (this.antiWeb.getCurrentValue() && mc.player.isInWater() && !this.rotation) {
                BlockPos playerPos = mc.player.blockPosition();
                if (mc.level.getBlockState(playerPos).getBlock() == Blocks.COBWEB) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = mc.player.getInventory().getItem(i);
                        if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
                            this.originalSlot = mc.player.getInventory().selected;
                            mc.player.getInventory().selected = i;
                            this.rotation = true;
                            this.placeWater = true;
                            this.timeout = 5;
                            break;
                        }
                    }
                }
            }
            if (this.antiFire.getCurrentValue() && mc.player.isOnFire() && !this.rotation) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = mc.player.getInventory().getItem(i);
                    if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
                        this.originalSlot = mc.player.getInventory().selected;
                        mc.player.getInventory().selected = i;
                        this.rotation = true;
                        this.placeWater = true;
                        this.timeout = 5;
                        break;
                    }
                }
            }
            if (mc.player.fallDistance > this.distance.getCurrentValue()) {
                if (this.rotation && isOnGround(mc.player.getDeltaMovement().y)) {
                    this.placeWater = true;
                } else if (isOnGround(mc.player.getDeltaMovement().y * 3.0)) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = mc.player.getInventory().getItem(i);
                        if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
                            this.originalSlot = mc.player.getInventory().selected;
                            mc.player.getInventory().selected = i;
                            this.rotation = true;
                            this.timeout = 5;
                            this.placingWater = true;
                            break;
                        }
                    }
                }
            }

            if (--this.timeout == 0 && this.rotation) {
                mc.player.getInventory().selected = this.originalSlot;
                this.rotation = false;
                this.placingWater = false;
                this.collectingWater = false;
                this.above = null;
                this.rotation = false;
                this.placingWater = false;
                this.collectingWater = false;
                mc.player.getInventory().selected = this.originalSlot;
            }
        }
    }

    @Getter
    @SuppressWarnings("Lombok")
    public boolean collectingWater;

    @EventTarget
    public void onClick(EventClick e) {
        if (this.placeWater) {
            this.placeWater = false;
            if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK && ((BlockHitResult) mc.hitResult).getDirection() == Direction.UP) {
                this.above = ((BlockHitResult) mc.hitResult).getBlockPos().above();
                this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
                Vector2f lookAtRotation = calculateLookAt(this.above);
                RotationManager.setRotations(lookAtRotation);
                this.placingWater = false;

                Helper helper = (Helper) Naven.getInstance().getModuleManager().getModule(Helper.class);
                boolean helperCollecting = helper != null && helper.isEnabled() && helper.collectWater.getCurrentValue();

                if (helperCollecting) {
                    this.collectingWater = false;
                    this.rotation = false;
                    mc.player.getInventory().selected = this.originalSlot;
                    this.above = null;
                    Notification notification = new Notification(NotificationLevel.SUCCESS, "Auto MLG placed water!", 3000L);
                    Naven.getInstance().getNotificationManager().addNotification(notification);
                } else {
                    this.collectingWater = true;
                    if (SkipTicks.isActive()) {
                        this.waitingForSkipTicks = true;
                    }
                }
            } else {
                Notification notification = new Notification(NotificationLevel.WARNING, "Failed to place water!", 3000L);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                this.rotation = false;
                this.placingWater = false;
                this.collectingWater = false;
                mc.player.getInventory().selected = this.originalSlot;
            }
            return;
        }

        if (this.collectingWater) {
            if (waitingForSkipTicks && SkipTicks.isActive()) {
                return;
            }
            waitingForSkipTicks = false;

            if (mc.hitResult instanceof BlockHitResult hitResult && mc.level.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.WATER) {
                this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
            } else {
                this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
                Notification notification = new Notification(NotificationLevel.SUCCESS, "Water recycled!", 3000L);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                this.collectingWater = false;
                mc.player.getInventory().selected = this.originalSlot;
            }
            mc.player.getInventory().selected = this.originalSlot;
            this.rotation = false;
            this.placingWater = false;
            this.above = null;
            this.waitingForSkipTicks = false;
        }
    }

    private boolean placingWater = false;

    public Vector2f calculateLookAt(BlockPos pos) {
        Vec3 eyesPos = mc.player.getEyePosition();
        Vec3 blockCenter = Vec3.atCenterOf(pos);

        double diffX = blockCenter.x - eyesPos.x;
        double diffY = blockCenter.y - eyesPos.y;
        double diffZ = blockCenter.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float)-Math.toDegrees(Math.atan2(diffY, diffXZ));

        return new Vector2f(yaw, pitch);
    }

    public boolean isMLGActive() {
        return rotation || placingWater || collectingWater;
    }

    public boolean isPlacingWater() {
        return rotation && !collectingWater;
    }

    public InteractionResult useItem(Player pPlayer, Level pLevel, InteractionHand pHand) {
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else {
            PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(pHand, id));
            mc.player.swing(InteractionHand.MAIN_HAND);
            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
                return InteractionResult.PASS;
            } else {
                InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(pLevel, pPlayer, pHand);
                ItemStack itemstack1 = interactionresultholder.getObject();

                if (itemstack1 != itemstack) {
                    pPlayer.setItemInHand(pHand, itemstack1);
                }
                return interactionresultholder.getResult();
            }
        }
    }
}