package org.chapzlock.core.entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates Id-s for entities for the entire lifecycle of the application
 * Guarantees thread safe ID generation.
 */
public class EntityIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
