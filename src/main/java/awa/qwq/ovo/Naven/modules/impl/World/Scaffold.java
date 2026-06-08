package awa.qwq.ovo.Naven.modules.impl.World;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.annotations.FlowExclude;
import awa.qwq.ovo.Naven.annotations.ParameterObfuscationExclude;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.*;
import awa.qwq.ovo.Naven.modules.impl.Player.AutoMLG;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;
import java.util.*;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.RandomUtils;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.WORLD
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE, Blocks.GLASS_PANE, Blocks.GLASS_PANE, Blocks.IRON_BARS, Blocks.SNOW, Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.TORCH, Blocks.ANVIL, Blocks.TRAPPED_CHEST, Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.TNT, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.STONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK, Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.VINE, Blocks.SUNFLOWER, Blocks.LADDER, Blocks.FURNACE, Blocks.SAND, Blocks.CACTUS, Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN, Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH, Blocks.FLOWER_POT);
   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();
   private int bigVelocityTick = 0;
   public static boolean reachable;
   private final Random random = new Random();
   private boolean stopMove = false;
   private int tickCounter = 0;
   private int lastSwitchSlot = -1;
   private int placeSuccessTimer = 0;
   private int fallCheckTimer = 0;
   private boolean waitingForGround = false;
   private int sameYCounter = 0;
   private boolean working;
   private ItemStack itemStack = null;
   private int lastSelectedSlot = -1;

   private BlockPos lastPlacedPos = null;
   private int lastPlacedTimer = 0;

   public ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Normal", "Telly Bridge", "Keep Y")
           .build()
           .getModeValue();

   public ModeValue swingMode = ValueBuilder.create(this, "Swing Mode")
           .setDefaultModeIndex(0)
           .setModes("Clientside", "Serverside")
           .build()
           .getModeValue();

   public ModeValue switchMode = ValueBuilder.create(this, "Switch Mode")
           .setDefaultModeIndex(0)
           .setModes("Pick", "Spoof", "Switch")
           .build()
           .getModeValue();

   public ModeValue rotMode = ValueBuilder.create(this, "Rotation Mode")
           .setDefaultModeIndex(0)
           .setModes("KeyBinds Yaw", "Strict", "Sideways", "Sideways2")
           .build()
           .getModeValue();

   FloatValue sidewaysYaw = ValueBuilder.create(this, "Sideways Yaw")
           .setDefaultFloatValue(45.0F)
           .setMaxFloatValue(55.0F)
           .setMinFloatValue(5.0F)
           .setFloatStep(5.0F)
           .setVisibility(() -> this.rotMode.isCurrentMode("Sideways"))
           .build()
           .getFloatValue();

   public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> this.mode.isCurrentMode("Normal"))
           .build()
           .getBooleanValue();

   public BooleanValue vulcan = ValueBuilder.create(this, "Vulcan")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue movementCorrection = ValueBuilder.create(this, "Movement Correction")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public BooleanValue clutch = ValueBuilder.create(this, "Clutch(Stop Send Move Packet)")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue smooth = ValueBuilder.create(this, "Smooth")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   FloatValue smoothValue = ValueBuilder.create(this, "Smooth Value")
           .setDefaultFloatValue(25F)
           .setMaxFloatValue(50F)
           .setMinFloatValue(5F)
           .setFloatStep(5F)
           .setVisibility(() -> this.smooth.getCurrentValue())
           .build()
           .getFloatValue();

   FloatValue rotateMinSpeed = ValueBuilder.create(this, "Rotation Min Speed")
           .setDefaultFloatValue(180)
           .setMaxFloatValue(180)
           .setMinFloatValue(5)
           .setFloatStep(5)
           .build()
           .getFloatValue();

   FloatValue rotateMaxSpeed = ValueBuilder.create(this, "Rotation Max Speed")
           .setDefaultFloatValue(180)
           .setMaxFloatValue(180)
           .setMinFloatValue(5)
           .setFloatStep(5)
           .build()
           .getFloatValue();

   {rotateMinSpeed.linkAsMin(rotateMaxSpeed);rotateMaxSpeed.linkAsMax(rotateMinSpeed);}

   int oldSlot;
   private BlockPos pos;
   private int lastSneakTicks;
   public int baseY = -1;
   private boolean cancelMove = false;
   private boolean hasSentNoReachableMessage = false;
   private int moveCooldown = 0;
   @Getter
   private boolean lastPlaceSuccess = false;
   @Getter
   private BlockPos lastSuccessPos = null;
   private TimeHelper safeTimer = new TimeHelper();
   private int cancelTicks = 0;
   private boolean wasCancelledForPlacement = false;
   @Getter
   private BlockPos lastPlacePos = null;
   private Vector2f targetRotation = null;

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
         return false;
      } else if (!InventoryUtils.isItemValid(stack)) {
         return false;
      } else {
         String string = stack.getDisplayName().getString();
         if (string.contains("Click") || string.contains("点击")) {
            return false;
         } else if (stack.getItem() instanceof ItemNameBlockItem) {
            return false;
         } else {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof BushBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
            }
         }
      }
   }

   private static Vec3 getVec3(BlockPos checkPosition, BlockState block) {
      VoxelShape shape = block.getShape(mc.level, checkPosition);
      double ex = MathHelper.clamp(mc.player.getX(), checkPosition.getX(), checkPosition.getX() + shape.max(Axis.X));
      double ey = MathHelper.clamp(mc.player.getY(), checkPosition.getY(), checkPosition.getY() + shape.max(Axis.Y));
      double ez = MathHelper.clamp(mc.player.getZ(), checkPosition.getZ(), checkPosition.getZ() + shape.max(Axis.Z));
      return new Vec3(ex, ey, ez);
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level.getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-sensitivity, 0.0, -sensitivity)).iterator().hasNext();
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         cancelMove = false;
         hasSentNoReachableMessage = false;
         moveCooldown = 0;
         lastPlaceSuccess = false;
         lastSuccessPos = null;
         cancelTicks = 0;
         wasCancelledForPlacement = false;
         this.targetRotation = null;
         tickCounter = 0;
         stopMove = false;
         this.oldSlot = mc.player.getInventory().selected;
         this.lastSelectedSlot = this.oldSlot;
         this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
         this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
         this.pos = null;
         bigVelocityTick = 0;
         reachable = true;
         lastPlacedPos = null;
         lastPlacedTimer = 0;
         this.itemStack = null;
      }
   }

   @Override
   public void onDisable() {
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
      boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getDefaultKey().getValue());
      mc.options.keyJump.setDown(isHoldingJump);
      mc.options.keyShift.setDown(isHoldingShift);
      mc.options.keyUse.setDown(false);
      if (switchMode.isCurrentMode("Switch") && this.itemStack != null) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(this.oldSlot));
         this.itemStack = null;
      }
      mc.player.getInventory().selected = this.oldSlot;
      this.targetRotation = null;
      bigVelocityTick = 0;
      stopMove = false;
      reachable = true;
      tickCounter = 0;
      placeSuccessTimer = 0;
      working = false;
      fallCheckTimer = 0;
      waitingForGround = false;
      sameYCounter = 0;
      SkipTicks.dispatch();
      cancelMove = false;
      hasSentNoReachableMessage = false;
      moveCooldown = 0;
      lastPlaceSuccess = false;
      lastSuccessPos = null;
      cancelTicks = 0;
      wasCancelledForPlacement = false;
      tickCounter = 0;
      lastPlacedPos = null;
      lastPlacedTimer = 0;
      lastSelectedSlot = -1;
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (e.getHand() != InteractionHand.MAIN_HAND) return;

      if (switchMode.isCurrentMode("Spoof")) {
         e.setItem(mc.player.getInventory().getItem(this.oldSlot));
      }
   }

   @EventTarget(1)
   public void onPerTicks(EventRunTicks e) {
      if (lastPlacedTimer > 0) {
         lastPlacedTimer--;
      }

      boolean shouldUpdateBaseY = false;
      if (baseY == -0.0) {
         shouldUpdateBaseY = true;
      } else if (mc.player.onGround()) {
         shouldUpdateBaseY = true;
      }

      if (shouldUpdateBaseY) {
         int newBaseY = (int) (mc.player.getY() - 0.0);
         if (baseY != newBaseY) {
            baseY = newBaseY;
         }
      }
      if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
         if (cancelMove) {
            cancelTicks++;
         }

         int currentSelected = mc.player.getInventory().selected;
         if (lastSelectedSlot != -1 && lastSelectedSlot != currentSelected) {
            if (switchMode.isCurrentMode("Switch") && this.itemStack != null) {
               this.itemStack = null;
            }
         }
         lastSelectedSlot = currentSelected;

         AutoMLG autoMLG = (AutoMLG) Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
         boolean mlgActive = autoMLG != null && autoMLG.isEnabled() && autoMLG.isMLGActive();
         ItemStack currentItem = mc.player.getMainHandItem();
         boolean holdingValidBlock = currentItem.getItem() instanceof BlockItem && isValidStack(currentItem);

         int slotID = -1;
         if (!mlgActive || !holdingValidBlock) {
            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
                  slotID = i;
                  break;
               }
            }

            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
               switch (switchMode.getCurrentMode()) {
                  case "Pick":
                     mc.player.getInventory().selected = slotID;
                     this.itemStack = null;
                     break;
                  case "Spoof":
                     mc.player.getInventory().selected = slotID;
                     this.itemStack = null;
                     break;
                  case "Switch":
                     mc.getConnection().send(new ServerboundSetCarriedItemPacket(slotID));
                     this.itemStack = mc.player.getInventory().getItem(slotID).copy();
                     this.lastSwitchSlot = slotID;
                     break;
               }
            }
         }

         this.pos = this.getBlockPos();

         if (this.pos != null) {
            this.correctRotation = this.getPlayerYawRotation();

            if (this.mode.isCurrentMode("Normal")) {
               this.rots.setX(this.correctRotation.getX());
               this.rots.setY(this.correctRotation.getY());
               this.targetRotation = null;
            } else {
               float minSpeed = this.rotateMinSpeed.getCurrentValue();
               float maxSpeed = this.rotateMaxSpeed.getCurrentValue();
               float currentSpeed = minSpeed + (random.nextFloat() * (maxSpeed - minSpeed));

               if (this.smooth.getCurrentValue() && this.smoothValue.getCurrentValue() > 0) {
                  if (this.targetRotation == null) {
                     this.targetRotation = new Vector2f(this.correctRotation.getX(), this.correctRotation.getY());
                  }
                  float finalYawDiff = RotationUtils.getAngleDifference(this.targetRotation.getX(), this.correctRotation.getX());
                  float finalPitchDiff = this.targetRotation.getY() - this.correctRotation.getY();

                  if (Math.abs(finalYawDiff) > 0.1F || Math.abs(finalPitchDiff) > 0.1F) {
                     this.targetRotation = new Vector2f(this.correctRotation.getX(), this.correctRotation.getY());
                  }
                  float yawToFinal = RotationUtils.getAngleDifference(this.rots.getX(), this.targetRotation.getX());
                  float pitchToFinal = this.targetRotation.getY() - this.rots.getY();
                  float steps = this.smoothValue.getCurrentValue();
                  float yawStep = yawToFinal / steps;
                  float pitchStep = pitchToFinal / steps;

                  float intermediateYaw = this.rots.getX() + yawStep;
                  float intermediatePitch = this.rots.getY() + pitchStep;
                  this.rots.setX(RotationUtils.rotateToYaw(currentSpeed, this.rots.getX(), intermediateYaw));
                  this.rots.setY(RotationUtils.rotateToYaw(currentSpeed, this.rots.getY(), intermediatePitch));
               } else {
                  this.rots.setX(RotationUtils.rotateToYaw(currentSpeed, this.rots.getX(), this.correctRotation.getX()));
                  this.rots.setY(RotationUtils.rotateToYaw(currentSpeed, this.rots.getY(), this.correctRotation.getY()));
                  this.targetRotation = null;
               }
            }
         }
         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
         if (this.vulcan.getCurrentValue()) {
            this.lastSneakTicks++;
            System.out.println(this.lastSneakTicks);
            if (this.lastSneakTicks == 18) {
               if (mc.player.isSprinting()) {
                  mc.options.keySprint.setDown(false);
                  mc.player.setSprinting(false);
               }

               mc.options.keyShift.setDown(true);
            } else if (this.lastSneakTicks >= 21) {
               mc.options.keyShift.setDown(false);
               this.lastSneakTicks = 0;
            }
         }

         if (this.mode.isCurrentMode("Telly Bridge")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            if (mc.player.onGround() && PlayerUtils.movementInput()) {
               this.rots.setX(RotationUtils.rotateToYaw(360F, this.rots.getX(), mc.player.getYRot()));
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }
         } else if (this.mode.isCurrentMode("Keep Y")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);

         } else {
            if (this.eagle.getCurrentValue()) {
               mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
            }
         }

         this.lastRots.set(this.rots.getX(), this.rots.getY());
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ClientboundSetEntityMotionPacket velocity
              && mc.player != null
              && velocity.getId() == mc.player.getId()) {

         double dx = velocity.getXa() / 8000.0;
         double dy = velocity.getYa() / 8000.0;
         double dz = velocity.getZa() / 8000.0;
         double strength = Math.sqrt(dx * dx + dy * dy + dz * dz);

         if (strength >= 1.5D) {
            ChatUtils.addChatMessage("你也是要飞了: " + strength);
            bigVelocityTick = 60;
         }

         if (!(strength <= 0.3D)) {
            if (!cancelMove) {
               SkipTicks.skipTicks(10);
               cancelMove = true;
               stopMove = true;
               cancelTicks = 0;
               wasCancelledForPlacement = true;
            }
         }
      }
   }

   private void onClutch() {
      if (stopMove && cancelMove) {
         tickCounter++;
      }

      if (EventStuck.isMoveCancelled() || SkipTicks.isActive()) {
         if (!working) {
            working = true;
            ChatUtils.addChatMessage("working");
         }
      } else {
         working = false;
      }
      if (!cancelMove || !wasCancelledForPlacement) {
         return;
      }

      cancelTicks++;
      if (cancelTicks >= 8) {
         if (stopMove) {
            EventStuck.resetMove();
            SkipTicks.dispatch();
            stopMove = false;
         }
         cancelMove = false;
         moveCooldown = 0;
         cancelTicks = 0;
         tickCounter = 0;
      }
      if (pos == null) return;

      HitResult objectPosition = RayTraceUtils.rayCast(1.0F, RotationManager.rotations);
      BlockPos belowPos = new BlockPos((int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
      BlockState blockState = mc.level.getBlockState(belowPos);

      if (!blockState.isAir() && !blockState.liquid()) {
         if (stopMove && cancelMove) {
            SkipTicks.dispatch();
            stopMove = false;
         }
         cancelMove = false;
         reachable = true;
         wasCancelledForPlacement = false;
         lastPlaceSuccess = false;
         tickCounter = 0;
         moveCooldown = 0;
         return;
      }

      BlockHitResult position = (BlockHitResult) objectPosition;
      double distance = mc.player.distanceToSqr(position.getBlockPos().getX() + 0.5, position.getBlockPos().getY() + 0.5, position.getBlockPos().getZ() + 0.5);
      boolean isBelowBaseY = baseY == -1 || mc.player.getY() +1 < baseY;

      if (distance > 25.0 || !clutch.getCurrentValue() || !isBelowBaseY) {
         lastPlaceSuccess = false;
         if (stopMove && cancelMove) {
            SkipTicks.dispatch();
            stopMove = false;
         }
         cancelMove = false;
         wasCancelledForPlacement = false;
         reachable = true;
         tickCounter = 0;
         moveCooldown = 0;
         return;
      }

      if (moveCooldown > 8) {
         lastPlaceSuccess = false;
         if (stopMove && cancelMove) {
            SkipTicks.dispatch();
            stopMove = false;
         }
         cancelMove = false;
         wasCancelledForPlacement = false;
         reachable = true;
         tickCounter = 0;
         moveCooldown = 0;
         return;
      }

      lastPlaceSuccess = true;
      safeTimer.reset();

      if (!cancelMove) {
         SkipTicks.skipTicks(8);
         cancelMove = true;
         reachable = false;
         stopMove = true;
      }

      wasCancelledForPlacement = true;
      cancelTicks = 0;
      moveCooldown = 0;
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player != null) {
         this.place();
      }
      e.setCancelled(true);
   }

   private void place() {
      if (this.pos != null) {
         onClutch();
         HitResult objectPosition = RayTraceUtils.rayCast(1.0F, RotationManager.rotations);
         if (objectPosition.getType() == Type.BLOCK) {
            BlockHitResult position = (BlockHitResult) objectPosition;
            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
             if (mc.player != null && position.getBlockPos().equals(this.pos) && (position.getDirection() != Direction.UP || mc.player.onGround() || isHoldingJump || this.mode.isCurrentMode("Normal"))) {
                 HitResult hitResult = RayTraceUtils.rayCast(1.0F, RotationManager.lastRotations);
                 boolean isLastRotationValid = true;
                 if (hitResult.getType() != Type.BLOCK) {
                     isLastRotationValid = false;
                 } else {
                     BlockHitResult lastPosition = (BlockHitResult) hitResult;
                     if (!lastPosition.getBlockPos().equals(position.getBlockPos())) {
                         isLastRotationValid = false;
                     }
                 }
                 if (!isLastRotationValid) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                 }

                 if (mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, position) == InteractionResult.SUCCESS) {
                     if (switchMode.isCurrentMode("Switch") && this.lastSwitchSlot != -1) {
                         mc.getConnection().send(new ServerboundSetCarriedItemPacket(this.oldSlot));
                     }
                     lastPlacePos = position.getBlockPos();
                     BlockPos newPlacedPos = position.getBlockPos().relative(position.getDirection());
                     lastPlacedPos = newPlacedPos;
                     if (swingMode.isCurrentMode("Clientside")) {
                         mc.player.swing(InteractionHand.MAIN_HAND);
                     }
                 }
             }
         }
      }
   }

   @EventTarget
   public void onRender(EventRender event) {
      if (lastPlacedPos == null || mc.gameRenderer == null) return;

      PoseStack poseStack = event.getPMatrixStack();
      Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

      poseStack.pushPose();
      poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
      AABB box = new AABB(lastPlacedPos);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      Color color = new Color(74, 144, 226);

      RenderSystem.setShaderColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.25f);
      RenderUtils.drawSolidBox(box, poseStack);
      RenderSystem.setShaderColor(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.75f);
      RenderUtils.drawOutlineBox(box, poseStack);

      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);

      poseStack.popPose();
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private Vector2f getPlayerYawRotation() {

      float rotationYaw = mc.player.getYRot() - 180.0F;
      if (this.isTower()) {
         HitResult objectPosition = mc.hitResult;
         if (objectPosition != null) {
            float pitch = 90.0F;
            return new Vector2f(rotationYaw, pitch);
         }
      }

      float pitch;
      if (rotMode.isCurrentMode("Sideways2")) {
         pitch = 87F;
      } else {
         pitch = 82F;
      }

      Vector2f rotations = new Vector2f(rotationYaw, pitch);
      float realYaw = mc.player.getYRot();
      float magic = RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F;
      if (this.rotMode.isCurrentMode("KeyBinds Yaw")) {
         if (mc.options.keyDown.isDown()) {
            realYaw += 180.0F;
            if (mc.options.keyLeft.isDown()) {
               realYaw += 45.0F;
            } else if (mc.options.keyRight.isDown()) {
               realYaw -= 45.0F;
            }
         } else if ((mc.options.keyDown.isDown())) {
            if (rotMode.isCurrentMode("Sideways2")) {
               realYaw += 45F;
            }
            if (mc.options.keyLeft.isDown()) {
               realYaw -= 45.0F;
            } else if (mc.options.keyRight.isDown()) {
               realYaw += 45.0F;
            }
         } else if (mc.options.keyUp.isDown()) {
            if (mc.options.keyLeft.isDown()) {
               realYaw -= 45.0F;
            } else if (mc.options.keyRight.isDown()) {
               realYaw += 45.0F;
            }
         } else if (mc.options.keyRight.isDown()) {
            realYaw += 90.0F;
         } else if (mc.options.keyLeft.isDown()) {
            realYaw -= 90.0F;
         }
      }

      BlockPos targetPos = getBlockPos();

      if (rotMode.isCurrentMode("Sideways") && targetPos != null) {
         double deltaX = (targetPos.getX() + 0.5) - mc.player.getX();
         double deltaY = (targetPos.getY() + 0.5) - mc.player.getEyePosition().y;
         double deltaZ = (targetPos.getZ() + 0.5) - mc.player.getZ();

         double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
         targetYaw = MathHelper.wrapDegrees(targetYaw);

         float currentYaw = mc.player.yHeadRot;
         currentYaw = MathHelper.wrapDegrees(currentYaw);
         float currentPitch = mc.player.getXRot();
         float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
         float maxTurn = sidewaysYaw.getCurrentValue();
         if (Math.abs(yawDiff) > maxTurn) {
            yawDiff = Math.signum(yawDiff) * maxTurn;
         }

         realYaw = MathHelper.wrapDegrees(currentYaw + yawDiff);
         if (mc.options.keyDown.isDown()) {
            float backYaw = MathHelper.wrapDegrees(realYaw + 180.0F);
            if (Math.abs(MathHelper.wrapDegrees(backYaw - currentYaw)) <= maxTurn) {
               realYaw = backYaw;
            }
         }
      }
      if (rotMode.isCurrentMode("Strict") && targetPos != null) {
         double deltaX = (targetPos.getX() + 0.5) - mc.player.getX();
         double deltaZ = (targetPos.getZ() + 0.5) - mc.player.getZ();
         float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         realYaw = MathHelper.wrapDegrees(targetYaw);
         if (mc.options.keyDown.isDown()) {
            realYaw = MathHelper.wrapDegrees(realYaw + 180.0F);
         }
      }
      if (rotMode.isCurrentMode("Sideways2") && targetPos != null) {
         double deltaX = (targetPos.getX() + 0.5) - mc.player.getX();
         double deltaY = (targetPos.getY() + 0.5) - mc.player.getEyePosition().y;
         double deltaZ = (targetPos.getZ() + 0.5) - mc.player.getZ();

         double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
         targetYaw = MathHelper.wrapDegrees(targetYaw);
         float currentYaw = mc.player.yHeadRot;
         currentYaw = MathHelper.wrapDegrees(currentYaw);
         float normalizedYaw = MathHelper.wrapDegrees(currentYaw);
         float yawDiff = MathHelper.wrapDegrees(targetYaw - normalizedYaw);
         float roundedDiff = Math.round(yawDiff / 45.0f) * 45.0f;
         if (Math.abs(roundedDiff % 90.0f) < 5.0f) {
            realYaw = targetYaw;
         } else {
            float absDiff = Math.abs(yawDiff);
            if (absDiff <= 15.0f) {
               realYaw = normalizedYaw;
            } else if (absDiff <= 30.0f) {
               realYaw = MathHelper.wrapDegrees(normalizedYaw + (yawDiff * 0.5f));
            } else {
               realYaw = MathHelper.wrapDegrees(normalizedYaw + Math.signum(yawDiff) * Math.min(absDiff, 30.0f));
            }
         }
         if (mc.options.keyDown.isDown()) {
            realYaw = MathHelper.wrapDegrees(realYaw + 180.0F);
         }
      }

      float yaw = realYaw - 180.0F + magic;
      rotations.setX(yaw);
      if (this.shouldBuild()) {
         HitResult initialHit = this.performRayCast(rotations);
         if (this.isHitValid(initialHit)) {
            return rotations;
         }

         ArrayList<Float> validPitches = this.findValidPitches(yaw);
         if (!validPitches.isEmpty()) {
            validPitches.sort(Comparator.comparingDouble(this::distanceToLastPitch));
            rotations.setY(validPitches.get(0));
            return rotations;
         }

         Vector2f optimalRotation = this.findOptimalRotation(yaw);
         if (optimalRotation != null) {
            return optimalRotation;
         }
      }

      return rotations;
   }

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
   }

   private double distanceToLastPitch(float pitch) {
      return Math.abs(pitch - this.rots.y);
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private ArrayList<Float> findValidPitches(float yaw) {
      ArrayList<Float> validPitches = new ArrayList<>();

      for (float i = Math.max(this.rots.y - 30.0F, -90.0F); i < Math.min(this.rots.y + 20.0F, 90.0F); i += 0.3F) {
         Vector2f fixed = RotationUtils.getFixedRotation(yaw, i, this.rots.x, this.rots.y);
         HitResult position = this.performRayCast(new Vector2f(yaw, fixed.y));
         if (this.isHitValid(position)) {
            validPitches.add(fixed.y);
         }
      }

      return validPitches;
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private HitResult performRayCast(Vector2f rotations) {
      return RayTraceUtils.rayCast(1.0F, rotations);
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private boolean isHitValid(HitResult hit) {
      Type block = Type.BLOCK;
      if (hit.getType() != block) {
         return false;
      } else {
         BlockHitResult blockHit = (BlockHitResult)hit;
         return this.isValidBlock(blockHit.getBlockPos()) && this.isNearbyBlockPos(blockHit.getBlockPos()) && blockHit.getDirection() != Direction.DOWN && blockHit.getDirection() != Direction.UP;
      }
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private Vector2f findOptimalRotation(float yaw) {
      for (float yawLoops = 0.0F; yawLoops < 360.0F; yawLoops += 2.0F) {
         float currentPitch = this.rots.y;

         for (float pitchLoops = 0.0F; pitchLoops < 50.0F; pitchLoops += 2.0F) {
            for (int i = 0; i < 2; i++) {
               float pitch = currentPitch - pitchLoops * (i == 0 ? 1 : -1);
               float[][] offsets = new float[][]{{yaw + yawLoops, pitch}, {yaw - yawLoops, pitch}};

               for (float[] rotation : offsets) {
                  float rayCastPitch = MathHelper.clamp(rotation[1], -90.0F, 90.0F);
                  Vector2f fixedRotation = RotationUtils.getFixedRotation(rotation[0], rayCastPitch, this.rots.x, this.rots.y);
                  HitResult position = this.performRayCast(fixedRotation);
                  if (this.isHitValid(position)) {
                     return fixedRotation;
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean isNearbyBlockPos(BlockPos blockPos) {
      if (!mc.player.onGround()) {
         return blockPos.equals(this.pos);
      } else {
         for (int x = this.pos.getX() - 1; x <= this.pos.getX() + 1; x++) {
            for (int z = this.pos.getZ() - 1; z <= this.pos.getZ() + 1; z++) {
               if (blockPos.equals(new BlockPos(x, this.pos.getY(), z))) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private BlockPos getBlockPos() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
      ArrayList<Vec3> positions = new ArrayList<>();
      HashMap<Vec3, BlockPos> hashMap = new HashMap<>();

      for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; x++) {
         for (int y = playerPos.getY() - 1; y <= playerPos.getY(); y++) {
            for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; z++) {
               BlockPos checkPosition = new BlockPos(x, y, z);
               if (this.isValidBlock(checkPosition)) {
                  BlockState block = mc.level.getBlockState(checkPosition);
                  Vec3 vec3 = getVec3(checkPosition, block);
                  positions.add(vec3);
                  hashMap.put(vec3, checkPosition);
               }
            }
         }
      }

      if (!positions.isEmpty()) {
         positions.sort(Comparator.comparingDouble(this::getBlockDistance));
         return this.isTower() && hashMap.get(positions.get(0)).getY() != mc.player.getY() - 1.5
                 ? BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.5, mc.player.getZ())
                 : hashMap.get(positions.get(0));
      } else {
         return null;
      }
   }

   public boolean isValidBlock(BlockPos blockPos) {
      Block block = mc.level.getBlockState(blockPos).getBlock();
      return !(block instanceof LiquidBlock)
              && !(block instanceof AirBlock)
              && !(block instanceof ChestBlock)
              && !(block instanceof FurnaceBlock)
              && !(block instanceof EnderChestBlock)
              && !(block instanceof TallGrassBlock)
              && !(block instanceof SnowLayerBlock)
              && !(block instanceof EnchantmentTableBlock)
              && !(block instanceof AnvilBlock)
              && !(block instanceof CraftingTableBlock);
   }

   private boolean isTower() {
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
      return isHoldingJump && !mc.options.keyUp.isDown() && !mc.options.keyDown.isDown() && !mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown();
   }

   private double getBlockDistance(Vec3 vec3) {
      return mc.player.distanceToSqr(vec3.x, vec3.y, vec3.z);
   }

   public int getBlockCount() {
      if (mc.player == null) return 0;

      int totalBlocks = 0;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem) {
            totalBlocks += stack.getCount();
         }
      }
      ItemStack offhandStack = mc.player.getOffhandItem();
      if (offhandStack.getItem() instanceof BlockItem) {
         totalBlocks += offhandStack.getCount();
      }

      return totalBlocks;
   }
}