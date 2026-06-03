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
    private static final float MODEL_YAW_OFFSET = -90f;

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

        Quaternionf initial = new Quaternionf().rotationY((float) Math.toRadians(45));
        transform.setRotationQuaternion(initial);

        system.onUpdate(DELTA_TIME);

        Quaternionf after = new Quaternionf();
        transform.getRotationQuaternion(after);

        assertEquals(initial.x, after.x, EPSILON);
        assertEquals(initial.y, after.y, EPSILON);
        assertEquals(initial.z, after.z, EPSILON);
        assertEquals(initial.w, after.w, EPSILON);
    }

    @Test
    void movingInDirection_rotationChanges() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        // Start at identity
        transform.setRotationQuaternion(new Quaternionf());

        // Move in +X — target yaw = atan2(1,0)=90° + offset(-90°) = 0° (identity)
        // Start from a different rotation so we can detect change
        transform.setRotationQuaternion(new Quaternionf().rotationY((float) Math.toRadians(45)));
        state.getMovementDirection().set(1f, 0f, 0f);

        system.onUpdate(DELTA_TIME);

        Quaternionf after = new Quaternionf();
        transform.getRotationQuaternion(after);

        // Should have moved away from 45° toward 0°
        Quaternionf fortyFive = new Quaternionf().rotationY((float) Math.toRadians(45));
        assertNotEquals(fortyFive.y, after.y, EPSILON, "Rotation should change");
    }

    @Test
    void movingInPositiveX_converges() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        transform.setRotationQuaternion(new Quaternionf().rotationY((float) Math.toRadians(90)));
        state.getMovementDirection().set(1f, 0f, 0f);

        for (int i = 0; i < 200; i++) {
            system.onUpdate(DELTA_TIME);
        }

        // +X direction: atan2(1, 0) = 90° + MODEL_YAW_OFFSET(-90°) = 0°
        Quaternionf result = new Quaternionf();
        transform.getRotationQuaternion(result);
        Quaternionf expected = new Quaternionf().rotationY(0f);

        assertEquals(expected.y, result.y, EPSILON);
        assertEquals(expected.w, result.w, EPSILON);
    }

    @Test
    void movingInNegativeZ_converges() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        transform.setRotationQuaternion(new Quaternionf());
        state.getMovementDirection().set(0f, 0f, -1f);

        for (int i = 0; i < 200; i++) {
            system.onUpdate(DELTA_TIME);
        }

        // -Z direction: atan2(0, 1) = 0° + MODEL_YAW_OFFSET(-90°) = -90°
        Quaternionf result = new Quaternionf();
        transform.getRotationQuaternion(result);
        Quaternionf expected = new Quaternionf().rotationY((float) Math.toRadians(-90));

        assertEquals(expected.y, result.y, EPSILON);
        assertEquals(expected.w, result.w, EPSILON);
    }

    private int createPlayer() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new CharacterState());
        return entity;
    }
}
