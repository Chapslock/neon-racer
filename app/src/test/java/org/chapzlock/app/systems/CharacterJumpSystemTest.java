package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.physics.PhysicsSystem;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;

class CharacterJumpSystemTest {

    private static final float JUMP_IMPULSE = 5f;
    private static final float EPSILON = 0.001f;

    private EngineContext ctx;
    private ComponentRegistry registry;
    private EventBus eventBus;
    private PhysicsSystem physicsSystem;
    private Physics physics;
    private CharacterJumpSystem system;
    private InputMap inputMap;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        eventBus = ctx.getEventBus();

        physicsSystem = new PhysicsSystem();
        physicsSystem.onInit(ctx);
        physics = ctx.getPhysics();

        // Build and activate InputMap with "jump" action bound to SPACE
        inputMap = InputMap.builder()
            .action("jump", Key.SPACE)
            .build();
        inputMap.activate(eventBus);
        ctx.setActiveInputMap(inputMap);

        system = new CharacterJumpSystem();
        system.onInit(ctx);
    }

    @Test
    void jump_appliesUpwardImpulse_whenGroundedAndJustPressed() {
        int player = createPlayer(0, 1, 0, true);

        // Register physics body in the world
        physicsSystem.onUpdate(0.016f);

        // Zero out velocity after physics step (gravity may have been applied)
        physics.setLinearVelocity(player, new Vector3f(0, 0, 0));

        // Simulate SPACE just pressed
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(JUMP_IMPULSE, vel.y, EPSILON,
            "Y velocity should equal JUMP_IMPULSE after jump");
    }

    @Test
    void jump_doesNothing_whenNotGrounded() {
        int player = createPlayer(0, 1, 0, false);

        // Register physics body in the world
        physicsSystem.onUpdate(0.016f);

        // Zero out velocity after physics step (gravity may have been applied)
        physics.setLinearVelocity(player, new Vector3f(0, 0, 0));

        // Simulate SPACE just pressed
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(0f, vel.y, EPSILON,
            "Y velocity should remain 0 when not grounded");
    }

    @Test
    void jump_setsGroundedFalse_immediately() {
        int player = createPlayer(0, 1, 0, true);

        // Register physics body in the world
        physicsSystem.onUpdate(0.016f);

        // Simulate SPACE just pressed
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));

        system.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);
        assertFalse(state.isGrounded(),
            "Grounded should be false immediately after jump");
    }

    @Test
    void jump_doesNotRepeatOnHold() {
        int player = createPlayer(0, 1, 0, true);

        // Register physics body in the world
        physicsSystem.onUpdate(0.016f);

        // Simulate SPACE just pressed on first frame
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_SPACE, 0));
        system.onUpdate(0.016f);

        // Record velocity after first jump
        Vector3f velAfterJump = physics.getLinearVelocity(player);
        float yAfterFirstJump = velAfterJump.y;

        // Reset frame state (clears justPressed) — simulates next frame
        inputMap.frameReset();

        // Re-set grounded to true to isolate the wasJustPressed check
        CharacterState state = registry.getComponent(player, CharacterState.class);
        state.setGrounded(true);

        // Reset velocity to zero to clearly see if second jump applies impulse
        physics.setLinearVelocity(player, new Vector3f(0, 0, 0));

        // On second frame, key is still held but wasJustPressed should be false
        system.onUpdate(0.016f);

        Vector3f velSecondFrame = physics.getLinearVelocity(player);
        assertNotNull(velSecondFrame);
        assertEquals(0f, velSecondFrame.y, EPSILON,
            "Jump should not apply impulse when key is held (not just pressed)");
    }

    @Test
    void jump_doesNothing_whenNoInputMap() {
        int player = createPlayer(0, 1, 0, true);

        // Register physics body in the world
        physicsSystem.onUpdate(0.016f);

        // Zero out velocity after physics step (gravity may have been applied)
        physics.setLinearVelocity(player, new Vector3f(0, 0, 0));

        // Remove active input map
        ctx.setActiveInputMap(null);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(0f, vel.y, EPSILON,
            "Y velocity should remain 0 when no InputMap is active");
    }

    // --- Helpers ---

    private int createPlayer(float x, float y, float z, boolean grounded) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(x, y, z));
        CharacterState state = new CharacterState();
        state.setGrounded(grounded);
        registry.addComponent(entity, state);
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.capsule(0.3f, 0.6f))
            .build());
        return entity;
    }
}
