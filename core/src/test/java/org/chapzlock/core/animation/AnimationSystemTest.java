package org.chapzlock.core.animation;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimationSystemTest {

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
    void basicTranslationPlayback() {
        // Channel moves from (0,0,0) at t=0 to (10,20,30) at t=1
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        Channel channel = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 20f, 30f});
        Animation anim = new Animation("move", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        registry.addComponent(entity, state);

        system.onUpdate(0.5f);

        assertEquals(5f, transform.getPosition().x, 0.001f);
        assertEquals(10f, transform.getPosition().y, 0.001f);
        assertEquals(15f, transform.getPosition().z, 0.001f);
    }

    @Test
    void pausedAnimationDoesNotAdvance() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        Channel channel = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 20f, 30f});
        Animation anim = new Animation("move", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        state.pause();
        registry.addComponent(entity, state);

        system.onUpdate(0.5f);

        assertEquals(0f, transform.getPosition().x, 0.001f);
        assertEquals(0f, transform.getPosition().y, 0.001f);
        assertEquals(0f, transform.getPosition().z, 0.001f);
    }

    @Test
    void loopingWrapsTimeAroundDuration() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        // Two keyframes: t=0 -> (0,0,0), t=2 -> (20,0,0)
        Channel channel = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 2f},
            new float[]{0f, 0f, 0f, 20f, 0f, 0f});
        Animation anim = new Animation("loop", 2f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        state.setLooping(true);
        state.setTime(1.5f);
        registry.addComponent(entity, state);

        // Advance by 1.0s: time goes from 1.5 to 2.5, wraps to 0.5
        system.onUpdate(1.0f);

        assertEquals(0.5f, state.getTime(), 0.001f);
        // At t=0.5 out of 2.0 duration -> 25% through -> x=5
        assertEquals(5f, transform.getPosition().x, 0.001f);
    }

    @Test
    void nonLoopingClampsTimeToDuration() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        Channel channel = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 0f, 0f});
        Animation anim = new Animation("once", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        state.setLooping(false);
        state.setTime(0.8f);
        registry.addComponent(entity, state);

        // Advance by 0.5s: time would be 1.3, clamped to 1.0
        system.onUpdate(0.5f);

        assertEquals(1.0f, state.getTime(), 0.001f);
        // At t=1.0 (last keyframe) -> x=10
        assertEquals(10f, transform.getPosition().x, 0.001f);
    }

    @Test
    void rotationChannelWritesQuaternion() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        // Rotate from identity to 90-degree Y rotation over 1 second
        Quaternionf start = new Quaternionf().identity();
        Quaternionf end = new Quaternionf().rotationY((float) Math.toRadians(90));

        Channel channel = new Channel(target, TargetPath.ROTATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{start.x, start.y, start.z, start.w, end.x, end.y, end.z, end.w});
        Animation anim = new Animation("rotate", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        registry.addComponent(entity, state);

        system.onUpdate(0.5f);

        // At midpoint, should be roughly 45-degree Y rotation
        Quaternionf expected = new Quaternionf().rotationY((float) Math.toRadians(45));
        Quaternionf actual = transform.getRotationQuaternion();
        assertEquals(expected.x, actual.x, 0.01f);
        assertEquals(expected.y, actual.y, 0.01f);
        assertEquals(expected.z, actual.z, 0.01f);
        assertEquals(expected.w, actual.w, 0.01f);
    }

    @Test
    void scaleChannelWritesUniformScaleFromXComponent() {
        int target = Entity.create(registry);
        Transform transform = new Transform();
        registry.addComponent(target, transform);

        // Scale from (1,1,1) to (3,3,3) over 1 second
        Channel channel = new Channel(target, TargetPath.SCALE, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{1f, 1f, 1f, 3f, 3f, 3f});
        Animation anim = new Animation("grow", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        registry.addComponent(entity, state);

        system.onUpdate(0.5f);

        // Midpoint: scale x = lerp(1, 3, 0.5) = 2.0
        assertEquals(2f, transform.getScale(), 0.001f);
    }

    @Test
    void nullAnimationDoesNotCrash() {
        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(null);
        registry.addComponent(entity, state);

        assertDoesNotThrow(() -> system.onUpdate(0.5f));
    }

    @Test
    void missingTargetTransformDoesNotCrash() {
        // Target entity exists but has no Transform component
        int target = Entity.create(registry);

        Channel channel = new Channel(target, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f},
            new float[]{0f, 0f, 0f, 10f, 20f, 30f});
        Animation anim = new Animation("move", 1f, List.of(channel));

        int entity = Entity.create(registry);
        AnimationState state = new AnimationState(anim);
        registry.addComponent(entity, state);

        assertDoesNotThrow(() -> system.onUpdate(0.5f));
    }
}
