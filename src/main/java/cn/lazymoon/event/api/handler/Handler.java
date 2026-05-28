package cn.lazymoon.event.api.handler;

import cn.lazymoon.event.api.EventTarget;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class Handler {

    @Getter
    private final Object listener;
    @Getter
    private final Method method;
    private final EventTarget annotation;

    public Handler(Object listener, Method method, EventTarget annotation) {
        this.listener = listener;
        this.method = method;
        this.annotation = annotation;
    }

    public Object getListener() {
        return listener;
    }

    public Method getMethod() {
        return method;
    }

    public byte getPriority() {
        return annotation.value();
    }

    public boolean isIgnoringCancelled() {
        return annotation.ignoreCancelled();
    }
}
