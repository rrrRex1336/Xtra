package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelProtocol;
import awa.qwq.ovo.Naven.protocol.heypixel.HeypixelSession;
import awa.qwq.ovo.Naven.protocol.heypixel.network.HeypixelNetworkBridge;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;

@ModuleInfo(
        name = "Protocol",
        description = "Fix the Protocol issue on your Minecraft server hosted by NetEase",
        category = Category.MISC
)
public class Protocol extends Module {
   private static Protocol instance;

   public Protocol() {
      instance = this;
   }

   public static boolean isProtocolEnabled() {
      return instance != null && instance.isEnabled();
   }

   @Override
   public void onEnable() {
      HeypixelSession.resetCurrent();
      HeypixelProtocol.enable();
   }

   @Override
   public void onDisable() {
      HeypixelProtocol.disable();
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getType() == EventType.SEND) {
         this.handleSend(event);
      } else if (event.getType() == EventType.RECEIVE) {
         this.handleReceive(event);
      }
   }

   private void handleSend(EventPacket event) {
      if (event.getPacket() instanceof HandSwingC2SPacket) {
         HeypixelSession.current().getClickTracker().recordSwingArm();
      } else if (event.getPacket() instanceof PlayerInteractItemC2SPacket) {
         HeypixelSession.current().getClickTracker().recordUseItem();
      } else if (event.getPacket() instanceof CustomPayloadC2SPacket customPayload
              && HeypixelNetworkBridge.MINECRAFT_REGISTER.equals(customPayload.payload().id())
              && !HeypixelNetworkBridge.isInjectingRegister()) {
         event.setCancelled(true);
      }
   }

   private void handleReceive(EventPacket event) {
      if (event.getPacket() instanceof GameJoinS2CPacket) {
         HeypixelSession session = HeypixelSession.current();
         session.activate();
         session.sendInitialInfo();
      } else if (event.getPacket() instanceof TeamS2CPacket teamPacket
              && teamPacket.getPlayerListOperation() == TeamS2CPacket.Operation.REMOVE) {
         event.setCancelled(true);
      }
   }

}
