package com.novaedit.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight, thread-safe publish-subscribe EventBus for loose coupling across components.
 */
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add((Consumer<Object>) listener);
    }

    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<Object>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void publish(Object event) {
        if (event == null) return;
        List<Consumer<Object>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<Object> consumer : list) {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(EventBus.class)
                            .error("Error dispatching event {}", event.getClass().getSimpleName(), e);
                }
            }
        }
    }
}
