package org.mixin;

import awa.qwq.ovo.Naven.Naven;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkThreadUtils.class)
public class MixinPacketUtils {

    /**
     * @reason 拦截 Velocity 包；在 HEAD 派发事件，需要取消时 ci.cancel()，
     *         让 vanilla 处理主线程调度，避免 @Overwrite 里手调 packet.apply 造成递归 StackOverflow。
     */
    @Inject(
        method = "forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static <T extends PacketListener> void beforeForceMainThread(
            Packet<T> packet, T listener, ThreadExecutor<?> loop, CallbackInfo ci) {

        if (packet instanceof EntityVelocityUpdateS2CPacket) {
            EventPacket event = new EventPacket(EventType.RECEIVE, packet);
            Naven.getInstance().getEventManager().call(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
