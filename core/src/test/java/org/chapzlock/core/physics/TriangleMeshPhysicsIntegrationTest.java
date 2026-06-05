package org.chapzlock.core.physics;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests proving physics registration works correctly with deferred queue.
 *
 * <h3>Design</h3>
 * <p>Physics bodies are registered during PhysicsSystem.onUpdate(), not on ComponentAddedEvent.
 * This guarantees glTF mesh data is available for mesh-dependent shapes (TriangleMesh,
 * ConvexHull) because glTF loading completes before the first physics step.
 *
 * <p>These tests exercise the full pipeline: EntityBuilder -> EntityFactory -> component resolution
 * -> PhysicsSystem.onUpdate() queue drain -> PhysicsWorld.register().
 */
class TriangleMeshPhysicsIntegrationTest {

    private Engine engine;
    private ComponentRegistry registry;
    private PhysicsSystem physicsSystem;
    private EntityFactory entityFactory;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngineWithPhysics();
        registry = engine.getRegistry();
        entityFactory = engine.getEntityFactory();
        physicsSystem = engine.getPhysicsSystem();
    }

    @Test
    void entitySystem_instantiate_withPhysicsAndTransform_registersAfterOnUpdate() {
        int entity = entityFactory.instantiate(
            EntityBuilder.create("ball")
                .transform(Transform.at(0, 5, 0))
                .component(PhysicsBody.dynamic(1f).shape(CollisionGeometry.sphere(1f))));

        assertTrue(entity >= 0, "Entity should be created successfully");

        assertFalse(engine.getPhysics().isRegistered(entity),
            "Body should NOT be registered before onUpdate()");

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(engine.getPhysics().isRegistered(entity),
            "Body should be registered after onUpdate() drains the queue");
    }

    @Test
    void entitySystem_instantiate_simpleShapes_allRegisterAfterOnUpdate() {
        int box = entityFactory.instantiate(
            EntityBuilder.create("box")
                .transform(Transform.at(0, 0, 0))
                .component(PhysicsBody.asStatic().shape(CollisionGeometry.box(1, 1, 1))));

        int capsule = entityFactory.instantiate(
            EntityBuilder.create("capsule")
                .transform(Transform.at(5, 0, 0))
                .component(PhysicsBody.dynamic(1f).shape(CollisionGeometry.capsule(0.5f, 2f))));

        int sphere = entityFactory.instantiate(
            EntityBuilder.create("sphere")
                .transform(Transform.at(10, 0, 0))
                .component(PhysicsBody.asStatic().shape(CollisionGeometry.sphere(2f))));

        physicsSystem.onUpdate(1f / 60f);

        Physics physics = engine.getPhysics();
        assertTrue(physics.isRegistered(box), "Box body should be registered");
        assertTrue(physics.isRegistered(capsule), "Capsule body should be registered");
        assertTrue(physics.isRegistered(sphere), "Sphere body should be registered");
    }

    @Test
    void entitySystem_instantiate_physicsWithoutTransform_failsValidation() {
        int entity = entityFactory.instantiate(
            EntityBuilder.create("invalid")
                .component(PhysicsBody.asStatic().shape(CollisionGeometry.sphere(1f))));

        assertEquals(-1, entity, "Should return -1 when validation fails (PhysicsBody requires Transform)");
    }

    @Test
    void entitySystem_instantiate_physicsBodyDirectlyAttached() {
        int entity = entityFactory.instantiate(
            EntityBuilder.create("resolved")
                .transform(Transform.at(2, 3, 4))
                .component(PhysicsBody.dynamic(5f)
                    .shape(CollisionGeometry.box(1, 2, 3))
                    .friction(0.8f)
                    .restitution(0.3f)));

        assertTrue(entity >= 0, "Entity should be created");

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(engine.getPhysics().isRegistered(entity), "Body should be registered");

        var physicsBody = registry.getComponent(entity, PhysicsBody.class);
        assertNotNull(physicsBody, "PhysicsBody component should exist");
        assertEquals(5f, physicsBody.getMass(), "Mass should match");
        assertEquals(0.8f, physicsBody.getFriction(), "Friction should match");
        assertEquals(0.3f, physicsBody.getRestitution(), "Restitution should match");
    }

    @Test
    void entitySystem_instantiate_multipleEntitiesAllRegistered() {
        int floor = entityFactory.instantiate(
            EntityBuilder.create("floor")
                .transform(Transform.at(0, -1, 0))
                .component(PhysicsBody.asStatic().shape(CollisionGeometry.box(50, 0.5f, 50))));

        int player = entityFactory.instantiate(
            EntityBuilder.create("player")
                .transform(Transform.at(0, 5, 0))
                .component(PhysicsBody.dynamic(80f).shape(CollisionGeometry.capsule(0.4f, 1.8f))));

        int crate = entityFactory.instantiate(
            EntityBuilder.create("crate")
                .transform(Transform.at(3, 2, 0))
                .component(PhysicsBody.dynamic(10f).shape(CollisionGeometry.box(0.5f, 0.5f, 0.5f))));

        physicsSystem.onUpdate(1f / 60f);

        Physics physics = engine.getPhysics();
        assertTrue(physics.isRegistered(floor), "Floor should be registered");
        assertTrue(physics.isRegistered(player), "Player should be registered");
        assertTrue(physics.isRegistered(crate), "Crate should be registered");
    }
}
