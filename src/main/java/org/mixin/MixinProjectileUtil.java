package org.mixin;

import awa.qwq.ovo.Naven.utils.BlinkingPlayer;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ProjectileUtil.class})
public class MixinProjectileUtil {
   @Redirect(
      method = {"raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
      )
   )
   private static List<Entity> hook(World instance, Entity pEntity, Box pBoundingBox, Predicate<? super Entity> pPredicate) {
      List<Entity> entities = instance.getOtherEntities(pEntity, pBoundingBox, pPredicate);
      entities.removeIf(entity -> entity instanceof BlinkingPlayer);
      return entities;
   }
}
