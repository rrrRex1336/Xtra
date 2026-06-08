package awa.qwq.ovo.Naven.utils;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlinkingPlayer extends RemotePlayer {
   private final AbstractClientPlayer player;

   public BlinkingPlayer(AbstractClientPlayer player) {
      super(Minecraft.getInstance().level, new GameProfile(UUID.randomUUID(), "Real Position"));
      this.player = player;
      this.copyPosition(player);
      this.noPhysics = true;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.yHeadRot = player.yHeadRot;
      this.yBodyRot = player.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
      PlayerSkin playerSkin = player.getSkin();
      byte playerModel = (byte) (playerSkin.model() == PlayerSkin.Model.SLIM ? 1 : 0);
      this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
   }

   public boolean isSkinLoaded() {
      return true;
   }

   @NotNull
   public ResourceLocation getSkinTextureLocation() {
      PlayerSkin skin = this.player.getSkin();
      return skin.texture();
   }

   public boolean isCapeLoaded() {
      PlayerSkin skin = this.player.getSkin();
      return skin != null && skin.capeTexture() != null;
   }

   @Nullable
   public ResourceLocation getCloakTextureLocation() {
      PlayerSkin skin = this.player.getSkin();
      return skin != null ? skin.capeTexture() : null;
   }
}