package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.physics.PhysicsSystem;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterMovementSystemTest {

    private Engine engine;
    private ComponentRegistry registry;
    private Physics physics;
    private PhysicsSystem physicsSystem;
    private CharacterMovementSystem system;
    private int player;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();

        physicsSystem = new PhysicsSystem();
        physicsSystem.onInit(engine);
        physics = engine.getPhysics();

        player = createPlayer();
        physicsSystem.onUpdate(0.016f);

        system = new CharacterMovementSystem();
        system.onInit(engine);
    }

    @Test
    void noInput_velocityXZIsZero() {
        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(0f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertEquals(0f, vel.x, 0.01f);
        assertEquals(0f, vel.z, 0.01f);
    }

    @Test
    void forwardInput_movesInModelForwardDirection() {
        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertTrue(vel.x > 0.1f, "Should move in +X (model forward at identity rotation)");
    }

    @Test
    void forwardInput_whenRotated90_movesInRotatedDirection() {
        Transform transform = registry.getComponent(player, Transform.class);
        transform.setRotationQuaternion(new Quaternionf().rotationY((float) Math.toRadians(90)));

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertTrue(vel.z < -0.1f, "Should move in -Z after 90° Y rotation");
    }

    @Test
    void movement_preservesYVelocity() {
        physics.applyImpulse(player, new Vector3f(0, 10, 0));
        Vector3f velBefore = physics.getLinearVelocity(player);

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);
        system.onUpdate(0.016f);

        Vector3f velAfter = physics.getLinearVelocity(player);
        assertEquals(velBefore.y, velAfter.y, 0.01f);
    }

    @Test
    void movement_writesDirectionToCharacterState() {
        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);
        assertTrue(state.getMovementDirection().length() > 0.9f);
    }

    private int createPlayer() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 5, 0));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.capsule(0.3f, 0.6f))
            .build());
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, new PlayerInputComponent());
        registry.addComponent(entity, new CharacterState());
        return entity;
    }
}
