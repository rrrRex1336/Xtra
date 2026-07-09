package awa.qwq.ovo.Naven.events.impl;

import awa.qwq.ovo.Naven.events.api.events.Event;
import awa.qwq.ovo.Naven.events.api.events.callables.EventCancellable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

@Setter
@Getter
public class EventAttack extends EventCancellable implements Event {
    private boolean post;
    private boolean pre;
    private final Entity target;

    public EventAttack(boolean post, Entity target) {
        this.post = post;
        this.pre = pre;
        this.target = target;
    }

    public void setPost(boolean post) {
        this.post = post;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EventAttack other)) {
            return false;
        } else if (!other.canEqual(this)) {
            return false;
        } else if (this.isPost() != other.isPost()) {
            return false;
        } else {
            Object this$target = this.getTarget();
            Object other$target = other.getTarget();
            return this$target == null ? other$target == null : this$target.equals(other$target);
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof EventAttack;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isPost() ? 79 : 97);
        Object $target = this.getTarget();
        return result * 59 + ($target == null ? 43 : $target.hashCode());
    }

    @Override
    public String toString() {
        return "EventAttack(post=" + this.isPost() + ", target=" + this.getTarget() + ")";
    }
}
