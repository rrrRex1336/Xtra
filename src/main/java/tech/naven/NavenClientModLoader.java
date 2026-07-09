package tech.naven;

import awa.qwq.ovo.Naven.viaversionfix.items.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class NavenClientModLoader implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      EntityRendererRegistry.register(ModEntities.WIND_CHARGE_PROJECTILE, context -> new FlyingItemEntityRenderer<>(context, 0.75F, true));
   }
}
