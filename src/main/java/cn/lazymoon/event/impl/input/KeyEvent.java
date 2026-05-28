package cn.lazymoon.event.impl.input;

import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Setter
@Getter
public class KeyEvent implements Event {
    private int key;

    public KeyEvent(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}
