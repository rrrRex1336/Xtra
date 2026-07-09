package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import java.util.Objects;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(
   name = "Teams",
   description = "Prevent attack teammates",
   category = Category.MISC
)
public class Teams extends Module {
   public static Teams instance;
   public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Scoreboard", "Color").build().getModeValue();

   public Teams() {
      instance = this;
   }

   public static boolean isSameTeam(Entity player) {
      if (!Naven.getInstance().getModuleManager().getModule(Teams.class).isEnabled()) {
         return false;
      } else if (player instanceof PlayerEntity) {
         if (instance.mode.isCurrentMode("Color")) {
            Integer c1 = player.getTeamColorValue();
            Integer c2 = mc.player.getTeamColorValue();
            return c1.equals(c2);
         } else {
            String playerTeam = getTeam(player);
            String targetTeam = getTeam(mc.player);
            return Objects.equals(playerTeam, targetTeam);
         }
      } else {
         return false;
      }
   }

   public static String getTeam(Entity entity) {
      PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
      if (playerInfo == null) {
         return null;
      } else {
         return playerInfo.getScoreboardTeam() != null ? playerInfo.getScoreboardTeam().getName() : null;
      }
   }
}
