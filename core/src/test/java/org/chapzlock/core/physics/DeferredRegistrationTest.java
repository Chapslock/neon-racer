package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.event.EventBus;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the original registration ordering bug is fixed.
 *
 * The bug: PhysicsBody added before Transform → ComponentAddedEvent fires → PhysicsSystem
 * tries to read Transform → null → body silently dropped.
 *
 * The fix: Registration is deferred. PhysicsSystem queues the entity ID when it sees
 * ComponentAddedEvent, then processes the queue at the start of onUpdate() when all
 * components are guaranteed present.
 */
class DeferredRegistrationTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private PhysicsSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        system = new PhysicsSystem(PhysicsWorldConfig.defaults());
        system.onInit(ctx);
    }

    @Test
    void physicsBodyAddedBeforeTransform_stillRegistersSuccessfully() {
        int entity = Entity.create(registry);

        // Add PhysicsBody FIRST (this was the bug scenario)
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.sphere(1f))
            .build());

        // Add Transform SECOND
        registry.addComponent(entity, Transform.at(0, 10, 0));

        // Simulate one frame — deferred registration should process successfully
        system.onUpdate(1f / 60f);

        // The body should be registered and falling under gravity
        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(entity),
            "Body should be registered even though PhysicsBody was added before Transform");
    }

    @Test
    void physicsBodyAddedAfterTransform_alsoWorks() {
        int entity = Entity.create(registry);

        registry.addComponent(entity, Transform.at(0, 10, 0));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.capsule(0.4f, 1.8f))
            .build());

        system.onUpdate(1f / 60f);

        assertTrue(ctx.getPhysics().isRegistered(entity));
    }

    @Test
    void multipleEntitiesRegisteredInSingleFrame() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, PhysicsBody.builder().mass(1f).geometry(CollisionGeometry.sphere(0.5f)).build());
        registry.addComponent(e1, Transform.at(0, 5, 0));

        int e2 = Entity.create(registry);
        registry.addComponent(e2, Transform.at(3, 0, 0));
        registry.addComponent(e2, PhysicsBody.builder().mass(0f).geometry(CollisionGeometry.box(5, 0.1f, 5)).build());

        system.onUpdate(1f / 60f);

        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(e1));
        assertTrue(physics.isRegistered(e2));
    }

    @Test
    void entityRemovedAfterRegistration_getsDeregistered() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 5, 0));
        registry.addComponent(entity, PhysicsBody.builder().mass(1f).geometry(CollisionGeometry.sphere(1f)).build());

        system.onUpdate(1f / 60f);
        assertTrue(ctx.getPhysics().isRegistered(entity));

        registry.destroyEntity(entity);
        system.onUpdate(1f / 60f);

        assertFalse(ctx.getPhysics().isRegistered(entity));
    }
}
