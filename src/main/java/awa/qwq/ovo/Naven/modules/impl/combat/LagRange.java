package awa.qwq.ovo.Naven.modules.impl.combat;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.events.api.types.EventType;
import awa.qwq.ovo.Naven.events.impl.EventAttack;
import awa.qwq.ovo.Naven.events.impl.EventMotion;
import awa.qwq.ovo.Naven.events.impl.EventPacket;
import awa.qwq.ovo.Naven.events.impl.EventRender;
import awa.qwq.ovo.Naven.modules.Category;
import awa.qwq.ovo.Naven.modules.Module;
import awa.qwq.ovo.Naven.modules.ModuleInfo;
import awa.qwq.ovo.Naven.utils.*;
import awa.qwq.ovo.Naven.values.ValueBuilder;
import awa.qwq.ovo.Naven.values.impl.BooleanValue;
import awa.qwq.ovo.Naven.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(name = "LagRange", description = "Lags your movement and releases attack packets to hit entities from far away.", category = Category.COMBAT)
public class LagRange extends Module {
    public static final Set<Class<?>> whitelist = new HashSet<>() {
        {
            this.add(HandshakeC2SPacket.class);
            this.add(QueryRequestC2SPacket.class);
            this.add(QueryPingC2SPacket.class);
            this.add(LoginHelloC2SPacket.class);
            this.add(LoginKeyC2SPacket.class);
        }
    };
    private final java.util.Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);

    public FloatValue maxTicks = ValueBuilder.create(this, "Max Ticks")
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    public BooleanValue smart = ValueBuilder.create(this, "Smart Release")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private boolean disabling = false;
    private boolean enabling = false;
    @Getter
    private int shouldReleaseTicks = 0;
    private int releasedTicks = 0;
    private Vec3d targetPosition = null;
    private boolean hasPosition = false;
    private LivingEntity currentAttackTarget = null;
    @Getter
    private int lastAttackTick = 0;
    private int targetLagTicks = 1;
    private int currentLagTicks = 1;
    @Getter
    private LivingEntity currentTarget = null;
    @Getter
    private int targetHurtTime = 0;
    @Getter
    private int noTargetTimer = 0;
    private Vec3d lastTargetPosition = null;
    private long lastUpdateTime = 0;

    private void updateTargetPosition(PlayerMoveC2SPacket packet) {
        if (packet.changesPosition()) {
            if (this.targetPosition != null) {
                this.lastTargetPosition = this.targetPosition;
            }
            this.targetPosition = new Vec3d(
                    packet.getX(0),
                    packet.getY(0),
                    packet.getZ(0)
            );
            this.lastUpdateTime = System.currentTimeMillis();
            this.hasPosition = true;
        }
    }

    private Vec3d getInterpolatedPosition(float partialTicks) {
        if (targetPosition == null) return null;
        if (lastTargetPosition == null) return targetPosition;

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        if (delta > 100) return targetPosition;

        float progress = Math.min(1.0f, delta / 50.0f);
        return lastTargetPosition.lerp(targetPosition, progress);
    }

    private long getBlinkTicks() {
        return this.packets.stream().filter(packet -> packet instanceof PlayerMoveC2SPacket).count();
    }

    private void releaseTick() {
        while (!this.packets.isEmpty()) {
            Packet<?> poll = this.packets.poll();
            NetworkUtils.sendPacketNoEvent(poll);
            if (poll instanceof PlayerMoveC2SPacket movePacket) {
                this.releasedTicks++;
                this.updateTargetPosition(movePacket);
                break;
            }
        }
    }

    private void releaseAllPackets() {
        while (!this.packets.isEmpty()) {
            Packet<?> packet = this.packets.poll();
            if (packet != null) {
                NetworkUtils.sendPacketNoEvent(packet);
            }
        }
        this.targetPosition = null;
        this.hasPosition = false;
    }

    private void releaseToTarget(int targetTicks) {
        long currentTicks = getBlinkTicks();
        int toRelease = (int) (currentTicks - targetTicks);
        if (toRelease <= 0) return;

        int released = 0;
        while (!this.packets.isEmpty() && released < toRelease && released < 10) {
            Packet<?> poll = this.packets.poll();
            NetworkUtils.sendPacketNoEvent(poll);
            if (poll instanceof PlayerMoveC2SPacket movePacket) {
                released++;
                this.updateTargetPosition(movePacket);
            }
        }
    }

    @Override
    public void onEnable() {
        this.packets.clear();
        this.disabling = false;
        this.shouldReleaseTicks = 0;
        this.releasedTicks = 0;
        this.progress.value = 0.0F;
        this.progress.target = 0.0F;
        this.targetPosition = null;
        this.hasPosition = false;
        this.targetLagTicks = 1;
        this.currentLagTicks = 1;
        this.currentTarget = null;
        this.targetHurtTime = 0;
        this.noTargetTimer = 0;
    }

    @Override
    public void onDisable() {
        this.releaseAllPackets();
    }

    @EventTarget
    public void onRenderWorld(EventRender e) {
        if (!this.hasPosition || this.targetPosition == null) return;
        Vec3d renderPos = getInterpolatedPosition(e.getRenderPartialTicks());
        if (renderPos == null) renderPos = this.targetPosition;

        MatrixStack stack = e.getPMatrixStack();
        stack.push();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderUtils.applyRegionalRenderOffset(stack);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.8F);
        Box box = new Box(
                renderPos.x - 0.3, renderPos.y, renderPos.z - 0.3,
                renderPos.x + 0.3, renderPos.y + 1.8, renderPos.z + 0.3
        );
        RenderUtils.drawSolidBox(box, stack);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        stack.pop();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mc.player == null) {
            return;
        }

        if (enabled) {
            if (!this.enabling) {
                this.enabling = true;
                super.setEnabled(true);
                this.enabling = false;
            }
        } else {
            if (!this.disabling) {
                this.disabling = true;
                super.setEnabled(false);
                this.disabling = false;
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack e) {
        if (!smart.getCurrentValue()) return;
        if (e.isPost() && e.getTarget() instanceof LivingEntity target && target != mc.player) {
            currentAttackTarget = target;
            if (targetLagTicks < maxTicks.getCurrentValue()) {
                targetLagTicks++;
            }
        }
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            if (smart.getCurrentValue()) {
                boolean hasValidTarget = currentAttackTarget != null && currentAttackTarget.isAlive();

                if (hasValidTarget) {
                    noTargetTimer = 0;
                    int currentHurtTime = currentAttackTarget.hurtTime;
                    float hurtTimeThreshold = this.hurtTime.getCurrentValue();

                    if (currentHurtTime >= hurtTimeThreshold) {
                        if (targetLagTicks < maxTicks.getCurrentValue()) {
                            targetLagTicks++;
                        }
                    } else if (currentHurtTime > 0) {
                        if (targetLagTicks > 1) {
                            targetLagTicks--;
                        }
                    } else {  // currentHurtTime == 0
                        if (targetLagTicks > 1) {
                            targetLagTicks--;
                        }
                    }
                } else {
                    if (targetLagTicks > 1) {
                        targetLagTicks -= 2;
                        if (targetLagTicks < 1) targetLagTicks = 1;
                    }
                    currentAttackTarget = null;
                }

                targetLagTicks = MathHelper.clamp(targetLagTicks, 1, (int) maxTicks.getCurrentValue());
                if (currentLagTicks < targetLagTicks) {
                    currentLagTicks++;
                } else if (currentLagTicks > targetLagTicks) {
                    currentLagTicks--;
                }

                long currentTicks = getBlinkTicks();
                if (currentTicks > currentLagTicks) {
                    releaseToTarget(currentLagTicks);
                }

                this.setSuffix("Lag: " + currentLagTicks + "/" + maxTicks.getCurrentValue());
                this.progress.target = MathHelper.clamp((float) currentLagTicks / this.maxTicks.getCurrentValue() * 100.0F, 0.0F, 100.0F);
            } else {
                this.setSuffix("Lag Ticks: " + this.getBlinkTicks());
                this.progress.target = MathHelper.clamp((float) this.getBlinkTicks() / this.maxTicks.getCurrentValue() * 100.0F, 0.0F, 100.0F);
                this.releasedTicks = 0;
                while ((float)this.releasedTicks < 10.0F && (float)this.getBlinkTicks() >= this.maxTicks.getCurrentValue() && !this.packets.isEmpty()) {
                    this.releaseTick();
                }
            }

            if (this.enabling && (float)this.getBlinkTicks() < this.maxTicks.getCurrentValue()) {
                while ((float)this.releasedTicks < 3.0F && !this.packets.isEmpty()) {
                    this.releaseTick();
                }
            }

            if (this.disabling) {
                while ((float)this.releasedTicks < 10.0F && !this.packets.isEmpty()) {
                    this.releaseTick();
                }
                if (this.packets.isEmpty()) {
                    this.setEnabled(false);
                }
            }
        }
    }

    @EventTarget(4)
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.SEND && mc.player != null && !e.isCancelled()) {
            if (whitelist.contains(e.getPacket().getClass())) {
                return;
            }

            e.setCancelled(true);
            if (e.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
                if (this.packets.isEmpty()) {
                    this.updateTargetPosition(movePacket);
                }
            }

            this.packets.offer(e.getPacket());
        }
    }
}