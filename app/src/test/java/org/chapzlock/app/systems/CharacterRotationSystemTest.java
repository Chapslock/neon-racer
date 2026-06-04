package org.chapzlock.app.systems;

import org.chapzlock.app.component.CharacterState;
import org.chapzlock.app.component.ThirdPersonCamera;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.input.InputMap;
import org.chapzlock.core.input.Key;
import org.chapzlock.core.input.keyboard.KeyPressedEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class CharacterRotationSystemTest {

    private static final float DELTA_TIME = 0.016f;

    private Engine engine;
    private ComponentRegistry registry;
    private EventBus eventBus;
    private CharacterRotationSystem system;
    private InputMap inputMap;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        eventBus = engine.getEventBus();

        inputMap = InputMap.builder()
            .axis("turnRight", Key.A, Key.D)
            .build();
        inputMap.activate(eventBus);
        engine.setActiveInputMap(inputMap);

        int camera = Entity.create(registry);
        registry.addComponent(camera, new ThirdPersonCamera());

        system = new CharacterRotationSystem();
        system.onInit(engine);
    }

    @Test
    void noTurnInput_rotationUnchanged() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        transform.setRotationQuaternion(new Quaternionf().rotationY((float) Math.toRadians(45)));

        Quaternionf before = new Quaternionf();
        transform.getRotationQuaternion(before);

        system.onUpdate(DELTA_TIME);

        Quaternionf after = new Quaternionf();
        transform.getRotationQuaternion(after);
        assertEquals(before.y, after.y, 0.001f);
    }

    @Test
    void pressingD_rotatesPlayer() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        transform.setRotationQuaternion(new Quaternionf());

        eventBus.publish(new KeyPressedEvent(GLFW_KEY_D, 0));

        system.onUpdate(DELTA_TIME);

        Quaternionf after = new Quaternionf();
        transform.getRotationQuaternion(after);
        assertNotEquals(0f, after.y, 0.001f, "D key should rotate the player");
    }

    @Test
    void pressingA_rotatesOppositeToD() {
        int player = createPlayer();
        Transform transform = registry.getComponent(player, Transform.class);
        CharacterState state = registry.getComponent(player, CharacterState.class);

        // Test D key
        state.setFacingYaw(0f);
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_D, 0));
        system.onUpdate(DELTA_TIME);
        float yawAfterD = state.getFacingYaw();
        eventBus.publish(new org.chapzlock.core.input.keyboard.KeyReleasedEvent(GLFW_KEY_D, 0));

        // Test A key
        state.setFacingYaw(0f);
        inputMap.frameReset();
        eventBus.publish(new KeyPressedEvent(GLFW_KEY_A, 0));
        system.onUpdate(DELTA_TIME);
        float yawAfterA = state.getFacingYaw();

        assertTrue(yawAfterA * yawAfterD < 0, "A and D should rotate in opposite directions");
    }

    private int createPlayer() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new PlayerTag());
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new CharacterState());
        return entity;
    }
}
