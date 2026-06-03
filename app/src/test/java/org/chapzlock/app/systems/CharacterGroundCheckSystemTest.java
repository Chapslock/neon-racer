package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.physics.PhysicsSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterGroundCheckSystemTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private PhysicsSystem physicsSystem;
    private CharacterGroundCheckSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();

        // PhysicsSystem creates PhysicsWorld + Physics internally, sets ctx.physics
        physicsSystem = new PhysicsSystem();
        physicsSystem.onInit(ctx);

        system = new CharacterGroundCheckSystem();
        system.onInit(ctx);
    }

    @Test
    void setsGroundedTrue_whenFloorIsBelowWithinRayLength() {
        // Player at y=1.0, floor box with half-height=0.5 centered at y=0
        // Floor top is at y=0.5, player center at y=1.0
        // Distance from player center down to floor top = 0.5, within ray length (0.65)
        int player = createPlayer(0, 1.0f, 0);
        createStaticFloor(0, 0, 0);

        // Process pending registrations so bodies enter the physics world
        physicsSystem.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);
        assertFalse(state.isGrounded(), "Should start not grounded before ground check runs");

        system.onUpdate(0.016f);

        assertTrue(state.isGrounded(), "Should be grounded when floor is within ray length");
    }

    @Test
    void setsGroundedFalse_whenNoFloorBelow() {
        // Player floating with no floor beneath
        int player = createPlayer(0, 1.0f, 0);

        // Process pending registrations
        physicsSystem.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);

        system.onUpdate(0.016f);

        assertFalse(state.isGrounded(), "Should not be grounded when no floor exists");
    }

    @Test
    void setsGroundedFalse_whenFloorIsFarBelow() {
        // Player at y=10, floor at y=0 — distance from player center to floor top is 9.5
        // Far exceeds ray length of 0.65
        int player = createPlayer(0, 10.0f, 0);
        createStaticFloor(0, 0, 0);

        // Process pending registrations
        physicsSystem.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);

        system.onUpdate(0.016f);

        assertFalse(state.isGrounded(), "Should not be grounded when floor is far below");
    }

    // --- Helpers ---

    private int createPlayer(float x, float y, float z) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, new CharacterState());
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.capsule(0.3f, 0.6f))
            .build());
        return entity;
    }

    private int createStaticFloor(float x, float y, float z) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(0f)
            .geometry(CollisionGeometry.box(50f, 0.5f, 50f))
            .build());
        return entity;
    }
}
