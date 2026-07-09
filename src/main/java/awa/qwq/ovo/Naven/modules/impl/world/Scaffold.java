package awa.qwq.ovo.Naven.modules.impl.world;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventClick;
import awa.qwq.ovo.Naven.events.impl.EventMouseClick;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.events.impl.EventRunTicks;
import awa.qwq.ovo.Naven.events.impl.EventUpdateFoV;
import awa.qwq.ovo.Naven.managers.rotation.RotationManager;
import awa.qwq.ovo.Naven.managers.rotation.utils.RotationUtils;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.modules.impl.player.AutoMLG;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.AirBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.FungusBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShortPlantBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

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

   public BooleanValue visualSwing = ValueBuilder.create(this, "Visual Swing")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue jumpSprint = ValueBuilder.create(this, "Jump Sprint")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
           .build()
           .getBooleanValue();

   public BooleanValue esp = ValueBuilder.create(this, "ESP")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue useItemBeforePlace = ValueBuilder.create(this, "Use Item before place(1.17+)")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue keepFov = ValueBuilder.create(this, "Keep Fov (>No Fov)")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue fov = ValueBuilder.create(this, "Fov")
           .setDefaultFloatValue(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(2.0F)
           .setFloatStep(0.05F)
           .setVisibility(() -> this.keepFov.getCurrentValue())
           .build()
           .getFloatValue();

   public BooleanValue vulcan = ValueBuilder.create(this, "Vulcan")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public BooleanValue recursionPlace = ValueBuilder.create(this, "Recursion Place")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public ModeValue rotationMode = ValueBuilder.create(this, "Rotation Mode")
           .setDefaultModeIndex(1)
           .setModes("Keybind Yaw", "Strict")
           .build()
           .getModeValue();

   public BooleanValue legitUP = ValueBuilder.create(this, "Legit UP")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue clutchSafeDistance = ValueBuilder.create(this, "Clutch Safe Distance")
           .setDefaultFloatValue(4.25F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(6.0F)
           .setFloatStep(0.05F)
           .build()
           .getFloatValue();

   private int oldSlot;
   private BlockPos pos;
   private int lastSneakTicks;
   private int airTicks;
   private int rotateCount;
   private int placeCount;
   private boolean jumpKeyHeld;
   private int lastClutchLayer = Integer.MIN_VALUE;
   private boolean preferUpFace;
   private Vec3d lastClutchDebugPosition;
   private boolean clutchDebugAnchorActive;
   private Vec3d clutchDebugAnchorPosition;
   private boolean clutchSafeDistanceActive;
   private long lastPlaceGameTick = -1L;
   private float lastMovementYaw;
   private int tellyStopTicks;
   private boolean useLastTellyMovementYaw;
   private int emergencySneakTicks;
   private boolean ignoreJumpDuringSkipTick;
   private int skipTickAttempts;
   private double lastStrictYawDiff = Double.NaN;
   private double lastStrictPitchDiff = Double.NaN;
   private int strictJitterCounter;
   private Direction strictPlacementFace;
   private int multiPlaceDepth;
   private final CopyOnWriteArrayList<RenderedBlock> renderedBlocks = new CopyOnWriteArrayList<>();

   @Override
   public void onEnable() {
      if (mc.player == null) {
         return;
      }
      this.oldSlot = mc.player.getInventory().selectedSlot;
      this.rots.set(mc.player.getYaw(), mc.player.getPitch());
      this.lastRots.set(mc.player.prevYaw, mc.player.prevPitch);
      this.pos = null;
      this.rotateCount = 0;
      this.placeCount = 0;
      this.reachable = true;
      this.jumpKeyHeld = false;
      this.lastClutchLayer = mc.player.getBlockPos().getY();
      this.preferUpFace = false;
      this.lastClutchDebugPosition = mc.player.getPos();
      this.clutchDebugAnchorActive = false;
      this.clutchDebugAnchorPosition = null;
      this.clutchSafeDistanceActive = false;
      this.lastSneakTicks = 0;
      this.lastPlaceGameTick = -1L;
      this.lastMovementYaw = mc.player.getYaw();
      this.tellyStopTicks = 0;
      this.useLastTellyMovementYaw = false;
      this.emergencySneakTicks = 0;
      this.ignoreJumpDuringSkipTick = false;
      this.skipTickAttempts = 0;
      this.lastStrictYawDiff = Double.NaN;
      this.lastStrictPitchDiff = Double.NaN;
      this.strictJitterCounter = 0;
      this.strictPlacementFace = null;
      this.multiPlaceDepth = 0;
      this.renderedBlocks.clear();
   }

   @Override
   public void onDisable() {
      Naven.skipTasks.clear();
      this.ignoreJumpDuringSkipTick = false;
      this.skipTickAttempts = 0;
      this.lastStrictYawDiff = Double.NaN;
      this.lastStrictPitchDiff = Double.NaN;
      this.strictJitterCounter = 0;
      this.strictPlacementFace = null;
      this.multiPlaceDepth = 0;
      this.lastClutchLayer = Integer.MIN_VALUE;
      this.preferUpFace = false;
      this.lastClutchDebugPosition = null;
      this.clutchDebugAnchorActive = false;
      this.clutchDebugAnchorPosition = null;
      this.clutchSafeDistanceActive = false;
      if (mc.player == null) {
         return;
      }
      boolean holdingJump = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());
      boolean holdingShift = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode());
      mc.options.jumpKey.setPressed(holdingJump);
      mc.options.sneakKey.setPressed(holdingShift);
      mc.options.useKey.setPressed(false);
      mc.player.getInventory().selectedSlot = this.oldSlot;
      this.jumpKeyHeld = false;
      this.lastSneakTicks = 0;
      this.lastPlaceGameTick = -1L;
      this.tellyStopTicks = 0;
      this.useLastTellyMovementYaw = false;
      this.emergencySneakTicks = 0;
      this.renderedBlocks.clear();
   }

   @EventTarget
   public void onUpdateFoV(EventUpdateFoV event) {
      if (this.keepFov.getCurrentValue()) {
         event.setFov(this.fov.getCurrentValue() + PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
      }
   }

   @EventTarget
   public void onMouse(EventMouseClick event) {
      if (mc.currentScreen == null && (event.getKey() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getKey() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
         event.setCancelled(true);
      }
   }

   @EventTarget(1)
   public void onPreRunTick(EventRunTicks event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      if (event.getType() != EventType.PRE) {
         this.updateClutchDebug();
         if (mc.player.isOnGround()) {
            this.airTicks = 0;
         } else {
            ++this.airTicks;
         }
         this.renderedBlocks.removeIf(block -> !block.tick());
         return;
      }

      if (mc.currentScreen != null) {
         return;
      }

      AutoMLG autoMLG = AutoMLG.INSTANCE;
      boolean mlgActive = autoMLG != null && autoMLG.isEnabled() && autoMLG.isMLGActive();
      boolean holdingValidBlock = isValidStack(mc.player.getMainHandStack()) || isValidStack(mc.player.getOffHandStack());
      if ((!mlgActive || !holdingValidBlock) && !isValidStack(mc.player.getMainHandStack()) && !isValidStack(mc.player.getOffHandStack())) {
         int slot = findBlockSlot();
         if (slot != -1 && mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
         }
      }

      boolean holdingJump = isJumpHeld();
      boolean moving = PlayerUtils.movementInput();
      boolean tellyStopActive;
      boolean jumpSprintActive = this.mode.isCurrentMode("Telly Bridge") && this.jumpSprint.getCurrentValue();
      if (!this.mode.isCurrentMode("Telly Bridge") || !jumpSprintActive) {
         this.tellyStopTicks = 0;
         this.useLastTellyMovementYaw = false;
         tellyStopActive = false;
      } else if (moving) {
         this.lastMovementYaw = currentMovementYaw();
         this.tellyStopTicks = 5;
         this.useLastTellyMovementYaw = false;
         tellyStopActive = false;
      } else {
         tellyStopActive = holdingJump && this.tellyStopTicks > 0 && (isOnBlockEdge(0.3F) || !isBlockUnder() || !mc.player.isOnGround());
         this.useLastTellyMovementYaw = tellyStopActive;
         if (this.tellyStopTicks > 0) {
            --this.tellyStopTicks;
         }
      }

      this.pos = getBlockPos();
      if (this.pos != null) {
         this.correctRotation = getPlayerYawRotation();
         if (this.rotationMode.isCurrentMode("Strict")) {
            float yawSpeed = this.mode.isCurrentMode("Telly Bridge") ? 65.0F : 95.0F;
            float pitchSpeed = this.mode.isCurrentMode("Telly Bridge") ? 55.0F : 80.0F;
            this.rots.setX(RotationUtils.rotateToYaw(yawSpeed, this.rots.getX(), this.correctRotation.getX()));
            this.rots.setY(RotationUtils.rotateToPitch(pitchSpeed, this.rots.getY(), this.correctRotation.getY()));
         } else {
            float yawSpeed = this.legitUP.getCurrentValue() ? 55.0F : 75.0F;
            float pitchSpeed = this.legitUP.getCurrentValue() ? 45.0F : 75.0F;
            this.rots.setX(RotationUtils.rotateToYaw(yawSpeed, this.rots.getX(), this.correctRotation.getX()));
            this.rots.setY(RotationUtils.rotateToPitch(pitchSpeed, this.rots.getY(), this.correctRotation.getY()));
         }
      }

      this.jumpKeyHeld = holdingJump || tellyStopActive;

      if (this.vulcan.getCurrentValue()) {
         ++this.lastSneakTicks;
         if (this.lastSneakTicks == 18) {
            if (mc.player.isSprinting()) {
               mc.options.sprintKey.setPressed(false);
               mc.player.setSprinting(false);
            }
            mc.options.sneakKey.setPressed(true);
         } else if (this.lastSneakTicks >= 21) {
            mc.options.sneakKey.setPressed(false);
            this.lastSneakTicks = 0;
         }
      } else if (this.lastSneakTicks != 0) {
         this.lastSneakTicks = 0;
      }

      if (this.emergencySneakTicks > 0 && --this.emergencySneakTicks == 0 && !(this.vulcan.getCurrentValue() && this.lastSneakTicks >= 18 && this.lastSneakTicks < 21)) {
         mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sneakKey.getDefaultKey().getCode()));
      }

      if (this.mode.isCurrentMode("Telly Bridge")) {
         mc.options.jumpKey.setPressed(jumpSprintActive && moving || tellyStopActive || holdingJump);
         if (mc.player.isOnGround() && (jumpSprintActive && moving || tellyStopActive)) {
            float yaw = tellyStopActive ? this.lastMovementYaw : mc.player.getYaw();
            this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), yaw));
            this.lastRots.set(this.rots.getX(), this.rots.getY());
            return;
         }
      }

      this.lastRots.set(this.rots.getX(), this.rots.getY());
   }

   @EventTarget
   public void onClick(EventClick event) {
      event.setCancelled(true);
      if (mc.currentScreen != null || mc.player == null || mc.world == null || mc.interactionManager == null || this.pos == null) {
         return;
      }
      if (this.mode.isCurrentMode("Telly Bridge") && this.airTicks < 3.0F && !this.jumpKeyHeld) {
         return;
      }

      reachable = true;
      if (mc.player.getVelocity().y < -0.1D) {
         double y = mc.player.getY();
         double motionY = mc.player.getVelocity().y;
         for (int i = 0; i < 2; ++i) {
            motionY = (motionY - 0.08D) * 0.98D;
            y += motionY;
            if (motionY < 0.0D) {
               BlockPos below = BlockPos.ofFloored(mc.player.getX(), y - 0.5D, mc.player.getZ());
               if (!mc.world.isAir(below)) {
                  y = Math.floor(y) + 0.5D;
                  break;
               }
            }
         }
         if (this.pos.getY() > y) {
            reachable = false;
         }
      }

      boolean clutchDanger = !reachable && this.rotateCount <= 8;
      Vec3d hitCenter = new Vec3d(this.pos.getX() + 0.5D, this.pos.getY(), this.pos.getZ() + 0.5D);
      if (hitCenter.subtract(mc.player.getEyePos()).lengthSquared() > (clutchDanger ? 25.0D : 20.25D)) {
         return;
      }
      if ((this.preferUpFace || this.clutchSafeDistanceActive) && this.lastPlaceGameTick == mc.world.getTime()) {
         return;
      }

      Vector2f placeRotation = new Vector2f(RotationManager.rotations != null ? RotationManager.rotations.x : this.rots.x, RotationManager.rotations != null ? RotationManager.rotations.y : this.rots.y);

      boolean skippedTick = false;
      if (clutchDanger) {
         if (isBlockUnder()) {
            reachable = true;
            this.rotateCount = 0;
            this.placeCount = 0;
            return;
         }
         if (this.skipTickAttempts >= 8) {
            Naven.skipTasks.clear();
            this.ignoreJumpDuringSkipTick = false;
            this.skipTickAttempts = 0;
            this.rotateCount = 0;
            this.placeCount = 0;
            return;
         }
         if (!this.recursionPlace.getCurrentValue() || this.multiPlaceDepth == 0) {
            Naven.skipTasks.clear();
         }
         for (int i = 0; i < 2; ++i) {
            Naven.skipTasks.offer(() -> {
            });
         }
         placeRotation.set(this.rots.x, this.rots.y);
         ++this.skipTickAttempts;
         ++this.placeCount;
         ++this.rotateCount;
         skippedTick = true;
         this.ignoreJumpDuringSkipTick = true;
      } else {
         this.rotateCount = 0;
         this.placeCount = 0;
         this.skipTickAttempts = 0;
      }

      Hand hand = getPlaceHand();
      boolean placed = false;
      if (hand != null) {
         HitResult hit = skippedTick ? RayTraceUtils.rayCast(4.5D, 1.0F, true, placeRotation) : RayTraceUtils.rayCast(1.0F, placeRotation);
         if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && this.isPlaceHitValid(blockHit, skippedTick)) {
            boolean holdingJump = isJumpHeld();
            boolean invalidUpHit = blockHit.getSide() == Direction.UP && !this.preferUpFace && !this.canUseLegitUp() && !mc.player.isOnGround() && PlayerUtils.movementInput() && !holdingJump && !this.mode.isCurrentMode("Normal") && !skippedTick;
            if (!invalidUpHit) {
               if (skippedTick) {
                  this.rots.set(placeRotation.x, placeRotation.y);
                  RotationManager.setRotations(new Vector2f(placeRotation.x, placeRotation.y));
                  NetworkUtils.sendPacketNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(
                          placeRotation.x,
                          placeRotation.y,
                          mc.player.isOnGround()
                  ));
               }

               if (this.useItemBeforePlace.getCurrentValue() && !this.rotationMode.isCurrentMode("Strict")) {
                  mc.interactionManager.interactItem(mc.player, hand);
               }

               if (mc.interactionManager.interactBlock(mc.player, hand, blockHit) == ActionResult.SUCCESS) {
                  placed = true;
                  this.lastPlaceGameTick = mc.world.getTime();
                  if (this.visualSwing.getCurrentValue()) {
                     mc.player.swingHand(hand);
                  } else {
                     NetworkUtils.sendPacket(new HandSwingC2SPacket(hand));
                  }

                  BlockPos placedPos = blockHit.getBlockPos().offset(blockHit.getSide());
                  this.renderedBlocks.add(new RenderedBlock(placedPos, mc.world.getTime()));
                  while (this.renderedBlocks.size() > 2) {
                     this.renderedBlocks.remove(0);
                  }
               }
            }
         }
      }
      if (skippedTick && placed && this.recursionPlace.getCurrentValue() && this.placeCount < 8 && this.rotateCount <= 8) {
         ++this.multiPlaceDepth;
         try {
            this.onPreRunTick(new EventRunTicks(EventType.PRE));
            this.onClick(event);
         } finally {
            --this.multiPlaceDepth;
         }
      }
      if (skippedTick) {
         if (placed) {
            this.skipTickAttempts = 0;
         } else if (this.skipTickAttempts >= 8) {
            Naven.skipTasks.clear();
            this.ignoreJumpDuringSkipTick = false;
            this.skipTickAttempts = 0;
            this.rotateCount = 0;
            this.placeCount = 0;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender event) {
      if (!this.esp.getCurrentValue() || this.renderedBlocks.isEmpty() || mc.gameRenderer == null) {
         return;
      }

      MatrixStack poseStack = event.getPMatrixStack();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      poseStack.push();
      poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      for (RenderedBlock block : this.renderedBlocks) {
         Box box = new Box(block.position).expand(0.002D);
         float alpha = block.getAlpha();
         Color color = new Color(67, 87, 227);
         RenderSystem.setShaderColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, alpha);
         RenderUtils.drawSolidBox(box, poseStack);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      poseStack.pop();
   }

   private Vector2f getPlayerYawRotation() {
      if (this.preferUpFace) {
         Vector2f upRotation = this.getUpFaceRotation();
         if (upRotation != null) {
            return upRotation;
         }
         this.preferUpFace = false;
      }

      if (isTower()) {
         return new Vector2f(mc.player.getYaw(), 90.0F);
      }

      if (rotationMode.isCurrentMode("Strict")) {
         return getStrictCenterRotation();
      }

      float yaw;

      yaw = (this.mode.isCurrentMode("Telly Bridge") && this.useLastTellyMovementYaw ? this.lastMovementYaw : currentMovementYaw()) - 180.0F;

      yaw += (float) (Math.random() * 0.5D - 0.25D);
      Vector2f rotations = new Vector2f(yaw, 82.0F);

      boolean build = shouldBuild();
      if (!build) {
         return rotations;
      }
      if (isHitValid(RayTraceUtils.rayCast(1.0F, rotations))) {
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

      for (float yawLoop = 0.0F; yawLoop < 360.0F; yawLoop += 1.0F) {
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

   private Vector2f getStrictCenterRotation() {
      Direction bestFace = null;
      double bestFaceScore = Double.MAX_VALUE;
      BlockPos underPlayer = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0D, mc.player.getZ());
      boolean allowLegitUp = this.canUseLegitUp();
      boolean avoidUpFace = !allowLegitUp && !this.preferUpFace && this.mode.isCurrentMode("Telly Bridge") && !mc.player.isOnGround() && PlayerUtils.movementInput() && !isJumpHeld();
      Vec3d eye = mc.player.getEyePos();

      Direction[] faceOrder = new Direction[]{Direction.UP, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH};
      for (Direction candidateFace : faceOrder) {
         if (avoidUpFace && candidateFace == Direction.UP) {
            continue;
         }
         BlockPos placePos = this.pos.offset(candidateFace);
         if (!mc.world.isAir(placePos)) {
            continue;
         }

         Vec3d faceCenter = new Vec3d(
                 this.pos.getX() + 0.5D + candidateFace.getOffsetX() * 0.5D,
                 this.pos.getY() + 0.5D + candidateFace.getOffsetY() * 0.5D,
                 this.pos.getZ() + 0.5D + candidateFace.getOffsetZ() * 0.5D
         );
         Vec3d toHit = faceCenter.subtract(eye);
         boolean faceVisible;
         if (toHit.lengthSquared() <= 1.0E-6D) {
            faceVisible = true;
         } else {
            Vec3d faceNormal = Vec3d.of(candidateFace.getVector()).normalize();
            faceVisible = toHit.normalize().dotProduct(faceNormal.multiply(-1.0D)) >= 0.0D;
         }
         if (toHit.lengthSquared() > 20.25D || !faceVisible) {
            continue;
         }

         double score = placePos.getSquaredDistance(underPlayer) + faceCenter.squaredDistanceTo(eye) * 0.03D;
         if (this.preferUpFace && candidateFace == Direction.UP) {
            score -= 5.0D;
         } else if (candidateFace == Direction.UP && !isJumpHeld() && !allowLegitUp) {
            score += 1.0D;
         }
         if (avoidUpFace && candidateFace == Direction.UP) {
            score += 2.0D;
         }
         if (this.strictPlacementFace != null && candidateFace != this.strictPlacementFace) {
            score += 0.2D;
         }
         if (score < bestFaceScore) {
            bestFaceScore = score;
            bestFace = candidateFace;
         }
      }

      Direction face = bestFace != null ? bestFace : Direction.UP;
      this.strictPlacementFace = face;
      double x = this.pos.getX() + 0.5D + face.getOffsetX() * 0.5D;
      double y = this.pos.getY() + 0.5D + face.getOffsetY() * 0.5D;
      double z = this.pos.getZ() + 0.5D + face.getOffsetZ() * 0.5D;
      double jitter = 0.05D;
      if (face.getAxis() != Axis.X) {
         x += MathUtils.getRandomDoubleInRange(-jitter, jitter);
      }
      if (face.getAxis() != Axis.Y) {
         y += MathUtils.getRandomDoubleInRange(-jitter, jitter);
      }
      if (face.getAxis() != Axis.Z) {
         z += MathUtils.getRandomDoubleInRange(-jitter, jitter);
      }

      Vec3d target = new Vec3d(x, y, z);
      Vector2f targetRotation = RotationUtils.getRotations(target);
      float yawDelta = MathHelper.wrapDegrees(targetRotation.x - this.rots.x);
      float maxStep = this.mode.isCurrentMode("Telly Bridge") ? (this.airTicks <= 1 ? 90.0F : 75.0F) : 180.0F;
      Vector2f rotation = new Vector2f(
              this.rots.x + MathHelper.clamp(yawDelta, -maxStep, maxStep),
              MathHelper.clamp(targetRotation.y, -89.5F, 89.5F)
      );

      Vector2f fixedRotation = RotationUtils.getFixedRotation(rotation.x, rotation.y, this.rots.x, this.rots.y);
      HitResult fixedHit = RayTraceUtils.rayCast(1.0F, fixedRotation);
      boolean fixedValid = fixedHit instanceof BlockHitResult fixedBlockHit
              && fixedHit.getType() == HitResult.Type.BLOCK
              && this.pos != null
              && fixedBlockHit.getBlockPos().equals(this.pos)
              && fixedBlockHit.getSide() != Direction.DOWN
              && fixedBlockHit.getSide() == face;
      if (fixedValid) {
      } else {
          double bestRotationScore = Double.MAX_VALUE;
         for (float yawOffset = -12.0F; yawOffset <= 12.0F; yawOffset += 1.5F) {
            for (float pitchOffset = -12.0F; pitchOffset <= 12.0F; pitchOffset += 1.5F) {
               float candidateYawDelta = MathHelper.wrapDegrees(rotation.x + yawOffset - this.rots.x);
               Vector2f candidate = new Vector2f(
                       this.rots.x + MathHelper.clamp(candidateYawDelta, -maxStep, maxStep),
                       MathHelper.clamp(rotation.y + pitchOffset, -89.5F, 89.5F)
               );
               Vector2f fixedCandidate = RotationUtils.getFixedRotation(candidate.x, candidate.y, this.rots.x, this.rots.y);
               HitResult candidateHit = RayTraceUtils.rayCast(1.0F, fixedCandidate);
               if (!(candidateHit instanceof BlockHitResult candidateBlockHit)
                       || candidateHit.getType() != HitResult.Type.BLOCK
                       || this.pos == null
                       || !candidateBlockHit.getBlockPos().equals(this.pos)
                       || candidateBlockHit.getSide() == Direction.DOWN
                       || candidateBlockHit.getSide() != face) {
                  continue;
               }

               double score = Math.abs(yawOffset) + Math.abs(pitchOffset);
               if (score < bestRotationScore) {
                  bestRotationScore = score;
               }
            }
         }
      }
       return targetRotation;
   }

   private Vector2f getUpFaceRotation() {
      if (this.pos == null || mc.player == null || mc.world == null) {
         return null;
      }

      if (!isValidBlock(this.pos) || !mc.world.isAir(this.pos.offset(Direction.UP))) {
         return null;
      }

      Vec3d target = new Vec3d(this.pos.getX() + 0.5D, this.pos.getY() + 1.0D, this.pos.getZ() + 0.5D);
      Vector2f targetRotation = RotationUtils.getRotations(target);
      Vector2f fixedRotation = RotationUtils.getFixedRotation(targetRotation.x, targetRotation.y, this.rots.x, this.rots.y);
      HitResult fixedHit = RayTraceUtils.rayCast(1.0F, fixedRotation);
      if (fixedHit instanceof BlockHitResult fixedBlockHit
              && fixedHit.getType() == HitResult.Type.BLOCK
              && fixedBlockHit.getBlockPos().equals(this.pos)
              && fixedBlockHit.getSide() == Direction.UP) {
         this.strictPlacementFace = Direction.UP;
         return fixedRotation;
      }

      return null;
   }

   private BlockPos getBlockPos() {
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0D, mc.player.getZ());
      ArrayList<Vec3d> positions = new ArrayList<>();
      HashMap<Vec3d, BlockPos> lookup = new HashMap<>();

      for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; ++x) {
         for (int y = playerPos.getY() - 1; y <= playerPos.getY(); ++y) {
            for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; ++z) {
               BlockPos check = new BlockPos(x, y, z);
               if (isValidBlock(check)) {
                  BlockState block = mc.world.getBlockState(check);
                  VoxelShape shape = block.getOutlineShape(mc.world, check);
                  double ex = MathHelper.clamp(mc.player.getX(), check.getX(), check.getX() + shape.getMax(Axis.X));
                  double ey = MathHelper.clamp(mc.player.getY(), check.getY(), check.getY() + shape.getMax(Axis.Y));
                  double ez = MathHelper.clamp(mc.player.getZ(), check.getZ(), check.getZ() + shape.getMax(Axis.Z));
                  Vec3d vec = new Vec3d(ex, ey, ez);
                  positions.add(vec);
                  lookup.put(vec, check);
               }
            }
         }
      }

      if (positions.isEmpty()) {
         this.preferUpFace = false;
         return null;
      }
      positions.sort(Comparator.comparingDouble(vec -> mc.player.squaredDistanceTo(vec.x, vec.y, vec.z)));
      BlockPos best = lookup.get(positions.get(0));
      int currentLayer = mc.player.getBlockPos().getY();
      boolean risingLayer = currentLayer > this.lastClutchLayer;
      boolean preferUp = risingLayer && (isManualJumpHeld() || this.clutchSafeDistanceActive);
      BlockPos result = isTower() && !preferUp && best.getY() != mc.player.getY() - 1.5D ? BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.5D, mc.player.getZ()) : best;
      this.preferUpFace = preferUp;
      this.lastClutchLayer = currentLayer;
      return result;
   }

   private void updateClutchDebug() {
      Vec3d currentPosition = mc.player.getPos();
      if (this.lastClutchDebugPosition == null) {
         this.lastClutchDebugPosition = currentPosition;
         return;
      }

      if (currentPosition.y > this.lastClutchDebugPosition.y) {
         Vec3d currentHorizontalPosition = getHorizontalPosition(currentPosition);
         if (!this.clutchDebugAnchorActive) {
            this.clutchDebugAnchorPosition = getHorizontalPosition(this.lastClutchDebugPosition);
            this.clutchDebugAnchorActive = true;
         }

         float safeDistance = this.clutchSafeDistance.getCurrentValue();
         double horizontalDistance = this.clutchDebugAnchorPosition != null ? currentHorizontalPosition.distanceTo(this.clutchDebugAnchorPosition) : 0.0D;
         double horizontalStep = currentHorizontalPosition.distanceTo(getHorizontalPosition(this.lastClutchDebugPosition));
         double projectedDistance = horizontalDistance + horizontalStep * 6.0D;
         if (this.clutchDebugAnchorPosition != null && projectedDistance > safeDistance) {
            ChatUtils.addChatMessage("Working");
            this.clutchSafeDistanceActive = true;
            this.clutchDebugAnchorPosition = currentHorizontalPosition;
         }
      } else if (currentPosition.y < this.lastClutchDebugPosition.y) {
         this.clutchDebugAnchorActive = false;
         this.clutchDebugAnchorPosition = null;
         this.clutchSafeDistanceActive = false;
      }

      this.lastClutchDebugPosition = currentPosition;
   }

   private Vec3d getHorizontalPosition(Vec3d position) {
      return new Vec3d(position.x, 0.0D, position.z);
   }

   public boolean isValidBlock(BlockPos blockPos) {
      Block block = mc.world.getBlockState(blockPos).getBlock();
      return !(block instanceof FluidBlock) && !(block instanceof AirBlock) && !(block instanceof ChestBlock) && !(block instanceof FurnaceBlock) && !(block instanceof EnderChestBlock) && !(block instanceof ShortPlantBlock) && !(block instanceof SnowBlock) && !(block instanceof EnchantingTableBlock) && !(block instanceof AnvilBlock) && !(block instanceof CraftingTableBlock);
   }

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem blockItem) || stack.getCount() <= 1) {
         return false;
      }
      if (!InventoryUtils.isItemValid(stack)) {
         return false;
      }

      String name = stack.toHoverableText().getString();
      if (name.contains("Click") || name.contains("点击") || stack.getItem() instanceof AliasedBlockItem) {
         return false;
      }

      Block block = blockItem.getBlock();
      return !(block instanceof FlowerBlock) && !(block instanceof PlantBlock) && !(block instanceof FungusBlock) && !(block instanceof CropBlock) && !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return mc.player != null && mc.world != null && !mc.world.getCollisions(mc.player, mc.player.getBoundingBox().offset(0.0D, -0.5D, 0.0D).expand(-sensitivity, 0.0D, -sensitivity)).iterator().hasNext();
   }

   public boolean isBlockUnder() {
      if (mc.player == null || mc.world == null) {
         return false;
      }
      return mc.world.getBlockState(mc.player.getBlockPos().down()).isOpaqueFullCube(mc.world, mc.player.getBlockPos().down());
   }

   public int getBlockCount() {
      if (mc.player == null) {
         return 0;
      }
      int totalBlocks = 0;
      for (int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem) {
            totalBlocks += stack.getCount();
         }
      }
      ItemStack offhandStack = mc.player.getOffHandStack();
      if (offhandStack.getItem() instanceof BlockItem) {
         totalBlocks += offhandStack.getCount();
      }
      return totalBlocks;
   }

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.5D, mc.player.getZ());
      return mc.world.isAir(playerPos) && getPlaceHand() != null;
   }

   private boolean isHitValid(HitResult hit) {
      if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || this.pos == null) {
         return false;
      }
      return isValidBlock(blockHit.getBlockPos())
              && isNearbyBlockPos(blockHit.getBlockPos())
              && blockHit.getSide() != Direction.DOWN
              && (this.canUseLegitUp() || this.preferUpFace || blockHit.getSide() != Direction.UP);
   }

   private boolean isNearbyBlockPos(BlockPos blockPos) {
      if (this.pos == null) {
         return false;
      }
      if (!mc.player.isOnGround()) {
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
      return holdingJump && !this.useLastTellyMovementYaw && !mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed() && !mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed();
   }

   private boolean canUseLegitUp() {
      return this.legitUP.getCurrentValue() && isJumpHeld();
   }

   private boolean isManualJumpHeld() {
      return InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());
   }

   private boolean isJumpHeld() {
      if (this.ignoreJumpDuringSkipTick) {
         if (!Naven.skipTasks.isEmpty()) {
            return false;
         }
         this.ignoreJumpDuringSkipTick = false;
      }
      return isManualJumpHeld();
   }

   private float currentMovementYaw() {
      float realYaw = mc.player.getYaw();
      if (mc.options.backKey.isPressed()) {
         realYaw += 180.0F;
         if (mc.options.leftKey.isPressed()) {
            realYaw += 45.0F;
         } else if (mc.options.rightKey.isPressed()) {
            realYaw -= 45.0F;
         }
      } else if (mc.options.forwardKey.isPressed()) {
         if (mc.options.leftKey.isPressed()) {
            realYaw -= 45.0F;
         } else if (mc.options.rightKey.isPressed()) {
            realYaw += 45.0F;
         }
      } else if (mc.options.rightKey.isPressed()) {
         realYaw += 90.0F;
      } else if (mc.options.leftKey.isPressed()) {
         realYaw -= 90.0F;
      }
      return realYaw;
   }

   private Hand getPlaceHand() {
      if (isValidStack(mc.player.getMainHandStack())) {
         return Hand.MAIN_HAND;
      }
      if (isValidStack(mc.player.getOffHandStack())) {
         return Hand.OFF_HAND;
      }
      return null;
   }

   private boolean isPlaceHitValid(BlockHitResult blockHit, boolean skippedTick) {
      if (!skippedTick && this.rotationMode.isCurrentMode("Strict") && this.strictPlacementFace != null) {
         return blockHit.getBlockPos().equals(this.pos)
                 && isValidBlock(blockHit.getBlockPos())
                 && blockHit.getSide() != Direction.DOWN
                 && blockHit.getSide() == this.strictPlacementFace
                 && !this.isUnsafeRecentSupport(blockHit.getBlockPos())
                 && mc.world.isAir(blockHit.getBlockPos().offset(blockHit.getSide()));
      }
      if (blockHit.getBlockPos().equals(this.pos)) {
         return isValidBlock(blockHit.getBlockPos())
                 && blockHit.getSide() != Direction.DOWN
                 && !this.isUnsafeRecentSupport(blockHit.getBlockPos())
                 && mc.world.isAir(blockHit.getBlockPos().offset(blockHit.getSide()));
      }
      if (!skippedTick || this.pos == null || !isValidBlock(blockHit.getBlockPos()) || blockHit.getSide() == Direction.DOWN) {
         return false;
      }
      if (this.isUnsafeRecentSupport(blockHit.getBlockPos())) {
         return false;
      }

      BlockPos placedPos = blockHit.getBlockPos().offset(blockHit.getSide());
      if (!mc.world.isAir(placedPos)) {
         return false;
      }

      BlockPos hitPos = blockHit.getBlockPos();
      return Math.abs(hitPos.getX() - this.pos.getX()) <= 2
              && Math.abs(hitPos.getY() - this.pos.getY()) <= 1
              && Math.abs(hitPos.getZ() - this.pos.getZ()) <= 2;
   }

   private boolean isUnsafeRecentSupport(BlockPos blockPos) {
      if (!this.preferUpFace && !this.clutchSafeDistanceActive) {
         return false;
      }
      if (mc.world == null) {
         return false;
      }
      long now = mc.world.getTime();
      for (RenderedBlock block : this.renderedBlocks) {
         if (block.position.equals(blockPos) && now - block.placedTick <= 2L) {
            return true;
         }
      }
      return false;
   }

   private int findBlockSlot() {
      for (int i = 0; i < 9; ++i) {
         if (isValidStack(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }
      return -1;
   }

   private static class RenderedBlock {
      private final BlockPos position;
      private final long placedTick;
      private int lifetime = 10;

      private RenderedBlock(BlockPos position, long placedTick) {
         this.position = position;
         this.placedTick = placedTick;
      }

      private boolean tick() {
         return --this.lifetime > 0;
      }

      private float getAlpha() {
         return this.lifetime / 10.0F * 0.8F;
      }
   }
}
