package org.chapzlock.app.systems;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Channel;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.AnimationClipSet;
import org.chapzlock.core.component.AnimationState;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAnimationSystemTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private PlayerAnimationSystem system;

    private Animation idleAnim;
    private Animation runAnim;
    private Animation walkAnim;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        system = new PlayerAnimationSystem();
        system.onInit(ctx);

        Channel dummyChannel = new Channel(0, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{0, 0, 0, 0, 0, 0});
        idleAnim = new Animation("Idle", 1f, List.of(dummyChannel));
        runAnim = new Animation("Run", 1f, List.of(dummyChannel));
        walkAnim = new Animation("Walk", 1f, List.of(dummyChannel));
    }

    private int createPlayerEntity(float moveForward, float moveRight) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());

        PlayerInputComponent input = new PlayerInputComponent();
        input.setMoveForward(moveForward);
        input.setMoveRight(moveRight);
        registry.addComponent(entity, input);

        AnimationClipSet clips = new AnimationClipSet(Map.of(
            "Idle", idleAnim,
            "Run", runAnim,
            "Walk", walkAnim
        ));
        registry.addComponent(entity, clips);

        AnimationState state = new AnimationState(null);
        registry.addComponent(entity, state);

        return entity;
    }

    @Test
    void playsRunAnimationWhenMovingForward() {
        int entity = createPlayerEntity(1.0f, 0f);
        system.onUpdate(0.016f);

        AnimationState state = registry.getComponent(entity, AnimationState.class);
        assertEquals(runAnim, state.getCurrentAnimation());
        assertTrue(state.isPlaying());
        assertTrue(state.isLooping());
    }

    @Test
    void playsIdleAnimationWhenStationary() {
        int entity = createPlayerEntity(0f, 0f);
        system.onUpdate(0.016f);

        AnimationState state = registry.getComponent(entity, AnimationState.class);
        assertEquals(idleAnim, state.getCurrentAnimation());
        assertTrue(state.isPlaying());
        assertTrue(state.isLooping());
    }

    @Test
    void playsRunAnimationWhenMovingSideways() {
        int entity = createPlayerEntity(0f, 1.0f);
        system.onUpdate(0.016f);

        AnimationState state = registry.getComponent(entity, AnimationState.class);
        assertEquals(runAnim, state.getCurrentAnimation());
    }

    @Test
    void doesNotRestartAnimationIfAlreadyPlaying() {
        int entity = createPlayerEntity(1.0f, 0f);

        // First frame: starts Run
        system.onUpdate(0.016f);

        AnimationState state = registry.getComponent(entity, AnimationState.class);
        // Simulate some time passing
        state.setTime(0.5f);

        // Second frame: still moving, should not reset time
        system.onUpdate(0.016f);

        assertEquals(runAnim, state.getCurrentAnimation());
        assertEquals(0.5f, state.getTime(), 0.001f);
    }

    @Test
    void crossfadesWhenSwitchingFromRunToIdle() {
        int entity = createPlayerEntity(1.0f, 0f);
        system.onUpdate(0.016f);

        AnimationState state = registry.getComponent(entity, AnimationState.class);
        state.setTime(0.5f);

        // Stop moving
        PlayerInputComponent input = registry.getComponent(entity, PlayerInputComponent.class);
        input.setMoveForward(0f);
        system.onUpdate(0.016f);

        assertEquals(idleAnim, state.getCurrentAnimation());
        assertTrue(state.isBlending());
        assertEquals(runAnim, state.getPreviousAnimation());
    }
}
