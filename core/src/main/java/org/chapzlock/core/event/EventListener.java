package org.chapzlock.core.event;

/**
 * Functional interface representing an event listener.
 *
 * @param <T> The type of event this listener handles.
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    boolean onEvent(T event);
}
