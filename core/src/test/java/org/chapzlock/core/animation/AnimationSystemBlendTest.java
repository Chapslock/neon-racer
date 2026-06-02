package org.chapzlock.core.animation;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimationSystemBlendTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private AnimationSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        system = new AnimationSystem();
        system.onInit(ctx);
    }

    @Test
    void crossfadeLerpsBetweenTwoClips() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        // Clip A: holds position at (0, 0, 0) over 2 seconds
        Channel chA = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 2f}, new float[]{0, 0, 0, 0, 0, 0});
        Animation clipA = new Animation("A", 2f, List.of(chA));

        // Clip B: moves from (10, 10, 10) to (20, 20, 20) over 2 seconds
        Channel chB = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 2f}, new float[]{10, 10, 10, 20, 20, 20});
        Animation clipB = new Animation("B", 2f, List.of(chB));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(clipA);
        state.setTime(1.0f); // Midway through A
        registry.addComponent(entity, state);

        // Switch to clip B with 1-second crossfade
        state.play(clipB, 1.0f);

        // Advance 0.5s: blend factor should be 0.5
        // Previous (clipA at t=1.5): (0,0,0) (stays at 0 since both keyframes are 0)
        // Current (clipB at t=0.5): lerp(10,20, 0.25) = 12.5
        // Blended: lerp(0, 12.5, 0.5) = 6.25
        system.onUpdate(0.5f);

        assertEquals(6.25f, transform.getX(), 0.1f);
        assertEquals(6.25f, transform.getY(), 0.1f);
        assertEquals(6.25f, transform.getZ(), 0.1f);
        assertTrue(state.isBlending());
    }

    @Test
    void crossfadeCompletesAfterBlendDuration() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        Channel chA = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{0, 0, 0, 0, 0, 0});
        Animation clipA = new Animation("A", 1f, List.of(chA));

        Channel chB = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{10, 10, 10, 10, 10, 10});
        Animation clipB = new Animation("B", 1f, List.of(chB));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(clipA);
        registry.addComponent(entity, state);

        state.play(clipB, 0.2f);

        // Advance past blend duration
        system.onUpdate(0.3f);

        assertFalse(state.isBlending());
        assertNull(state.getPreviousAnimation());
        // Should now be purely clip B: (10,10,10)
        assertEquals(10f, transform.getX(), 0.1f);
    }
}
