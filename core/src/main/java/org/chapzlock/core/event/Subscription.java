package org.chapzlock.core.event;

/**
 * Represents a handle to a listener registration.
 * <p>
 * Use {@link #unsubscribe()} or {@link #close()} to remove the listener.
 * Once unsubscribed, the listener will no longer receive events.
 */
public final class Subscription implements AutoCloseable {
    private final EventBus eventBus;
    private final int id;
    private final Class<? extends Event> eventClass;
    private volatile boolean active = true;

    Subscription(EventBus eventBus, int id, Class<? extends Event> eventClass) {
        this.eventBus = eventBus;
        this.id = id;
        this.eventClass = eventClass;
    }

    /**
     * Returns whether this subscription is still active.
     *
     * @return {@code true} if active, {@code false} if unsubscribed.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Equivalent to {@link #unsubscribe()}.
     * Enables use in try-with-resources blocks.
     */
    @Override
    public void close() {
        unsubscribe();
    }

    /**
     * Unsubscribes the listener associated with this subscription.
     * <p>Safe to call multiple times; subsequent calls have no effect.</p>
     */
    public void unsubscribe() {
        if (!active) {
            return;
        }
        active = false;
        eventBus.removeListener(eventClass, id);
    }
}
