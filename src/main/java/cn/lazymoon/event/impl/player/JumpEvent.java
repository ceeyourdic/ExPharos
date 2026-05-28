package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JumpEvent extends CancellableEvent {
    public float yaw;
    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYRot() {
        return yaw;
    }

    public void setYRot(float yaw) {
        this.yaw = yaw;
    }
}
