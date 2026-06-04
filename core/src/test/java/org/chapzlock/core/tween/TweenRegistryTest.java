package org.chapzlock.core.tween;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TweenRegistryTest {

    private TweenRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TweenRegistry();
    }

    @Test
    void createAddsTweenToActiveList() {
        registry.create(1.0f, Ease.LINEAR, t -> {});
        assertEquals(1, registry.getActiveCount());
    }

    @Test
    void tweenProgressesOverTime() {
        List<Float> values = new ArrayList<>();
        registry.create(1.0f, Ease.LINEAR, values::add);

        registry.update(0.5f);

        assertEquals(1, values.size());
        assertEquals(0.5f, values.get(0), 0.0001f);
    }

    @Test
    void tweenCompletesAtDuration() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        registry.create(1.0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        registry.update(1.0f);

        assertEquals(1.0f, values.get(values.size() - 1), 0.0001f);
        assertTrue(completed.get());
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void tweenCompletesWhenOvershootingDuration() {
        List<Float> values = new ArrayList<>();
        registry.create(0.5f, Ease.LINEAR, values::add);

        registry.update(1.0f);

        assertEquals(1.0f, values.get(values.size() - 1), 0.0001f);
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void tweenAppliesEasing() {
        List<Float> values = new ArrayList<>();
        registry.create(1.0f, Ease.QUAD_IN, values::add);

        registry.update(0.5f);

        assertEquals(0.25f, values.get(0), 0.0001f);
    }

    @Test
    void cancelPreventsFurtherUpdatesAndOnComplete() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Tween tween = registry.create(1.0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        registry.update(0.25f);
        tween.cancel();
        registry.update(0.75f);

        assertEquals(1, values.size());
        assertFalse(completed.get());
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void zeroDurationTweenCompletesImmediately() {
        List<Float> values = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        registry.create(0f, Ease.LINEAR, values::add)
            .onComplete(() -> completed.set(true));

        registry.update(0.016f);

        assertEquals(1.0f, values.get(0), 0.0001f);
        assertTrue(completed.get());
        assertEquals(0, registry.getActiveCount());
    }

    @Test
    void zeroDeltaDoesNotAdvanceTween() {
        List<Float> values = new ArrayList<>();
        registry.create(1.0f, Ease.LINEAR, values::add);

        registry.update(0f);

        assertEquals(1, values.size());
        assertEquals(0f, values.get(0), 0.0001f);
    }

    @Test
    void multipleTweensUpdateIndependently() {
        List<Float> valuesA = new ArrayList<>();
        List<Float> valuesB = new ArrayList<>();

        registry.create(1.0f, Ease.LINEAR, valuesA::add);
        registry.create(2.0f, Ease.LINEAR, valuesB::add);

        registry.update(1.0f);

        assertEquals(1.0f, valuesA.get(valuesA.size() - 1), 0.0001f);
        assertEquals(0.5f, valuesB.get(valuesB.size() - 1), 0.0001f);
        assertEquals(1, registry.getActiveCount());
    }

    @Test
    void tweenStateReflectsLifecycle() {
        Tween tween = registry.create(1.0f, Ease.LINEAR, t -> {});

        assertTrue(tween.isActive());
        assertFalse(tween.isComplete());

        registry.update(1.0f);

        assertFalse(tween.isActive());
        assertTrue(tween.isComplete());
    }
}
