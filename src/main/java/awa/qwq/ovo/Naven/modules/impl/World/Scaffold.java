package awa.qwq.ovo.Naven.modules.impl.World;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.Player.AutoMLG;
import awa.qwq.ovo.Naven.utils.InventoryUtils;
import awa.qwq.ovo.Naven.utils.MathHelper;
import awa.qwq.ovo.Naven.utils.NetworkUtils;
import awa.qwq.ovo.Naven.utils.PlayerUtils;
import awa.qwq.ovo.Naven.utils.RayTraceUtils;
import awa.qwq.ovo.Naven.utils.RenderUtils;
import awa.qwq.ovo.Naven.utils.Vector2f;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.WORLD
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(
           Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE, Blocks.GLASS_PANE, Blocks.IRON_BARS,
           Blocks.SNOW, Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.CHEST, Blocks.TRAPPED_CHEST,
           Blocks.TORCH, Blocks.ANVIL, Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.TNT, Blocks.GOLD_ORE, Blocks.IRON_ORE,
           Blocks.LAPIS_ORE, Blocks.STONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
           Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS,
           Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK, Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM,
           Blocks.BROWN_MUSHROOM, Blocks.VINE, Blocks.SUNFLOWER, Blocks.LADDER, Blocks.FURNACE, Blocks.SAND,
           Blocks.CACTUS, Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN,
           Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH, Blocks.FLOWER_POT
   );

   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();
   public static boolean reachable = true;

   public ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(1)
           .setModes("Normal", "Telly Bridge")
           .build()
           .getModeValue();

   public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> this.mode.isCurrentMode("Normal"))
           .build()
           .getBooleanValue();

   public BooleanValue sneak = ValueBuilder.create(this, "Sneak")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public BooleanValue movementCorrection = ValueBuilder.create(this, "Movement Fix")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public BooleanValue renderPlacement = ValueBuilder.create(this, "Render Placement")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public BooleanValue clutch = ValueBuilder.create(this, "Clutch")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public BooleanValue multiPlace = ValueBuilder.create(this, "MultiPlace")
           .setDefaultBooleanValue(false)
           .setVisibility(() -> this.clutch.getCurrentValue())
           .build()
           .getBooleanValue();

   public BooleanValue swing = ValueBuilder.create(this, "Swing")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
           .setDefaultFloatValue(75.0F)
           .setMinFloatValue(30.0F)
           .setMaxFloatValue(180.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   public FloatValue tellyTick = ValueBuilder.create(this, "Telly Tick")
           .setDefaultFloatValue(3.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(3.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
           .build()
           .getFloatValue();

   public FloatValue placeDelayTicks = ValueBuilder.create(this, "Place Delay")
           .setDefaultFloatValue(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(20.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

   public FloatValue clutchTicks = ValueBuilder.create(this, "Skip Ticks")
           .setDefaultFloatValue(3.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> this.clutch.getCurrentValue())
           .build()
           .getFloatValue();

   private int oldSlot;
   private BlockPos pos;
   private int lastSneakTicks;
   private int airTicks;
   private int rotateCount;
   private int placeCount;
   private int bigVelocityTick;
   private static int direction = 1;
   private boolean jumpKeyHeld;
   private long lastPlaceGameTick = -1L;
   private float lastMovementYaw;
   private int tellyStopTicks;
   private boolean useLastTellyMovementYaw;
   private int emergencySneakTicks;
   private boolean ignoreJumpDuringSkipTick;
   private int multiPlaceDepth;
   private final CopyOnWriteArrayList<RenderedBlock> renderedBlocks = new CopyOnWriteArrayList<>();

   @Override
   public void onEnable() {
      if (mc.player == null) {
         return;
      }
      setSuffix(this.mode.getCurrentMode());
      this.oldSlot = mc.player.getInventory().selected;
      this.rots.set(mc.player.getYRot(), mc.player.getXRot());
      this.lastRots.set(mc.player.yRotO, mc.player.xRotO);
      this.pos = null;
      this.bigVelocityTick = 0;
      this.rotateCount = 0;
      this.placeCount = 0;
      reachable = true;
      this.jumpKeyHeld = false;
      this.lastSneakTicks = 0;
      this.lastPlaceGameTick = -1L;
      this.lastMovementYaw = mc.player.getYRot();
      this.tellyStopTicks = 0;
      this.useLastTellyMovementYaw = false;
      this.emergencySneakTicks = 0;
      this.ignoreJumpDuringSkipTick = false;
      this.multiPlaceDepth = 0;
      this.renderedBlocks.clear();
   }

   @Override
   public void onDisable() {
      Naven.skipTasks.clear();
      this.ignoreJumpDuringSkipTick = false;
      this.multiPlaceDepth = 0;
      if (mc.player == null) {
         return;
      }
      boolean holdingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
      boolean holdingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getDefaultKey().getValue());
      mc.options.keyJump.setDown(holdingJump);
      mc.options.keyShift.setDown(holdingShift);
      mc.options.keyUse.setDown(false);
      mc.player.getInventory().selected = this.oldSlot;
      this.jumpKeyHeld = false;
      this.lastSneakTicks = 0;
      this.lastPlaceGameTick = -1L;
      this.tellyStopTicks = 0;
      this.useLastTellyMovementYaw = false;
      this.emergencySneakTicks = 0;
      this.renderedBlocks.clear();
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getType() == EventType.RECEIVE
              && event.getPacket() instanceof ClientboundSetEntityMotionPacket velocity
              && mc.player != null
              && velocity.getId() == mc.player.getId()) {
         double strength = new Vec3(velocity.getXa() / 8000.0D, 0.0D, velocity.getZa() / 8000.0D).lengthSqr();
         if (strength >= 1.5D) {
            this.bigVelocityTick = 60;
         }
      }
   }

   @EventTarget
   public void onMouse(EventMouseClick event) {
      if (mc.screen == null && (event.getKey() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getKey() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
         event.setCancelled(true);
      }
   }

   @EventTarget(1)
   public void onTick(EventRunTicks event) {
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         return;
      }

      if (event.getType() != EventType.PRE) {
         if (mc.player.onGround()) {
            this.airTicks = 0;
         } else {
            ++this.airTicks;
         }
         this.renderedBlocks.removeIf(block -> !block.tick());
         return;
      }

      if (mc.screen != null) {
         return;
      }

      setSuffix(this.mode.getCurrentMode());

      AutoMLG autoMLG = AutoMLG.INSTANCE;
      boolean mlgActive = autoMLG != null && autoMLG.isEnabled() && autoMLG.isMLGActive();
      boolean holdingValidBlock = isValidStack(mc.player.getMainHandItem()) || isValidStack(mc.player.getOffhandItem());
      if ((!mlgActive || !holdingValidBlock) && !isValidStack(mc.player.getMainHandItem()) && !isValidStack(mc.player.getOffhandItem())) {
         int slot = findBlockSlot();
         if (slot != -1 && mc.player.getInventory().selected != slot) {
            mc.player.getInventory().selected = slot;
         }
      }

      boolean holdingJump = isJumpHeld();
      boolean moving = PlayerUtils.movementInput();
      boolean tellyStopActive;
      if (!this.mode.isCurrentMode("Telly Bridge")) {
         this.tellyStopTicks = 0;
         this.useLastTellyMovementYaw = false;
         tellyStopActive = false;
      } else if (moving) {
         this.lastMovementYaw = currentMovementYaw();
         this.tellyStopTicks = 5;
         this.useLastTellyMovementYaw = false;
         tellyStopActive = false;
      } else {
         tellyStopActive = holdingJump
                 && this.tellyStopTicks > 0
                 && (isOnBlockEdge(0.3F) || !isBlockUnder() || !mc.player.onGround());
         this.useLastTellyMovementYaw = tellyStopActive;
         if (this.tellyStopTicks > 0) {
            --this.tellyStopTicks;
         }
      }

      this.pos = getBlockPos();
      if (this.pos != null) {
         this.correctRotation = getPlayerYawRotation();
         if (this.mode.isCurrentMode("Normal")) {
            this.rots.set(this.correctRotation.getX(), this.correctRotation.getY());
         } else {
            this.rots.setX(RotationUtils.rotateToYaw(this.rotationSpeed.getCurrentValue(), this.rots.getX(), this.correctRotation.getX()));
            this.rots.setY(RotationUtils.rotateToPitch(this.rotationSpeed.getCurrentValue(), this.rots.getY(), this.correctRotation.getY()));
         }
      }

      this.jumpKeyHeld = holdingJump || tellyStopActive;

      if (this.sneak.getCurrentValue()) {
         ++this.lastSneakTicks;
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
      } else if (this.lastSneakTicks != 0) {
         this.lastSneakTicks = 0;
      }

      if (tellyStopActive) {
         this.emergencySneakTicks = 2;
         mc.options.keyShift.setDown(true);
         Vec3 motion = mc.player.getDeltaMovement();
         double factor = mc.player.onGround() ? 0.12D : 0.35D;
         mc.player.setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
      } else if (this.emergencySneakTicks > 0
              && --this.emergencySneakTicks == 0
              && !(this.sneak.getCurrentValue() && this.lastSneakTicks >= 18 && this.lastSneakTicks < 21)) {
         mc.options.keyShift.setDown(InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getDefaultKey().getValue()));
      }

      if (this.mode.isCurrentMode("Telly Bridge")) {
         mc.options.keyJump.setDown(moving || tellyStopActive || holdingJump);
         if (mc.player.onGround() && (moving || tellyStopActive)) {
            float yaw = tellyStopActive ? this.lastMovementYaw : mc.player.getYRot();
            this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), yaw));
            this.lastRots.set(this.rots.getX(), this.rots.getY());
            return;
         }
      } else if (this.eagle.getCurrentValue()) {
         mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
      }

      this.lastRots.set(this.rots.getX(), this.rots.getY());
      if (this.bigVelocityTick > 0) {
         --this.bigVelocityTick;
      }
   }

   @EventTarget
   public void onClick(EventClick event) {
      event.setCancelled(true);
      if (mc.screen != null || mc.player == null || mc.level == null || mc.gameMode == null || this.pos == null) {
         return;
      }
      if (this.mode.isCurrentMode("Telly Bridge") && this.airTicks < this.tellyTick.getCurrentValue() && !this.jumpKeyHeld) {
         return;
      }

      reachable = true;
      if (mc.player.getDeltaMovement().y < -0.1D) {
         double y = mc.player.getY();
         double motionY = mc.player.getDeltaMovement().y;
         for (int i = 0; i < 2; ++i) {
            motionY = (motionY - 0.08D) * 0.98D;
            y += motionY;
            if (motionY < 0.0D) {
               BlockPos below = BlockPos.containing(mc.player.getX(), y - 0.5D, mc.player.getZ());
               if (!mc.level.isEmptyBlock(below)) {
                  y = Math.floor(y) + 0.5D;
                  break;
               }
            }
         }
         if (this.pos.getY() > y) {
            reachable = false;
         }
      }

      Vec3 hitCenter = new Vec3(this.pos.getX() + 0.5D, this.pos.getY(), this.pos.getZ() + 0.5D);
      if (hitCenter.subtract(mc.player.getEyePosition()).lengthSqr() > 20.25D) {
         return;
      }
      boolean clutchDanger = this.clutch.getCurrentValue() && (!reachable || this.bigVelocityTick > 0) && this.rotateCount < 8;
      boolean multiPlaceActive = clutchDanger && this.multiPlace.getCurrentValue();
      if (this.placeDelayTicks.getCurrentValue() > 0.0F
              && !multiPlaceActive
              && this.lastPlaceGameTick >= 0L
              && mc.level.getGameTime() - this.lastPlaceGameTick < (long) this.placeDelayTicks.getCurrentValue()) {
         return;
      }

      Vector2f placeRotation = new Vector2f(RotationManager.rotations != null ? RotationManager.rotations.x : this.rots.x,
              RotationManager.rotations != null ? RotationManager.rotations.y : this.rots.y);

      boolean skippedTick = false;
      if (clutchDanger) {
         if (this.placeCount >= 7 || isBlockUnder()) {
            reachable = true;
            this.rotateCount = 0;
            return;
         }
         int skippedTicks = (int) this.clutchTicks.getCurrentValue();
         if (!this.multiPlace.getCurrentValue() || this.multiPlaceDepth == 0) {
            Naven.skipTasks.clear();
         }
         for (int i = 0; i < skippedTicks; ++i) {
            Naven.skipTasks.offer(() -> {
            });
         }
         direction *= -1;
         placeRotation.set(this.rots.x, this.rots.y);
         placeRotation.setX(placeRotation.getX() + 0.0001F * direction);
         ++this.placeCount;
         ++this.rotateCount;
         skippedTick = true;
         this.ignoreJumpDuringSkipTick = true;

      } else {
         this.rotateCount = 0;
         this.placeCount = 0;
      }

      InteractionHand hand = getPlaceHand();
      if (hand != null) {
         HitResult hit = RayTraceUtils.rayCast(1.0F, placeRotation);
         if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(this.pos)) {
            boolean holdingJump = isJumpHeld();
            boolean invalidUpHit = blockHit.getDirection() == Direction.UP
                    && !mc.player.onGround()
                    && PlayerUtils.movementInput()
                    && !holdingJump
                    && !this.mode.isCurrentMode("Normal")
                    && !skippedTick;
            if (!invalidUpHit && mc.gameMode.useItemOn(mc.player, hand, blockHit) == InteractionResult.SUCCESS) {
               this.lastPlaceGameTick = mc.level.getGameTime();
               if (this.swing.getCurrentValue()) {
                  mc.player.swing(hand);
               } else {
                  NetworkUtils.sendPacket(new ServerboundSwingPacket(hand));
               }

               BlockPos placedPos = blockHit.getBlockPos().relative(blockHit.getDirection());
               this.renderedBlocks.add(new RenderedBlock(placedPos));
               while (this.renderedBlocks.size() > 2) {
                  this.renderedBlocks.remove(0);
               }
            }
         }
      }

      if (skippedTick && this.multiPlace.getCurrentValue() && this.placeCount < 7) {
         ++this.multiPlaceDepth;
         try {
            this.onTick(new EventRunTicks(EventType.PRE));
            this.onClick(event);
         } finally {
            --this.multiPlaceDepth;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender event) {
      if (!this.renderPlacement.getCurrentValue() || this.renderedBlocks.isEmpty() || mc.gameRenderer == null) {
         return;
      }

      PoseStack poseStack = event.getPMatrixStack();
      Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
      poseStack.pushPose();
      poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      for (RenderedBlock block : this.renderedBlocks) {
         AABB box = new AABB(block.position).inflate(0.002D);
         float alpha = block.getAlpha();
         Color color = new Color(255, 0, 0);

         RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha * 0.45F);
         RenderUtils.drawSolidBox(box, poseStack);
         RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
         RenderUtils.drawOutlineBox(box, poseStack);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      poseStack.popPose();
   }

   private Vector2f getPlayerYawRotation() {
      if (isTower()) {
         return new Vector2f(mc.player.getYRot() - 180.0F, 90.0F);
      }

      float yaw = (this.mode.isCurrentMode("Telly Bridge") && this.useLastTellyMovementYaw ? this.lastMovementYaw : currentMovementYaw()) - 180.0F;
      yaw += (float) (Math.random() * 0.5D - 0.25D);
      Vector2f rotations = new Vector2f(yaw, 82.0F);
      if (!shouldBuild() || isHitValid(RayTraceUtils.rayCast(1.0F, rotations))) {
         return rotations;
      }

      ArrayList<Float> validPitches = new ArrayList<>();
      for (float pitch = Math.max(this.rots.y - 30.0F, -90.0F); pitch < Math.min(this.rots.y + 20.0F, 90.0F); pitch += 0.3F) {
         Vector2f fixed = RotationUtils.getFixedRotation(yaw, pitch, this.rots.x, this.rots.y);
         if (isHitValid(RayTraceUtils.rayCast(1.0F, new Vector2f(yaw, fixed.y)))) {
            validPitches.add(fixed.y);
         }
      }
      if (!validPitches.isEmpty()) {
         validPitches.sort(Comparator.comparingDouble(pitch -> Math.abs(pitch - this.rots.y)));
         rotations.setY(validPitches.get(0));
         return rotations;
      }

      for (float yawLoop = 0.0F; yawLoop < 180.0F; yawLoop += 1.0F) {
         float currentPitch = this.rots.y;
         for (float pitchLoop = 0.0F; pitchLoop < 25.0F; pitchLoop += 1.0F) {
            for (int i = 0; i < 2; ++i) {
               float pitch = currentPitch - pitchLoop * (i == 0 ? 1.0F : -1.0F);
               float[][] offsets = new float[][]{{yaw + yawLoop, pitch}, {yaw - yawLoop, pitch}};
               for (float[] offset : offsets) {
                  float clampedPitch = MathHelper.clamp(offset[1], -90.0F, 90.0F);
                  Vector2f fixed = RotationUtils.getFixedRotation(offset[0], clampedPitch, this.rots.x, this.rots.y);
                  if (isHitValid(RayTraceUtils.rayCast(1.0F, fixed))) {
                     return fixed;
                  }
               }
            }
         }
      }
      return rotations;
   }

   private BlockPos getBlockPos() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.0D, mc.player.getZ());
      ArrayList<Vec3> positions = new ArrayList<>();
      HashMap<Vec3, BlockPos> lookup = new HashMap<>();

      for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; ++x) {
         for (int y = playerPos.getY() - 1; y <= playerPos.getY(); ++y) {
            for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; ++z) {
               BlockPos check = new BlockPos(x, y, z);
               if (isValidBlock(check)) {
                  BlockState block = mc.level.getBlockState(check);
                  VoxelShape shape = block.getShape(mc.level, check);
                  double ex = MathHelper.clamp(mc.player.getX(), check.getX(), check.getX() + shape.max(Axis.X));
                  double ey = MathHelper.clamp(mc.player.getY(), check.getY(), check.getY() + shape.max(Axis.Y));
                  double ez = MathHelper.clamp(mc.player.getZ(), check.getZ(), check.getZ() + shape.max(Axis.Z));
                  Vec3 vec = new Vec3(ex, ey, ez);
                  positions.add(vec);
                  lookup.put(vec, check);
               }
            }
         }
      }

      if (positions.isEmpty()) {
         return null;
      }
      positions.sort(Comparator.comparingDouble(vec -> mc.player.distanceToSqr(vec.x, vec.y, vec.z)));
      BlockPos best = lookup.get(positions.get(0));
      return isTower() && best.getY() != mc.player.getY() - 1.5D
              ? BlockPos.containing(mc.player.getX(), mc.player.getY() - 1.5D, mc.player.getZ())
              : best;
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

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem blockItem) || stack.getCount() <= 1) {
         return false;
      }
      if (!InventoryUtils.isItemValid(stack)) {
         return false;
      }

      String name = stack.getDisplayName().getString();
      if (name.contains("Click") || name.contains("鐐瑰嚮") || stack.getItem() instanceof ItemNameBlockItem) {
         return false;
      }

      Block block = blockItem.getBlock();
      return !(block instanceof FlowerBlock)
              && !(block instanceof BushBlock)
              && !(block instanceof FungusBlock)
              && !(block instanceof CropBlock)
              && !(block instanceof SlabBlock)
              && !blacklistedBlocks.contains(block);
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return mc.player != null
              && mc.level != null
              && !mc.level.getCollisions(mc.player, mc.player.getBoundingBox().move(0.0D, -0.5D, 0.0D).inflate(-sensitivity, 0.0D, -sensitivity))
              .iterator()
              .hasNext();
   }

   public boolean isBlockUnder() {
      if (mc.player == null || mc.level == null) {
         return false;
      }
      return mc.level.getBlockState(mc.player.blockPosition().below()).isSolidRender(mc.level, mc.player.blockPosition().below());
   }

   public int getBlockCount() {
      if (mc.player == null) {
         return 0;
      }
      int totalBlocks = 0;
      for (int i = 0; i < 36; ++i) {
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

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5D, mc.player.getZ());
      return mc.level.isEmptyBlock(playerPos) && getPlaceHand() != null;
   }

   private boolean isHitValid(HitResult hit) {
      if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || this.pos == null) {
         return false;
      }
      return isValidBlock(blockHit.getBlockPos())
              && isNearbyBlockPos(blockHit.getBlockPos())
              && blockHit.getDirection() != Direction.DOWN
              && blockHit.getDirection() != Direction.UP;
   }

   private boolean isNearbyBlockPos(BlockPos blockPos) {
      if (this.pos == null) {
         return false;
      }
      if (!mc.player.onGround()) {
         return blockPos.equals(this.pos);
      }
      for (int x = this.pos.getX() - 1; x <= this.pos.getX() + 1; ++x) {
         for (int z = this.pos.getZ() - 1; z <= this.pos.getZ() + 1; ++z) {
            if (blockPos.equals(new BlockPos(x, this.pos.getY(), z))) {
               return true;
            }
         }
      }
      return false;
   }

   private boolean isTower() {
      boolean holdingJump = isJumpHeld();
      return holdingJump
              && !this.useLastTellyMovementYaw
              && !mc.options.keyUp.isDown()
              && !mc.options.keyDown.isDown()
              && !mc.options.keyLeft.isDown()
              && !mc.options.keyRight.isDown();
   }

   private boolean isJumpHeld() {
      if (this.ignoreJumpDuringSkipTick) {
         if (!Naven.skipTasks.isEmpty()) {
            return false;
         }
         this.ignoreJumpDuringSkipTick = false;
      }
      return InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue());
   }

   private float currentMovementYaw() {
      float realYaw = mc.player.getYRot();
      if (mc.options.keyDown.isDown()) {
         realYaw += 180.0F;
         if (mc.options.keyLeft.isDown()) {
            realYaw += 45.0F;
         } else if (mc.options.keyRight.isDown()) {
            realYaw -= 45.0F;
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
      return realYaw;
   }

   private InteractionHand getPlaceHand() {
      if (isValidStack(mc.player.getMainHandItem())) {
         return InteractionHand.MAIN_HAND;
      }
      if (isValidStack(mc.player.getOffhandItem())) {
         return InteractionHand.OFF_HAND;
      }
      return null;
   }

   private int findBlockSlot() {
      for (int i = 0; i < 9; ++i) {
         if (isValidStack(mc.player.getInventory().getItem(i))) {
            return i;
         }
      }
      return -1;
   }

   private static class RenderedBlock {
      private final BlockPos position;
      private int lifetime = 10;

      private RenderedBlock(BlockPos position) {
         this.position = position;
      }

      private boolean tick() {
         return --this.lifetime > 0;
      }

      private float getAlpha() {
         return this.lifetime / 10.0F * 0.8F;
      }
   }
}
