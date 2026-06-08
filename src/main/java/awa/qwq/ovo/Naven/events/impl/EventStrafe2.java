package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;

public class EventStrafe2 implements Event {
    private float yaw;
    private float friction;
    private float forward;
    private float strafe;
    private boolean onGround;

    public EventStrafe2(float yaw, float friction, float forward, float strafe, boolean onGround) {
        this.yaw = yaw;
        this.friction = friction;
        this.forward = forward;
        this.strafe = strafe;
        this.onGround = onGround;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getFriction() { return friction; }
    public void setFriction(float friction) { this.friction = friction; }

    public float getForward() { return forward; }
    public void setForward(float forward) { this.forward = forward; }

    public float getStrafe() { return strafe; }
    public void setStrafe(float strafe) { this.strafe = strafe; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
}