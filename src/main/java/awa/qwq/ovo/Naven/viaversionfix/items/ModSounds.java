package awa.qwq.ovo.Naven.viaversionfix.items;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
   public static final SoundEvent MACE_SMASH_GROUND = register("mace_smash_ground");
   public static final SoundEvent MACE_SMASH_AIR = register("mace_smash_air");
   public static final SoundEvent MACE_SMASH_GROUND_HEAVY = register("mace_smash_ground_heavy");
   public static final SoundEvent WIND_CHARGE_THROW = register("wind_charge_throw");
   public static final SoundEvent WIND_CHARGE_WIND_BURST = register("wind_charge_wind_burst");
   public static final SoundEvent SPEAR_ATTACK = register("spear_attack");
   public static final SoundEvent SPEAR_HIT = register("spear_hit");
   public static final SoundEvent SPEAR_USE = register("spear_use");
   public static final SoundEvent SPEAR_LUNGE_1 = register("spear_lunge_1");
   public static final SoundEvent SPEAR_LUNGE_2 = register("spear_lunge_2");
   public static final SoundEvent SPEAR_LUNGE_3 = register("spear_lunge_3");
   public static final SoundEvent SPEAR_WOOD_ATTACK = register("spear_wood_attack");
   public static final SoundEvent SPEAR_WOOD_HIT = register("spear_wood_hit");
   public static final SoundEvent SPEAR_WOOD_USE = register("spear_wood_use");
   public static final SoundEvent VANILLA_MACE_SMASH_GROUND = registerMinecraft("item.mace.smash_ground");
   public static final SoundEvent VANILLA_MACE_SMASH_AIR = registerMinecraft("item.mace.smash_air");
   public static final SoundEvent VANILLA_MACE_SMASH_GROUND_HEAVY = registerMinecraft("item.mace.smash_ground_heavy");
   public static final SoundEvent VANILLA_WIND_CHARGE_THROW = registerMinecraft("entity.wind_charge.throw");
   public static final SoundEvent VANILLA_WIND_CHARGE_WIND_BURST = registerMinecraft("entity.wind_charge.wind_burst");
   public static final SoundEvent VANILLA_SPEAR_ATTACK = registerMinecraft("item.spear.attack");
   public static final SoundEvent VANILLA_SPEAR_HIT = registerMinecraft("item.spear.hit");
   public static final SoundEvent VANILLA_SPEAR_LUNGE_1 = registerMinecraft("item.spear.lunge_1");
   public static final SoundEvent VANILLA_SPEAR_LUNGE_2 = registerMinecraft("item.spear.lunge_2");
   public static final SoundEvent VANILLA_SPEAR_LUNGE_3 = registerMinecraft("item.spear.lunge_3");
   public static final SoundEvent VANILLA_SPEAR_USE = registerMinecraft("item.spear.use");
   public static final SoundEvent VANILLA_SPEAR_WOOD_ATTACK = registerMinecraft("item.spear_wood.attack");
   public static final SoundEvent VANILLA_SPEAR_WOOD_HIT = registerMinecraft("item.spear_wood.hit");
   public static final SoundEvent VANILLA_SPEAR_WOOD_USE = registerMinecraft("item.spear_wood.use");

   private ModSounds() {
   }

   public static void init() {
   }

   private static SoundEvent register(String name) {
      Identifier id = new Identifier(ModItems.MOD_ID, name);
      return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
   }

   private static SoundEvent registerMinecraft(String name) {
      Identifier id = new Identifier("minecraft", name);
      return Registries.SOUND_EVENT.getOrEmpty(id)
         .orElseGet(() -> Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
   }
}
