package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterRotationSystemTest {

    private static final float EPSILON = 0.01f;
    private static final float DELTA_TIME = 0.016f;

    private EngineContext ctx;
    private ComponentRegistry registry;
    private CharacterRotationSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();

        system = new CharacterRotationSystem();
        system.onInit(ctx);
    }

    @Test
    void noMovement_rotationUnchanged() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);

        // Set an initial rotation (45 degrees around Y)
        Quaternionf initial = new Quaternionf().rotationY((float) Math.toRadians(45));
        transform.setRotationQuaternion(initial);

        // Movement direction is zero (default)
        system.onUpdate(DELTA_TIME);

        Quaternionf after = new Quaternionf();
        transform.getRotationQuaternion(after);

        assertEquals(initial.x, after.x, EPSILON, "X should be unchanged with no movement");
        assertEquals(initial.y, after.y, EPSILON, "Y should be unchanged with no movement");
        assertEquals(initial.z, after.z, EPSILON, "Z should be unchanged with no movement");
        assertEquals(initial.w, after.w, EPSILON, "W should be unchanged with no movement");
    }

    @Test
    void movingInDirection_rotationChangesOverMultipleFrames() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        // Start facing forward (identity = looking down -Z)
        Quaternionf identity = new Quaternionf();
        transform.setRotationQuaternion(identity);

        // Set movement direction to +X
        state.getMovementDirection().set(1f, 0f, 0f);

        // Run one frame - should start rotating but not be at target yet
        system.onUpdate(DELTA_TIME);

        Quaternionf afterOneFrame = new Quaternionf();
        transform.getRotationQuaternion(afterOneFrame);

        // Target is 90 degrees around Y for +X direction
        Quaternionf target = new Quaternionf().rotationY((float) Math.toRadians(90));

        // After one frame, should have moved toward target but not reached it
        assertNotEquals(identity.y, afterOneFrame.y, EPSILON,
            "Rotation should change after one frame");

        // Run more frames
        system.onUpdate(DELTA_TIME);
        Quaternionf afterTwoFrames = new Quaternionf();
        transform.getRotationQuaternion(afterTwoFrames);

        // Should be closer to target after two frames than after one
        float distOneFrame = Math.abs(target.y - afterOneFrame.y);
        float distTwoFrames = Math.abs(target.y - afterTwoFrames.y);
        assertTrue(distTwoFrames < distOneFrame,
            "Should be closer to target after more frames (slerp convergence)");
    }

    @Test
    void movingInPositiveX_facesPositiveXAfterEnoughFrames() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        // Start facing forward (identity = looking down -Z)
        transform.setRotationQuaternion(new Quaternionf());

        // Set movement direction to +X
        state.getMovementDirection().set(1f, 0f, 0f);

        // Run many frames to let slerp converge
        for (int i = 0; i < 200; i++) {
            system.onUpdate(DELTA_TIME);
        }

        // Target yaw for +X direction: atan2(1, 0) = 90 degrees
        Quaternionf result = new Quaternionf();
        transform.getRotationQuaternion(result);

        Quaternionf expected = new Quaternionf().rotationY((float) Math.toRadians(90));

        assertEquals(expected.x, result.x, EPSILON, "Should face +X (quaternion X)");
        assertEquals(expected.y, result.y, EPSILON, "Should face +X (quaternion Y)");
        assertEquals(expected.z, result.z, EPSILON, "Should face +X (quaternion Z)");
        assertEquals(expected.w, result.w, EPSILON, "Should face +X (quaternion W)");
    }

    @Test
    void movingInNegativeZ_facesNegativeZ() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        // Start facing +X (90 degrees)
        transform.setRotationQuaternion(new Quaternionf().rotationY((float) Math.toRadians(90)));

        // Set movement direction to -Z (forward)
        state.getMovementDirection().set(0f, 0f, -1f);

        // Run many frames to let slerp converge
        for (int i = 0; i < 200; i++) {
            system.onUpdate(DELTA_TIME);
        }

        // Target yaw for -Z direction: atan2(0, 1) = 0 degrees (identity rotation)
        Quaternionf result = new Quaternionf();
        transform.getRotationQuaternion(result);

        Quaternionf expected = new Quaternionf().rotationY(0f);

        assertEquals(expected.x, result.x, EPSILON, "Should face -Z (quaternion X)");
        assertEquals(expected.y, result.y, EPSILON, "Should face -Z (quaternion Y)");
        assertEquals(expected.z, result.z, EPSILON, "Should face -Z (quaternion Z)");
        assertEquals(expected.w, result.w, EPSILON, "Should face -Z (quaternion W)");
    }

    // --- Helpers ---

    private int createPlayer() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new CharacterState());
        return entity;
    }
}
