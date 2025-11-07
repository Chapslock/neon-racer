package org.chapzlock.core.event;

import java.util.function.Predicate;

/**
 * Immutable record representing a registered listener entry.
 *
 * @param id       The unique listener ID.
 * @param listener The listener callback.
 * @param filter   The filter determining whether this listener should handle an event.
 */
record ListenerHolder(int id, EventListener<Event> listener, Predicate<Event> filter) {
}
