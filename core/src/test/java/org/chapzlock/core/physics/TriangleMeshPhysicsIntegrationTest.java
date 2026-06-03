package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
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
 * <p>These tests exercise the full pipeline: EntityBuilder -> EntitySystem -> DescriptorResolver
 * -> PhysicsSystem.onUpdate() queue drain -> PhysicsWorld.register().
 */
class TriangleMeshPhysicsIntegrationTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private PhysicsSystem physicsSystem;
    private EntitySystem entitySystem;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        entitySystem = ctx.getEntitySystem();

        physicsSystem = new PhysicsSystem(PhysicsWorldConfig.defaults());
        physicsSystem.onInit(ctx);
    }

    @Test
    void entitySystem_instantiate_withPhysicsAndTransform_registersAfterOnUpdate() {
        int entity = entitySystem.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertTrue(entity >= 0, "Entity should be created successfully");

        assertFalse(ctx.getPhysics().isRegistered(entity),
            "Body should NOT be registered before onUpdate()");

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(ctx.getPhysics().isRegistered(entity),
            "Body should be registered after onUpdate() drains the queue");
    }

    @Test
    void entitySystem_instantiate_simpleShapes_allRegisterAfterOnUpdate() {
        int box = entitySystem.builder("box")
            .transform(Transform.at(0, 0, 0))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.box(1, 1, 1)))
            .instantiate();

        int capsule = entitySystem.builder("capsule")
            .transform(Transform.at(5, 0, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.capsule(0.5f, 2f)))
            .instantiate();

        int sphere = entitySystem.builder("sphere")
            .transform(Transform.at(10, 0, 0))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.sphere(2f)))
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);

        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(box), "Box body should be registered");
        assertTrue(physics.isRegistered(capsule), "Capsule body should be registered");
        assertTrue(physics.isRegistered(sphere), "Sphere body should be registered");
    }

    @Test
    void entitySystem_instantiate_physicsWithoutTransform_failsValidation() {
        int entity = entitySystem.builder("invalid")
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(-1, entity, "Should return -1 when validation fails (PhysicsBody requires Transform)");
    }

    @Test
    void entitySystem_instantiate_descriptorResolvesToPhysicsBody() {
        int entity = entitySystem.builder("resolved")
            .transform(Transform.at(2, 3, 4))
            .physics(PhysicsDescriptor.dynamic(5f)
                .shape(CollisionGeometry.box(1, 2, 3))
                .friction(0.8f)
                .restitution(0.3f))
            .instantiate();

        assertTrue(entity >= 0, "Entity should be created");

        physicsSystem.onUpdate(1f / 60f);

        assertTrue(ctx.getPhysics().isRegistered(entity), "Body should be registered");

        var physicsBody = registry.getComponent(entity,
            org.chapzlock.core.component.PhysicsBody.class);
        assertNotNull(physicsBody, "PhysicsBody component should exist after descriptor resolution");
        assertEquals(5f, physicsBody.getMass(), "Mass should be resolved from descriptor");
        assertEquals(0.8f, physicsBody.getFriction(), "Friction should be resolved from descriptor");
        assertEquals(0.3f, physicsBody.getRestitution(), "Restitution should be resolved from descriptor");
    }

    @Test
    void entitySystem_instantiate_multipleEntitiesAllRegistered() {
        int floor = entitySystem.builder("floor")
            .transform(Transform.at(0, -1, 0))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.box(50, 0.5f, 50)))
            .instantiate();

        int player = entitySystem.builder("player")
            .transform(Transform.at(0, 5, 0))
            .physics(PhysicsDescriptor.dynamic(80f).shape(CollisionGeometry.capsule(0.4f, 1.8f)))
            .instantiate();

        int crate = entitySystem.builder("crate")
            .transform(Transform.at(3, 2, 0))
            .physics(PhysicsDescriptor.dynamic(10f).shape(CollisionGeometry.box(0.5f, 0.5f, 0.5f)))
            .instantiate();

        physicsSystem.onUpdate(1f / 60f);

        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(floor), "Floor should be registered");
        assertTrue(physics.isRegistered(player), "Player should be registered");
        assertTrue(physics.isRegistered(crate), "Crate should be registered");
    }
}
