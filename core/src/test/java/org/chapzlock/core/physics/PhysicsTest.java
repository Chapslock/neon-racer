package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsTest {

    private Physics physics;
    private PhysicsWorld world;
    private ComponentRegistry registry;

    @BeforeEach
    void setUp() {
        EngineContext ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        world = new PhysicsWorld(PhysicsWorldConfig.defaults());
        physics = new Physics(world);
    }

    @Test
    void applyImpulse_changesVelocity() {
        int entity = createDynamicEntity(0, 10, 0);
        world.register(entity, registry);

        physics.applyImpulse(entity, new Vector3f(0, 10, 0));
        Vector3f velocity = physics.getLinearVelocity(entity);
        assertTrue(velocity.y > 0f, "Expected upward velocity after impulse");
    }

    @Test
    void isRegistered_returnsTrueForRegisteredEntity() {
        int entity = createDynamicEntity(0, 5, 0);
        world.register(entity, registry);

        assertTrue(physics.isRegistered(entity));
    }

    @Test
    void isRegistered_returnsFalseForUnregisteredEntity() {
        assertFalse(physics.isRegistered(999));
    }

    @Test
    void stopMotion_zeroesBothVelocities() {
        int entity = createDynamicEntity(0, 10, 0);
        world.register(entity, registry);

        physics.applyImpulse(entity, new Vector3f(5, 5, 5));
        physics.stopMotion(entity);

        Vector3f linear = physics.getLinearVelocity(entity);
        Vector3f angular = physics.getAngularVelocity(entity);
        assertEquals(0f, linear.x, 0.001f);
        assertEquals(0f, linear.y, 0.001f);
        assertEquals(0f, linear.z, 0.001f);
        assertEquals(0f, angular.x, 0.001f);
        assertEquals(0f, angular.y, 0.001f);
        assertEquals(0f, angular.z, 0.001f);
    }

    @Test
    void operationsOnUnregisteredEntity_areNoOps() {
        physics.applyForce(999, new Vector3f(10, 0, 0));
        physics.applyImpulse(999, new Vector3f(0, 10, 0));
        physics.stopMotion(999);

        assertNull(physics.getLinearVelocity(999));
    }

    @Test
    void teleport_movesBodyInstantly() {
        int entity = createDynamicEntity(0, 0, 0);
        world.register(entity, registry);

        physics.teleport(entity, new Vector3f(100, 200, 300));
        world.syncTransformsToEcs(registry);

        Transform transform = registry.getComponent(entity, Transform.class);
        Vector3f pos = new Vector3f();
        transform.getPosition(pos);
        assertEquals(100f, pos.x, 0.001f);
        assertEquals(200f, pos.y, 0.001f);
        assertEquals(300f, pos.z, 0.001f);
    }

    private int createDynamicEntity(float x, float y, float z) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(CollisionGeometry.sphere(1f))
            .build());
        return entity;
    }
}
