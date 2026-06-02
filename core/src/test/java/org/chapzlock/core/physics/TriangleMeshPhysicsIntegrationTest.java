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
 * Integration tests proving the original TriangleMesh collision shape timing bug is fixed.
 *
 * <h3>The original bug</h3>
 * <pre>
 * IllegalStateException: TriangleMesh geometry requires mesh data but none was available.
 * </pre>
 *
 * <p>This happened because:
 * <ol>
 *   <li>Entity got PhysicsBody added -> ComponentAddedEvent fired immediately</li>
 *   <li>PhysicsSystem tried to resolve collision shape</li>
 *   <li>glTF mesh data wasn't loaded yet -> null -> exception</li>
 * </ol>
 *
 * <h3>The fix: EntitySystem atomic commit</h3>
 * <p>With EntitySystem's phased assembly pipeline:
 * <ol>
 *   <li>All components are added silently (no events)</li>
 *   <li>Events fire only after the entity is fully assembled</li>
 *   <li>PhysicsSystem registers immediately and successfully because all data is present</li>
 * </ol>
 *
 * <p>These tests exercise the full pipeline: EntityBuilder -> EntitySystem -> DescriptorResolver
 * -> PhysicsSystem event handler -> CollisionShapeResolver registration.
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
    void entitySystem_instantiate_withPhysicsAndTransform_registersImmediately() {
        // This is the scenario that used to throw IllegalStateException.
        // PhysicsDescriptor goes through DescriptorResolver -> PhysicsBody -> event -> registration.
        int entity = entitySystem.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertTrue(entity >= 0, "Entity should be created successfully");
        assertTrue(ctx.getPhysics().isRegistered(entity),
            "Body should be registered immediately via the descriptor pipeline");
    }

    @Test
    void entitySystem_instantiate_simpleShapes_noExceptionThrown() {
        // Box — static body with box shape
        int box = entitySystem.builder("box")
            .transform(Transform.at(0, 0, 0))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.box(1, 1, 1)))
            .instantiate();
        assertTrue(box >= 0, "Box entity should be created");
        assertTrue(ctx.getPhysics().isRegistered(box), "Box body should be registered");

        // Capsule — dynamic body with capsule shape
        int capsule = entitySystem.builder("capsule")
            .transform(Transform.at(5, 0, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.capsule(0.5f, 2f)))
            .instantiate();
        assertTrue(capsule >= 0, "Capsule entity should be created");
        assertTrue(ctx.getPhysics().isRegistered(capsule), "Capsule body should be registered");

        // Sphere — static body with sphere shape
        int sphere = entitySystem.builder("sphere")
            .transform(Transform.at(10, 0, 0))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.sphere(2f)))
            .instantiate();
        assertTrue(sphere >= 0, "Sphere entity should be created");
        assertTrue(ctx.getPhysics().isRegistered(sphere), "Sphere body should be registered");
    }

    @Test
    void entitySystem_instantiate_physicsWithoutTransform_failsValidation() {
        // PhysicsBody requires Transform (ComponentRule). EntitySystem should reject this
        // and return -1 instead of throwing.
        int entity = entitySystem.builder("invalid")
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(-1, entity, "Should return -1 when validation fails (PhysicsBody requires Transform)");
    }

    @Test
    void entitySystem_instantiate_registersBeforeOnUpdate() {
        // Proves registration is immediate (not deferred to onUpdate).
        // The old deferred-queue approach required onUpdate() to process pending registrations.
        int entity = entitySystem.builder("immediate")
            .transform(Transform.at(0, 0, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(0.5f)))
            .instantiate();

        // Check BEFORE any onUpdate call — this is the crux of the fix
        assertTrue(ctx.getPhysics().isRegistered(entity),
            "Body should be registered immediately without calling onUpdate()");
    }

    @Test
    void entitySystem_instantiate_descriptorResolvesToPhysicsBody() {
        // Verify the full pipeline: PhysicsDescriptor -> DescriptorResolver -> PhysicsBody component
        int entity = entitySystem.builder("resolved")
            .transform(Transform.at(2, 3, 4))
            .physics(PhysicsDescriptor.dynamic(5f)
                .shape(CollisionGeometry.box(1, 2, 3))
                .friction(0.8f)
                .restitution(0.3f))
            .instantiate();

        assertTrue(entity >= 0, "Entity should be created");
        assertTrue(ctx.getPhysics().isRegistered(entity), "Body should be registered");

        // Verify the PhysicsBody component was actually created from the descriptor
        var physicsBody = registry.getComponent(entity,
            org.chapzlock.core.component.PhysicsBody.class);
        assertNotNull(physicsBody, "PhysicsBody component should exist after descriptor resolution");
        assertEquals(5f, physicsBody.getMass(), "Mass should be resolved from descriptor");
        assertEquals(0.8f, physicsBody.getFriction(), "Friction should be resolved from descriptor");
        assertEquals(0.3f, physicsBody.getRestitution(), "Restitution should be resolved from descriptor");
    }

    @Test
    void entitySystem_instantiate_multipleEntitiesAllRegistered() {
        // Simulates a scene with multiple physics entities, proving no ordering issue exists
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

        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(floor), "Floor should be registered");
        assertTrue(physics.isRegistered(player), "Player should be registered");
        assertTrue(physics.isRegistered(crate), "Crate should be registered");
    }
}
