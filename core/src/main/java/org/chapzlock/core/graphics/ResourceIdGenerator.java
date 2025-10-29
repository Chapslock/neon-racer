package org.chapzlock.core.graphics;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

/**
 * Thread safe id generator for GPU resources.
 */
@UtilityClass
public class ResourceIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
