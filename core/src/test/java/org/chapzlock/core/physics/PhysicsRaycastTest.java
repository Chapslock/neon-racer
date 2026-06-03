package org.chapzlock.core.physics;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsRaycastTest {

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
    void raycast_hitsStaticSphere() {
        // Place a sphere at (0, 0, -10), shoot ray forward along -Z
        int target = createStaticEntity(0, 0, -10, CollisionGeometry.sphere(1f));

        // Ray from origin, direction -Z
        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f
        );

        assertTrue(hit.isPresent(), "Ray should hit the sphere");
        assertEquals(target, hit.get().entity());
        // Hit point should be on the near surface of the sphere (z ~ -9)
        assertEquals(-9f, hit.get().point().z, 0.1f);
        // Distance should be ~9 (origin to near surface)
        assertEquals(9f, hit.get().distance(), 0.1f);
    }

    @Test
    void raycast_missesWhenNothingInPath() {
        // Place sphere at (10, 0, 0), shoot ray along -Z
        createStaticEntity(10, 0, 0, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f
        );

        assertFalse(hit.isPresent(), "Ray should miss the sphere");
    }

    @Test
    void raycast_respectsMaxDistance() {
        // Place sphere at (0, 0, -50), shoot ray with max distance 10
        createStaticEntity(0, 0, -50, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            10f
        );

        assertFalse(hit.isPresent(), "Ray should not reach the sphere");
    }

    @Test
    void raycast_returnsHitNormal() {
        // Place sphere at (0, 0, -10), ray from origin along -Z
        createStaticEntity(0, 0, -10, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f
        );

        assertTrue(hit.isPresent());
        Vector3f normal = hit.get().normal();
        // Normal at the near pole of a sphere facing +Z should be approximately (0, 0, 1)
        assertEquals(0f, normal.x, 0.1f);
        assertEquals(0f, normal.y, 0.1f);
        assertEquals(1f, normal.z, 0.1f);
    }

    @Test
    void raycast_excludesSpecifiedEntity() {
        // Place a sphere at -5 and exclude it — closest hit is excluded so result is empty
        int excluded = createStaticEntity(0, 0, -5, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f,
            0xFFFFFFFF,
            excluded
        );

        assertFalse(hit.isPresent(), "Ray should return empty when closest hit is the excluded entity");
    }

    @Test
    void raycast_excludeDoesNotAffectOtherEntities() {
        // Place a sphere at -10, exclude a non-existent entity — should still hit
        int target = createStaticEntity(0, 0, -10, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f,
            0xFFFFFFFF,
            9999  // exclude entity that doesn't exist in the path
        );

        assertTrue(hit.isPresent(), "Ray should still hit when excluded entity is not in path");
        assertEquals(target, hit.get().entity());
    }

    @Test
    void raycast_layerFilterBlocksHit() {
        int layer2 = 2;
        // Place a sphere on layer 2
        createStaticEntityOnLayer(0, 0, -10, CollisionGeometry.sphere(1f), layer2);

        // Raycast only against layer 1 — should miss
        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f,
            1,  // layerMask = only layer 1
            -1  // no exclusion
        );

        assertFalse(hit.isPresent(), "Ray should not hit body on layer 2 when layerMask=1");
    }

    @Test
    void raycast_layerFilterAllowsHit() {
        int layer2 = 2;
        // Place a sphere on layer 2
        int target = createStaticEntityOnLayer(0, 0, -10, CollisionGeometry.sphere(1f), layer2);

        // Raycast against layer 2 — should hit
        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f,
            2,  // layerMask includes layer 2
            -1  // no exclusion
        );

        assertTrue(hit.isPresent(), "Ray should hit body on layer 2 when layerMask=2");
        assertEquals(target, hit.get().entity());
    }

    @Test
    void raycast_hitsDynamicBody() {
        // Place a dynamic sphere at (0, 0, -10)
        int target = createDynamicEntity(0, 0, -10, CollisionGeometry.sphere(1f));

        Optional<RaycastHit> hit = physics.raycast(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, -1),
            100f
        );

        assertTrue(hit.isPresent(), "Ray should hit dynamic body");
        assertEquals(target, hit.get().entity());
    }

    // --- Helpers ---

    private int createStaticEntity(float x, float y, float z, CollisionGeometry geometry) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(0f)
            .geometry(geometry)
            .build());
        world.register(entity, registry);
        return entity;
    }

    private int createStaticEntityOnLayer(float x, float y, float z, CollisionGeometry geometry, int layer) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(0f)
            .geometry(geometry)
            .layer(layer)
            .collisionMask(0xFFFFFFFF)
            .build());
        world.register(entity, registry);
        return entity;
    }

    private int createDynamicEntity(float x, float y, float z, CollisionGeometry geometry) {
        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(x, y, z));
        registry.addComponent(entity, PhysicsBody.builder()
            .mass(1f)
            .geometry(geometry)
            .build());
        world.register(entity, registry);
        return entity;
    }
}
