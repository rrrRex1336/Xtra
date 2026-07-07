package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;

public class EventAttackSlowdown extends EventCancellable {
    private final Type type;
    private double motionXZ;

    public enum Type {
        Delta_Movement,
        Sprinting
    }

    public EventAttackSlowdown(Type type) {
        this(type, 0.6D);
    }

    public EventAttackSlowdown(Type type, double motionXZ) {
        this.type = type;
        this.motionXZ = motionXZ;
    }

    public Type getType() {
        return type;
    }

    public double getMotionXZ() {
        return motionXZ;
    }

    public void setMotionXZ(double motionXZ) {
        this.motionXZ = motionXZ;
    }
}
