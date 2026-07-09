package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientWorld.class})
public class MixinClientLevel {
   @Redirect(
      method = {"tickEntity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;tick()V"
      )
   )
   public void hookSkipTicks(Entity instance) {
      if (!Naven.skipTasks.isEmpty() && instance == MinecraftClient.getInstance().player) {
         Runnable task = Naven.skipTasks.poll();
         if (task != null) {
            task.run();
         }
      } else {
         instance.tick();
      }
   }
}
