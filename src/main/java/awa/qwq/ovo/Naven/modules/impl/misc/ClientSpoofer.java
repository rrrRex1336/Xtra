package awa.qwq.ovo.Naven.modules.impl.misc;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.ModeValue;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

@ModuleInfo(
        name = "ClientSpoofer",
        description = "Spoof the client brand sent to the server.",
        category = Category.MISC
)
public class ClientSpoofer extends Module {
   public final ModeValue mode = ValueBuilder.create(this, "Mode")
           .setDefaultModeIndex(0)
           .setModes("Forge", "Fabric")
           .setOnUpdate(value -> this.updateSuffix())
           .build()
           .getModeValue();

   @Override
   public void onEnable() {
      this.updateSuffix();
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getType() != EventType.SEND) {
         return;
      }

      if (event.getPacket() instanceof CustomPayloadC2SPacket packet
              && packet.payload() instanceof BrandCustomPayload) {
         this.updateSuffix();
         event.setPacket(new CustomPayloadC2SPacket(new BrandCustomPayload(this.getBrand())));
      }
   }

   private String getBrand() {
      return this.mode.isCurrentMode("Forge") ? "forge" : "fabric";
   }

   private void updateSuffix() {
      this.setSuffix(this.mode.getCurrentMode());
   }
}
