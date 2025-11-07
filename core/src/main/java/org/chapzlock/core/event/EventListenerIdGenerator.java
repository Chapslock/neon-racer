package org.chapzlock.core.event;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

/**
 * Generates ID-s for event listener handlers
 */
@UtilityClass
class EventListenerIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
