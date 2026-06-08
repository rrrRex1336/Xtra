package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.EventTarget;
import awa.qwq.ovo.Naven.utils.ISkipTicks;

import java.util.HashMap;
import java.util.Map;

public final class EventMovement implements IMinecraft {
    public static final EventMovement INSTANCE = new EventMovement();

    private static final Map<Object, ModuleState> activeModules = new HashMap<>();

    private static class ModuleState {
        boolean forceStuck;
        long activateTime;

        ModuleState(boolean forceStuck) {
            this.forceStuck = forceStuck;
            this.activateTime = System.currentTimeMillis();
        }
    }

    public static void cancelMove() {
        cancelMove(DEFAULT_MODULE, false);
    }

    public static void cancelMove(boolean force) {
        cancelMove(DEFAULT_MODULE, force);
    }

    private static final Object DEFAULT_MODULE = new Object();

    public static void cancelMove(Object module) {
        cancelMove(module, false);
    }

    public static void cancelMove(Object module, boolean force) {
        if (mc.player == null) {
            return;
        }

        if (activeModules.containsKey(module)) {
            ModuleState state = activeModules.get(module);
            state.forceStuck = force;
        } else {
            activeModules.put(module, new ModuleState(force));
        }

        if (activeModules.size() == 1) {
            setSkipTicks(0);
        }
    }

    public static void resetMove() {
        resetMove(DEFAULT_MODULE);
    }

    public static void resetMove(Object module) {
        activeModules.remove(module);

        if (activeModules.isEmpty()) {
            setSkipTicks(0);
        }
    }

    public static boolean isMoveCancelled() {
        return !activeModules.isEmpty();
    }

    public static boolean isForceStuck() {
        for (ModuleState state : activeModules.values()) {
            if (state.forceStuck) {
                return true;
            }
        }
        return false;
    }

    public static int getActiveModuleCount() {
        return activeModules.size();
    }

    public static long getModuleActivateTime(Object module) {
        ModuleState state = activeModules.get(module);
        return state != null ? state.activateTime : 0;
    }

    private static ISkipTicks getSkipTicksAccessor() {
        return (ISkipTicks) mc;
    }

    private static int getSkipTicks() {
        return getSkipTicksAccessor().getSkipTicks();
    }

    private static void setSkipTicks(int ticks) {
        getSkipTicksAccessor().setSkipTicks(ticks);
    }

    @EventTarget
    public void onMove(EventMoveInStuck event) {
        if (isMoveCancelled()) {
            if (isForceStuck()) {
                return;
            }
            if (getSkipTicks() > 0) {
                return;
            }
            setSkipTicks(19);
        }
    }

    @EventTarget
    public void onTick(EventStuckTick event) {
        if (isMoveCancelled() && isForceStuck()) {
            setSkipTicks(19);
        }
    }
}