package org.chapzlock.core.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.chapzlock.core.logging.Log;

import lombok.NonNull;

/**
 * Thread-safe, synchronous event bus for in-process event dispatch.
 *
 * <p>This implementation is optimized for:
 * <ul>
 *   <li>Frequent event publishing (reads)</li>
 *   <li>Rare listener (un)subscription (writes)</li>
 * </ul>
 *
 * <p>It achieves this by:
 * <ul>
 *   <li>Storing listeners in {@link CopyOnWriteArrayList}s (lock-free iteration)</li>
 *   <li>Caching the full type hierarchy of event classes using {@link ClassValue}</li>
 *   <li>Supporting predicate-based filtering and short-circuit propagation</li>
 *   <li>Providing {@link Subscription} handles for safe unsubscription</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * EventBus bus = EventBus.instance();
 *
 * // Subscribe
 * Subscription sub = bus.subscribe(PlayerDiedEvent.class, event -> {
 *     System.out.println("Player died: " + event.playerName());
 *     return false; // do not stop propagation
 * });
 *
 * // Publish
 * bus.publish(new PlayerDiedEvent("Alice"));
 *
 * // Unsubscribe
 * sub.unsubscribe();
 * }</pre>
 */
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    /**
     * Cache of dispatch order (class hierarchy + interfaces) for each event class.
     */
    private static final ClassValue<Class<?>[]> EVENT_DISPATCH_ORDER_CACHE = new ClassValue<>() {
        @Override
        protected Class<?>[] computeValue(@NonNull Class<?> type) {
            List<Class<?>> list = computeDispatchOrder(type);
            return list.toArray(new Class<?>[0]);
        }
    };
    /**
     * Mapping of event type → list of listener holders.
     */
    private final ConcurrentMap<Class<? extends Event>, CopyOnWriteArrayList<ListenerHolder>> listenersByEventType = new ConcurrentHashMap<>();

    private EventBus() {
    }

    /**
     * Subscribes a listener to a specific event type.
     *
     * @param eventClass The event class to listen for.
     * @param listener   The listener callback.
     * @param <T>        The event type.
     * @return A {@link Subscription} handle for later unsubscription.
     */
    public <T extends Event> Subscription subscribe(Class<T> eventClass, EventListener<? super T> listener) {
        return subscribe(eventClass, listener, e -> true);
    }

    /**
     * Subscribes a listener with a predicate filter.
     * The listener will only receive events for which {@code filter.test(event)} returns true.
     *
     * @param eventClass The event class to listen for.
     * @param listener   The listener callback.
     * @param filter     A filter determining whether to receive a specific event.
     * @param <T>        The event type.
     * @return A {@link Subscription} handle for later unsubscription.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> Subscription subscribe(
        Class<T> eventClass,
        EventListener<? super T> listener,
        Predicate<? super T> filter) {

        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(filter, "filter");

        int id = EventListenerIdGenerator.nextId();
        ListenerHolder holder = new ListenerHolder(id,
            (EventListener<Event>) listener,
            (Predicate<Event>) filter);

        listenersByEventType
            .computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
            .add(holder);

        return new Subscription(this, id, eventClass);
    }

    /**
     * Publishes an event synchronously to all listeners.
     *
     * <p>Listeners are invoked in order of the event class hierarchy (superclasses first),
     * followed by implemented interfaces.
     *
     * @param event The event to publish. If {@code null}, the call is ignored.
     * @return {@code true} if any listener returned {@code true} to stop propagation.
     */
    public boolean publish(final Event event) {
        if (event == null) {
            return false;
        }

        final Class<?>[] dispatchOrder = EVENT_DISPATCH_ORDER_CACHE.get(event.getClass());

        for (Class<?> type : dispatchOrder) {
            var listenerHolders = listenersByEventType.get(type);
            if (listenerHolders == null) {
                continue;
            }

            for (ListenerHolder holder : listenerHolders) {
                try {
                    if (!holder.filter().test(event)) {
                        continue;
                    }
                    if (holder.listener().onEvent(event)) {
                        return true;
                    }
                } catch (Exception ex) {
                    // prefer a logger overload that accepts the throwable if available
                    Log.error("EventBus listener failed for " + event.getClass().getSimpleName() + ". Error: " + ex);
                }
            }
        }
        return false;
    }

    /**
     * Removes a listener by ID.
     * Called internally by {@link Subscription#unsubscribe()}.
     *
     * @param eventClass The event class.
     * @param id         The listener ID.
     */
    void removeListener(final Class<? extends Event> eventClass, final int id) {
        var list = listenersByEventType.get(eventClass);
        if (list == null) {
            return;
        }
        list.removeIf(holder -> holder.id() == id);
        if (list.isEmpty()) {
            listenersByEventType.remove(eventClass, list);
        }
    }

    /**
     * Clears all listeners for a specific event type.
     *
     * @param eventClass The event class whose listeners should be removed.
     */
    public void clearListenersFor(final Class<? extends Event> eventClass) {
        listenersByEventType.remove(eventClass);
    }

    /**
     * Clears all listeners for all event types.
     */
    public void clearAllListeners() {
        listenersByEventType.clear();
    }

    public static EventBus instance() {
        return INSTANCE;
    }

    /**
     * Computes the full dispatch order (superclasses + interfaces)
     * for an event class. The order is breadth-first across interfaces.
     *
     * @param start The starting class (event type).
     * @return An ordered list of types for dispatch.
     */
    private static List<Class<?>> computeDispatchOrder(final Class<?> start) {
        final LinkedHashSet<Class<?>> ordered = new LinkedHashSet<>();
        Class<?> current = start;
        while (current != null && current != Object.class) {
            ordered.add(current);
            current = current.getSuperclass();
        }
        final Queue<Class<?>> queue = new ArrayDeque<>(ordered);
        while (!queue.isEmpty()) {
            final Class<?> cls = queue.poll();
            for (Class<?> iface : cls.getInterfaces()) {
                if (ordered.add(iface)) {
                    queue.add(iface);
                }
            }
        }
        return new ArrayList<>(ordered);
    }
}
