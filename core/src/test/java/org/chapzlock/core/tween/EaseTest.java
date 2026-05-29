package org.chapzlock.core.tween;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class EaseTest {

    @ParameterizedTest
    @EnumSource(Ease.class)
    void allEasingsReturnZeroAtStart(Ease ease) {
        assertEquals(0f, ease.apply(0f), 0.0001f);
    }

    @ParameterizedTest
    @EnumSource(Ease.class)
    void allEasingsReturnOneAtEnd(Ease ease) {
        assertEquals(1f, ease.apply(1f), 0.0001f);
    }

    @Test
    void linearIsIdentity() {
        assertEquals(0.5f, Ease.LINEAR.apply(0.5f), 0.0001f);
        assertEquals(0.25f, Ease.LINEAR.apply(0.25f), 0.0001f);
    }

    @Test
    void quadInStartsSlow() {
        float midpoint = Ease.QUAD_IN.apply(0.5f);
        assertTrue(midpoint < 0.5f, "QUAD_IN at 0.5 should be less than 0.5, was " + midpoint);
    }

    @Test
    void quadOutStartsFast() {
        float midpoint = Ease.QUAD_OUT.apply(0.5f);
        assertTrue(midpoint > 0.5f, "QUAD_OUT at 0.5 should be greater than 0.5, was " + midpoint);
    }

    @ParameterizedTest
    @EnumSource(Ease.class)
    void allEasingsAreMonotonicallyIncreasing(Ease ease) {
        float prev = 0f;
        for (int i = 1; i <= 100; i++) {
            float t = i / 100f;
            float val = ease.apply(t);
            assertTrue(val >= prev - 0.0001f,
                ease + " is not monotonically increasing at t=" + t + ": " + val + " < " + prev);
            prev = val;
        }
    }
}
