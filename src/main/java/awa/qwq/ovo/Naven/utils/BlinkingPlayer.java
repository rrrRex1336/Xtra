package awa.qwq.ovo.Naven.utils;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlinkingPlayer extends OtherClientPlayerEntity {
   private final AbstractClientPlayerEntity player;

   public BlinkingPlayer(AbstractClientPlayerEntity player) {
      super(MinecraftClient.getInstance().world, new GameProfile(UUID.randomUUID(), "Real Position"));
      this.player = player;
      this.copyPositionAndRotation(player);
      this.noClip = true;
      this.prevYaw = this.getYaw();
      this.prevPitch = this.getPitch();
      this.headYaw = player.headYaw;
      this.bodyYaw = player.bodyYaw;
      this.prevHeadYaw = this.headYaw;
      this.prevBodyYaw = this.bodyYaw;
      SkinTextures playerSkin = player.getSkinTextures();
      byte playerModel = (byte) (playerSkin.model() == SkinTextures.Model.SLIM ? 1 : 0);
      this.dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
   }

   public boolean isSkinLoaded() {
      return true;
   }

   @NotNull
   public Identifier getSkinTextureLocation() {
      SkinTextures skin = this.player.getSkinTextures();
      return skin.texture();
   }

   public boolean isCapeLoaded() {
      SkinTextures skin = this.player.getSkinTextures();
      return skin != null && skin.capeTexture() != null;
   }

   @Nullable
   public Identifier getCloakTextureLocation() {
      SkinTextures skin = this.player.getSkinTextures();
      return skin != null ? skin.capeTexture() : null;
   }
}