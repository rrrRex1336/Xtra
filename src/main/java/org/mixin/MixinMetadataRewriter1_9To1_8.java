package org.mixin;

import awa.qwq.ovo.Naven.modules.impl.misc.ViaVersionFix;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.viaversion.viaversion.protocols.protocol1_9to1_8.metadata.MetadataRewriter1_9To1_8", remap = false)
public abstract class MixinMetadataRewriter1_9To1_8 {
   private static final String PROTOCOL_1_9_TO_1_8 = "com.viaversion.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8";

   @Inject(
      method = "handleMetadata",
      at = @At(
         value = "FIELD",
         target = "Lcom/viaversion/viaversion/protocols/protocol1_9to1_8/metadata/MetaIndex;PLAYER_HAND:Lcom/viaversion/viaversion/protocols/protocol1_9to1_8/metadata/MetaIndex;",
         ordinal = 0,
         shift = At.Shift.BEFORE
      ),
      cancellable = true,
      remap = false
   )
   private void preventSelfBlockingMetadata(@Coerce Object event, @Coerce Object metadata, CallbackInfo ci) {
      if (!ViaVersionFix.isBlockingFixEnabled()) {
         return;
      }

      int entityId = getInt(event, "entityId");
      int clientEntityId = getClientEntityId(event);
      if (entityId != Integer.MIN_VALUE && clientEntityId == entityId) {
         ci.cancel();
      }
   }

   private static int getClientEntityId(Object event) {
      try {
         Object user = invoke(event, "user");
         Class<?> protocolClass = Class.forName(PROTOCOL_1_9_TO_1_8);
         Method getEntityTracker = user.getClass().getMethod("getEntityTracker", Class.class);
         getEntityTracker.setAccessible(true);
         Object tracker = getEntityTracker.invoke(user, protocolClass);
         return getInt(tracker, "clientEntityId");
      } catch (Throwable ignored) {
         return Integer.MIN_VALUE;
      }
   }

   private static int getInt(Object target, String methodName) {
      try {
         Object value = invoke(target, methodName);
         return value instanceof Number number ? number.intValue() : Integer.MIN_VALUE;
      } catch (Throwable ignored) {
         return Integer.MIN_VALUE;
      }
   }

   private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
      if (target == null) {
         return null;
      }

      Method method = target.getClass().getMethod(methodName);
      method.setAccessible(true);
      return method.invoke(target);
   }
}
