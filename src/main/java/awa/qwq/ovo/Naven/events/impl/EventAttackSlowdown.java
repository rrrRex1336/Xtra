package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;

public class EventAttackSlowdown extends EventCancellable {
    private final Type type;

    public enum Type {
        Delta_Movement,
        Sprinting
    }

    public EventAttackSlowdown(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}