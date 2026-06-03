package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.physics.Physics;
import org.chapzlock.core.physics.PhysicsSystem;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterMovementSystemTest {

    private static final float MOVE_SPEED = 8f;
    private static final float EPSILON = 0.001f;

    private EngineContext ctx;
    private ComponentRegistry registry;
    private PhysicsSystem physicsSystem;
    private Physics physics;
    private CharacterMovementSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();

        physicsSystem = new PhysicsSystem();
        physicsSystem.onInit(ctx);
        physics = ctx.getPhysics();

        system = new CharacterMovementSystem();
        system.onInit(ctx);
    }

    @Test
    void noInput_setsXZVelocityToZero_preservesY() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        // Register physics body
        physicsSystem.onUpdate(0.016f);

        // Give the player some existing Y velocity (simulating gravity)
        physics.setLinearVelocity(player, new Vector3f(5f, -3f, 5f));

        // Player input is all zeros (default)
        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(0f, vel.x, EPSILON, "X velocity should be zero with no input");
        assertEquals(-3f, vel.y, EPSILON, "Y velocity should be preserved");
        assertEquals(0f, vel.z, EPSILON, "Z velocity should be zero with no input");
    }

    @Test
    void forwardInput_movesInNegativeZ_whenCameraYawIsZero() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        physicsSystem.onUpdate(0.016f);

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(0f, vel.x, EPSILON, "X should be zero for pure forward");
        assertEquals(-MOVE_SPEED, vel.z, EPSILON, "Z should be -MOVE_SPEED for forward with yaw=0");
    }

    @Test
    void diagonalInput_speedIsNormalized() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        physicsSystem.onUpdate(0.016f);

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);
        input.setMoveRight(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        float horizontalSpeed = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        assertEquals(MOVE_SPEED, horizontalSpeed, EPSILON,
            "Diagonal speed should be normalized to MOVE_SPEED, not MOVE_SPEED * sqrt(2)");
    }

    @Test
    void movement_preservesYVelocity() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        physicsSystem.onUpdate(0.016f);

        // Simulate gravity pulling player down
        physics.setLinearVelocity(player, new Vector3f(0f, -9.8f, 0f));

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        assertEquals(-9.8f, vel.y, EPSILON, "Y velocity (gravity) should be preserved during movement");
    }

    @Test
    void movement_writesDirectionToCharacterState() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        physicsSystem.onUpdate(0.016f);

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);
        Vector3f dir = state.getMovementDirection();
        assertEquals(0f, dir.x, EPSILON, "Direction X should be 0 for pure forward with yaw=0");
        assertEquals(0f, dir.y, EPSILON, "Direction Y should always be 0");
        assertEquals(-1f, dir.z, EPSILON, "Direction Z should be -1 for forward with yaw=0");
        assertEquals(MOVE_SPEED, state.getCurrentSpeed(), EPSILON, "Speed should be MOVE_SPEED");
    }

    @Test
    void noInput_clearsDirectionAndSpeed() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(0f);

        physicsSystem.onUpdate(0.016f);

        // First move so state has values
        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);
        system.onUpdate(0.016f);

        // Now stop
        input.setMoveForward(0f);
        system.onUpdate(0.016f);

        CharacterState state = registry.getComponent(player, CharacterState.class);
        assertEquals(0f, state.getMovementDirection().x, EPSILON);
        assertEquals(0f, state.getMovementDirection().y, EPSILON);
        assertEquals(0f, state.getMovementDirection().z, EPSILON);
        assertEquals(0f, state.getCurrentSpeed(), EPSILON);
    }

    @Test
    void cameraYaw90_forwardMovesInNegativeX() {
        int player = createPlayer(0, 1, 0);
        createCameraSingleton(90f);

        physicsSystem.onUpdate(0.016f);

        PlayerInputComponent input = registry.getComponent(player, PlayerInputComponent.class);
        input.setMoveForward(1f);

        system.onUpdate(0.016f);

        Vector3f vel = physics.getLinearVelocity(player);
        assertNotNull(vel);
        // yaw=90: camera forward = (-sin(90), 0, -cos(90)) = (-1, 0, 0)
        assertEquals(-MOVE_SPEED, vel.x, EPSILON, "X should be -MOVE_SPEED when yaw=90 and moving forward");
        assertEquals(0f, vel.z, EPSILON, "Z should be ~0 when yaw=90 and moving forward");
    }

    // --- Helpers ---

    private int createPlayer(float x, float y, float z) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, new CharacterState());
        registry.addComponent(entity, new PlayerInputComponent());
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.capsule(0.3f, 0.6f))
            .build());
        return entity;
    }

    private void createCameraSingleton(float yaw) {
        int camEntity = Entity.create(registry);
        ThirdPersonCamera cam = new ThirdPersonCamera();
        cam.setYaw(yaw);
        registry.addComponent(camEntity, cam);
    }
}
