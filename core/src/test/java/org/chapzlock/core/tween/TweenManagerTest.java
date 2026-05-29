package org.chapzlock.core.tween;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TweenManagerTest {

    private TweenManager manager;

    @BeforeEach
    void setUp() {
        manager = new TweenManager();
    }

    @Test
    void createAddsTweenToActiveList() {
        manager.create(1.0f, Ease.LINEAR, t -> {});
        assertEquals(1, manager.getActiveCount());
    }

    @Test
    void tweenProgressesOverTime() {
        List<Float> values = new ArrayList<>();
        manager.create(1.0f, Ease.LINEAR, values::add);

        manager.update(0.5f);

        assertEquals(1, values.size());
        assertEquals(0.5f, values.get(0), 0.0001f);
    }

    @Test
    void tweenCompletesAtDuration() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        manager.create(1.0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        manager.update(1.0f);

        assertEquals(1.0f, values.get(values.size() - 1), 0.0001f);
        assertTrue(completed.get());
        assertEquals(0, manager.getActiveCount());
    }

    @Test
    void tweenCompletesWhenOvershootingDuration() {
        List<Float> values = new ArrayList<>();
        manager.create(0.5f, Ease.LINEAR, values::add);

        manager.update(1.0f);

        assertEquals(1.0f, values.get(values.size() - 1), 0.0001f);
        assertEquals(0, manager.getActiveCount());
    }

    @Test
    void tweenAppliesEasing() {
        List<Float> values = new ArrayList<>();
        manager.create(1.0f, Ease.QUAD_IN, values::add);

        manager.update(0.5f);

        assertEquals(0.25f, values.get(0), 0.0001f);
    }

    @Test
    void cancelPreventsFurtherUpdatesAndOnComplete() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Tween tween = manager.create(1.0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        manager.update(0.25f);
        tween.cancel();
        manager.update(0.75f);

        assertEquals(1, values.size());
        assertFalse(completed.get());
        assertEquals(0, manager.getActiveCount());
    }

    @Test
    void zeroDurationTweenCompletesImmediately() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        manager.create(0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        manager.update(0.016f);

        assertEquals(1.0f, values.get(0), 0.0001f);
        assertTrue(completed.get());
        assertEquals(0, manager.getActiveCount());
    }

    @Test
    void zeroDeltaDoesNotAdvanceTween() {
        List<Float> values = new ArrayList<>();
        manager.create(1.0f, Ease.LINEAR, values::add);

        manager.update(0f);

        assertEquals(1, values.size());
        assertEquals(0f, values.get(0), 0.0001f);
    }

    @Test
    void multipleTweensUpdateIndependently() {
        List<Float> valuesA = new ArrayList<>();
        List<Float> valuesB = new ArrayList<>();

        manager.create(1.0f, Ease.LINEAR, valuesA::add);
        manager.create(2.0f, Ease.LINEAR, valuesB::add);

        manager.update(1.0f);

        assertEquals(1.0f, valuesA.get(valuesA.size() - 1), 0.0001f);
        assertEquals(0.5f, valuesB.get(valuesB.size() - 1), 0.0001f);
        assertEquals(1, manager.getActiveCount());
    }

    @Test
    void tweenStateReflectsLifecycle() {
        Tween tween = manager.create(1.0f, Ease.LINEAR, t -> {});

        assertTrue(tween.isActive());
        assertFalse(tween.isComplete());

        manager.update(1.0f);

        assertFalse(tween.isActive());
        assertTrue(tween.isComplete());
    }
}
