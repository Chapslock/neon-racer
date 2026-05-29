package org.chapzlock.core.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeTest {

    private Time time;

    @BeforeEach
    void setUp() {
        time = new Time();
    }

    @Test
    void initialStateIsZeroWithScaleOne() {
        assertEquals(0f, time.getDelta());
        assertEquals(0f, time.getUnscaledDelta());
        assertEquals(0f, time.getTime());
        assertEquals(0f, time.getUnscaledTime());
        assertEquals(1.0f, time.getScale());
        assertFalse(time.isPaused());
    }

    @Test
    void updateSetsDeltaAndAccumulatesTime() {
        time.update(0.016f);

        assertEquals(0.016f, time.getDelta(), 0.0001f);
        assertEquals(0.016f, time.getUnscaledDelta(), 0.0001f);
        assertEquals(0.016f, time.getTime(), 0.0001f);
        assertEquals(0.016f, time.getUnscaledTime(), 0.0001f);
    }

    @Test
    void updateRespectsTimeScale() {
        time.setScale(2.0f);
        time.update(0.016f);

        assertEquals(0.032f, time.getDelta(), 0.0001f);
        assertEquals(0.016f, time.getUnscaledDelta(), 0.0001f);
        assertEquals(0.032f, time.getTime(), 0.0001f);
        assertEquals(0.016f, time.getUnscaledTime(), 0.0001f);
    }

    @Test
    void pauseStopsDeltaButNotUnscaledTime() {
        time.pause();
        assertTrue(time.isPaused());

        time.update(0.016f);

        assertEquals(0f, time.getDelta());
        assertEquals(0.016f, time.getUnscaledDelta(), 0.0001f);
        assertEquals(0f, time.getTime());
        assertEquals(0.016f, time.getUnscaledTime(), 0.0001f);
    }

    @Test
    void resumeRestoresScale() {
        time.pause();
        time.resume();

        assertFalse(time.isPaused());
        assertEquals(1.0f, time.getScale());
    }

    @Test
    void negativeScaleClampedToZero() {
        time.setScale(-1.0f);
        assertEquals(0f, time.getScale());
    }

    @Test
    void multipleUpdatesAccumulate() {
        time.update(0.016f);
        time.update(0.016f);
        time.update(0.016f);

        assertEquals(0.048f, time.getTime(), 0.0001f);
        assertEquals(0.048f, time.getUnscaledTime(), 0.0001f);
    }
}
