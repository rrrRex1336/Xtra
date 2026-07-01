package awa.qwq.ovo.Naven.viaversionfix.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   public static final SoundEvent MACE_SMASH_GROUND = register("mace_smash_ground");
   public static final SoundEvent MACE_SMASH_AIR = register("mace_smash_air");
   public static final SoundEvent MACE_SMASH_GROUND_HEAVY = register("mace_smash_ground_heavy");
   public static final SoundEvent VANILLA_MACE_SMASH_GROUND = registerMinecraft("item.mace.smash_ground");
   public static final SoundEvent VANILLA_MACE_SMASH_AIR = registerMinecraft("item.mace.smash_air");
   public static final SoundEvent VANILLA_MACE_SMASH_GROUND_HEAVY = registerMinecraft("item.mace.smash_ground_heavy");

   private ModSounds() {
   }

   public static void init() {
   }

   private static SoundEvent register(String name) {
      ResourceLocation id = new ResourceLocation(ModItems.MOD_ID, name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   private static SoundEvent registerMinecraft(String name) {
      ResourceLocation id = new ResourceLocation("minecraft", name);
      return BuiltInRegistries.SOUND_EVENT.getOptional(id)
         .orElseGet(() -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
   }
}
