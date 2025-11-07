package org.chapzlock.core.event;

/**
 * Marker interface for all events dispatched via the {@link EventBus}.
 * <p>
 * Implementing classes can define any additional data fields needed by the listener.
 * Example:
 * <pre>{@code
 * public record PlayerDiedEvent(String playerName) implements Event {}
 * }</pre>
 */
public interface Event {
}
