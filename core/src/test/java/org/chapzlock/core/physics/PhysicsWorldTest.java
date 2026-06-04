package org.chapzlock.core.physics;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsWorldTest {

    private PhysicsWorld world;
    private ComponentRegistry registry;
    private Engine engine;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        world = new PhysicsWorld(PhysicsWorldConfig.defaults());
    }

    @Test
    void registerBody_createsInternalRigidBody() {
        int entity = createPhysicsEntity(1f, CollisionGeometry.sphere(1f), 0, 5, 0);

        world.register(entity, registry);

        assertTrue(world.isRegistered(entity));
    }

    @Test
    void dynamicBody_fallsUnderGravity() {
        int entity = createPhysicsEntity(1f, CollisionGeometry.sphere(1f), 0, 10, 0);

        world.register(entity, registry);

        for (int i = 0; i < 60; i++) {
            world.step(1f / 60f);
        }
        world.syncTransformsToEcs(registry);

        Transform transform = registry.getComponent(entity, Transform.class);
        Vector3f pos = new Vector3f();
        transform.getPosition(pos);
        assertTrue(pos.y < 10f, "Body should have fallen. Y=" + pos.y);
        assertTrue(pos.y > 0f, "Body shouldn't have fallen through the world. Y=" + pos.y);
    }

    @Test
    void staticBody_doesNotMove() {
        int entity = createPhysicsEntity(0f, CollisionGeometry.sphere(1f), 0, 5, 0);

        world.register(entity, registry);

        for (int i = 0; i < 60; i++) {
            world.step(1f / 60f);
        }
        world.syncTransformsToEcs(registry);

        Transform transform = registry.getComponent(entity, Transform.class);
        Vector3f pos = new Vector3f();
        transform.getPosition(pos);
        assertEquals(5f, pos.y, 0.001f);
    }

    @Test
    void queueRemoval_removesBodyFromWorld() {
        int entity = createPhysicsEntity(1f, CollisionGeometry.sphere(1f), 0, 5, 0);

        world.register(entity, registry);
        assertTrue(world.isRegistered(entity));

        world.queueRemoval(entity);
        world.processPendingRemovals();
        assertFalse(world.isRegistered(entity));
    }

    @Test
    void register_worksWhenBothComponentsPresent() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.box(1, 1, 1))
            .build());
        registry.addComponent(entity, Transform.at(0, 5, 0));

        world.register(entity, registry);

        assertTrue(world.isRegistered(entity));
    }

    @Test
    void dispose_cleansUpAllBodies() {
        int e1 = createPhysicsEntity(1f, CollisionGeometry.sphere(1f), 0, 5, 0);
        int e2 = createPhysicsEntity(0f, CollisionGeometry.box(1, 1, 1), 3, 0, 0);

        world.register(e1, registry);
        world.register(e2, registry);

        world.dispose();

        assertFalse(world.isRegistered(e1));
        assertFalse(world.isRegistered(e2));
    }

    @Test
    void dynamicBodyWithOffset_syncsBackToLogicalPosition() {
        int entity = createPhysicsEntity(1f, CollisionGeometry.sphere(0.5f).offset(0, 2f, 0), 0, 10, 0);

        world.register(entity, registry);

        world.step(1f / 60f);
        world.syncTransformsToEcs(registry);

        Transform transform = registry.getComponent(entity, Transform.class);
        Vector3f pos = new Vector3f();
        transform.getPosition(pos);

        // Entity should have fallen slightly (gravity) and offset subtracted
        assertTrue(pos.y < 10f, "Entity should have fallen slightly. Y=" + pos.y);
        assertTrue(pos.y > 9.5f, "Entity shouldn't have fallen far in one frame. Y=" + pos.y);
    }

    @Test
    void dynamicBodyWithOffset_rotatedEntity_appliesRotatedOffset() {
        // Entity at (0, 5, 0) rotated 90° around Y axis
        // Offset is (0, 0, 3) in local space
        // After 90° Y rotation, local Z maps to world X → body center at (3, 5, 0)
        int entity = Entity.create(registry);
        Transform transform = Transform.at(0, 5, 0, 0, 90, 0);
        registry.addComponent(entity, transform);
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(0f)
            .geometry(CollisionGeometry.sphere(0.5f).offset(0, 0, 3f))
            .build());

        world.register(entity, registry);

        com.bulletphysics.linearmath.Transform bulletTransform = new com.bulletphysics.linearmath.Transform();
        world.getBody(entity).getWorldTransform(bulletTransform);

        // After 90° Y rotation: local (0,0,3) → world (3,0,0) approximately
        assertEquals(3f, bulletTransform.origin.x, 0.1f);
        assertEquals(5f, bulletTransform.origin.y, 0.1f);
        assertEquals(0f, bulletTransform.origin.z, 0.1f);
    }

    @Test
    void dynamicBodyWithOffset_initialRegistrationPlacesBodyAtOffsetPosition() {
        int entity = createPhysicsEntity(0f, CollisionGeometry.sphere(0.5f).offset(0, 3f, 0), 0, 5, 0);

        world.register(entity, registry);

        com.bulletphysics.linearmath.Transform bulletTransform = new com.bulletphysics.linearmath.Transform();
        world.getBody(entity).getWorldTransform(bulletTransform);

        assertEquals(0f, bulletTransform.origin.x, 0.001f);
        assertEquals(8f, bulletTransform.origin.y, 0.001f);
        assertEquals(0f, bulletTransform.origin.z, 0.001f);
    }

    @Test
    void register_rejectsDynamicBodyWithTriangleMesh() {
        int entity = createPhysicsEntity(1f, CollisionGeometry.triangleMesh(), 0, 5, 0);

        world.register(entity, registry);

        assertFalse(world.isRegistered(entity),
            "Dynamic body with TriangleMesh should be rejected (static-only shape)");
    }

    @Test
    void register_allowsStaticBodyWithTriangleMesh() {
        int entity = createPhysicsEntity(0f, CollisionGeometry.triangleMesh(), 0, 0, 0);

        // Static + TriangleMesh is valid but will throw because no mesh data is available
        // in test context. The point: it should NOT be rejected by the dynamic-body validation
        // — it should proceed to mesh resolution and fail there instead.
        var ex = assertThrows(IllegalStateException.class, () -> world.register(entity, registry));
        assertTrue(ex.getMessage().contains("mesh data"),
            "Should fail at mesh resolution, not at dynamic-body validation");
    }

    private int createPhysicsEntity(float mass, CollisionGeometry geometry, float x, float y, float z) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(mass)
            .geometry(geometry)
            .build());
        return entity;
    }
}
