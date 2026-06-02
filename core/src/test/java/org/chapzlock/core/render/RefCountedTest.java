package org.chapzlock.core.render;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefCountedTest {

    private List<Integer> released;
    private RefCounted refCounted;

    @BeforeEach
    void setUp() {
        released = new ArrayList<>();
        refCounted = new RefCounted(released::add);
    }

    @Test
    void retainIncreasesCount() {
        refCounted.retain(1);
        assertEquals(1, refCounted.getRefCount(1));
        refCounted.retain(1);
        assertEquals(2, refCounted.getRefCount(1));
    }

    @Test
    void releaseDecreasesCount() {
        refCounted.retain(1);
        refCounted.retain(1);
        refCounted.release(1);
        assertEquals(1, refCounted.getRefCount(1));
    }

    @Test
    void releaseToZeroFiresCallback() {
        refCounted.retain(5);
        refCounted.release(5);
        assertEquals(0, refCounted.getRefCount(5));
        assertEquals(List.of(5), released);
    }

    @Test
    void releaseToZeroFiresCallbackOnlyOnce() {
        refCounted.retain(3);
        refCounted.release(3);
        refCounted.release(3);
        assertEquals(List.of(3), released);
    }

    @Test
    void getRefCountReturnsZeroForUnknownId() {
        assertEquals(0, refCounted.getRefCount(999));
    }

    @Test
    void multipleIdsTrackedIndependently() {
        refCounted.retain(1);
        refCounted.retain(2);
        refCounted.retain(2);
        assertEquals(1, refCounted.getRefCount(1));
        assertEquals(2, refCounted.getRefCount(2));
        refCounted.release(1);
        assertEquals(List.of(1), released);
        assertEquals(2, refCounted.getRefCount(2));
    }
}
