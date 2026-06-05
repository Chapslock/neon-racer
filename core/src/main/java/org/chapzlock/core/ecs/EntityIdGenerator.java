package org.chapzlock.core.ecs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates Id-s for entities for the entire lifecycle of the application
 * Guarantees thread safe ID generation.
 */
public class EntityIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Returns the next unique entity ID.
     *
     * <p>Thread-safe — uses an atomic counter internally. IDs start at 1 and increment
     * monotonically. IDs are never reused, even after entity destruction.
     */
    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
