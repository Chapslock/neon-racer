package org.chapzlock.core.asset;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

@UtilityClass
class ResourceIdGenerator {
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public static int nextId() {
        return sequence.incrementAndGet();
    }
}
