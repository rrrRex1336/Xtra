package org.mixin.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ClientPlayerInteractionManager.class})
public interface MultiPlayerGameModeAccessor {
   @Invoker("syncSelectedSlot")
   void invokeEnsureHasSentCarriedItem();

   @Accessor("gameMode")
   GameMode getLocalPlayerMode();
}
