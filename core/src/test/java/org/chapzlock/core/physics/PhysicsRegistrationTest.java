package org.chapzlock.core.physics;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that physics registration is deferred to PhysicsSystem.onUpdate().
 *
 * <p>Bodies are queued on ComponentAddedEvent and processed at the start of the
 * next physics step. This guarantees glTF child meshes are available for
 * mesh-dependent shapes (TriangleMesh, ConvexHull).
 */
class PhysicsRegistrationTest {

    private Engine engine;
    private ComponentRegistry registry;
    private EntityFactory entityFactory;
    private PhysicsSystem physicsSystem;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        entityFactory = engine.getEntityFactory();
        physicsSystem = new PhysicsSystem(PhysicsWorldConfig.defaults());
        physicsSystem.onInit(engine);
    }

    @Test
    void entityNotRegisteredBeforeOnUpdate() {
        int entity = entityFactory.builder("ball")
            .transform(Transform.at(0, 10, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        assertFalse(engine.getPhysics().isRegistered(entity),
            "Body should NOT be registered before onUpdate() drains the queue");
    }

    @Test
    void entityRegisteredAfterOnUpdate() {
        int entity = entityFactory.builder("ball")
            .transform(Transform.at(0, 10, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(engine.getPhysics().isRegistered(entity),
            "Body should be registered after onUpdate() drains the queue");
    }

    @Test
    void multipleEntitiesRegisteredAfterOnUpdate() {
        int e1 = entityFactory.builder("sphere")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(0.5f))
                .build())
            .instantiate();

        int e2 = entityFactory.builder("floor")
            .transform(Transform.at(3, 0, 0))
            .component(PhysicsBody.builder()
                .mass(0f)
                .geometry(CollisionGeometry.box(5, 0.1f, 5))
                .build())
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);

        Physics physics = engine.getPhysics();
        assertTrue(physics.isRegistered(e1));
        assertTrue(physics.isRegistered(e2));
    }

    @Test
    void entityDestroyedBeforeOnUpdate_skipsRegistration() {
        int entity = entityFactory.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        registry.destroyEntity(entity);
        physicsSystem.onUpdate(1f / 60f);

        assertFalse(engine.getPhysics().isRegistered(entity),
            "Destroyed entity should be skipped during queue drain");
    }

    @Test
    void physicsBodyRemovedBeforeOnUpdate_skipsRegistration() {
        int entity = entityFactory.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        registry.removeComponent(entity, PhysicsBody.class);
        physicsSystem.onUpdate(1f / 60f);

        assertFalse(engine.getPhysics().isRegistered(entity),
            "Entity whose PhysicsBody was removed should be skipped");
    }

    @Test
    void entityRemovedAfterRegistration_getsDeregistered() {
        int entity = entityFactory.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);
        assertTrue(engine.getPhysics().isRegistered(entity));

        registry.destroyEntity(entity);
        physicsSystem.onUpdate(1f / 60f);

        assertFalse(engine.getPhysics().isRegistered(entity));
    }

    @Test
    void capsuleGeometry_registersCorrectly() {
        int entity = entityFactory.builder("player")
            .transform(Transform.at(0, 10, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.capsule(0.4f, 1.8f))
                .build())
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(engine.getPhysics().isRegistered(entity));
    }
}
