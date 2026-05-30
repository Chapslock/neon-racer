package org.chapzlock.core.event;

/**
 * Functional interface representing an event listener.
 *
 * <p>Return {@code true} to stop event propagation (no further listeners receive it).
 * Return {@code false} to continue propagation.
 *
 * @param <T> The type of event this listener handles.
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    /**
     * Handles an event.
     *
     * @param event the event
     * @return {@code true} to stop propagation, {@code false} to continue
     */
    boolean onEvent(T event);
}
