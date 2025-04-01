package org.clientbase.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author LangYa466
 * @since 2025/3/22
 */
public class EventManager {
    // java.util.ConcurrentModificationException: null 很喜欢arraylist小朋友
    private final Map<Class<?>, CopyOnWriteArrayList<Object>> listenersMap;
    private final Logger logger = LogManager.getLogger();

    public EventManager() {
        listenersMap = new ConcurrentHashMap<>();
    }

    public void register(Object listener) {
        Arrays.stream(listener.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventTarget.class))
                .forEach(method -> listenersMap
                        .computeIfAbsent(method.getParameterTypes()[0], k -> new CopyOnWriteArrayList<>())
                        .add(listener));
    }

    public void unregister(Object listener) {
        listenersMap.values().forEach(list -> list.remove(listener));
        listenersMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public <T> void post(T event) {
        CopyOnWriteArrayList<Object> listeners = listenersMap.get(event.getClass());
        if (listeners != null) {
            listeners.forEach(listener -> Arrays.stream(listener.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventTarget.class) && method.getParameterTypes()[0].equals(event.getClass()))
                    .forEach(method -> {
                        try {
                            method.setAccessible(true);
                            method.invoke(listener, event);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }));
        }
    }
}