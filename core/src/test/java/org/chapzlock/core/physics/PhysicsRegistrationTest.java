package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntitySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that physics registration happens immediately via EntitySystem's atomic commit.
 *
 * <p>EntitySystem guarantees all components are present before ComponentAddedEvent fires.
 * PhysicsSystem subscribes to that event and registers immediately — no deferred queue needed.
 */
class PhysicsRegistrationTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private EntitySystem entitySystem;
    private PhysicsSystem physicsSystem;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        entitySystem = ctx.getEntitySystem();
        physicsSystem = new PhysicsSystem(PhysicsWorldConfig.defaults());
        physicsSystem.onInit(ctx);
    }

    @Test
    void entityRegisteredImmediatelyOnInstantiate() {
        int entity = entitySystem.builder("ball")
            .transform(Transform.at(0, 10, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        // Registration happens immediately — no onUpdate() needed
        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(entity),
            "Body should be registered immediately after instantiate()");
    }

    @Test
    void multipleEntitiesRegisteredImmediately() {
        int e1 = entitySystem.builder("sphere")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(0.5f))
                .build())
            .instantiate();

        int e2 = entitySystem.builder("floor")
            .transform(Transform.at(3, 0, 0))
            .component(PhysicsBody.builder()
                .mass(0f)
                .geometry(CollisionGeometry.box(5, 0.1f, 5))
                .build())
            .instantiate();

        Physics physics = ctx.getPhysics();
        assertTrue(physics.isRegistered(e1));
        assertTrue(physics.isRegistered(e2));
    }

    @Test
    void entityRemovedAfterRegistration_getsDeregistered() {
        int entity = entitySystem.builder("ball")
            .transform(Transform.at(0, 5, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.sphere(1f))
                .build())
            .instantiate();

        assertTrue(ctx.getPhysics().isRegistered(entity));

        registry.destroyEntity(entity);
        physicsSystem.onUpdate(1f / 60f);

        assertFalse(ctx.getPhysics().isRegistered(entity));
    }

    @Test
    void capsuleGeometry_registersCorrectly() {
        int entity = entitySystem.builder("player")
            .transform(Transform.at(0, 10, 0))
            .component(PhysicsBody.builder()
                .mass(1f)
                .geometry(CollisionGeometry.capsule(0.4f, 1.8f))
                .build())
            .instantiate();

        assertTrue(ctx.getPhysics().isRegistered(entity));
    }
}
