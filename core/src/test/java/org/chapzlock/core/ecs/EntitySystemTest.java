package org.chapzlock.core.ecs;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.physics.CollisionGeometry;
import org.chapzlock.core.scene.EntityBlueprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EntitySystemTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private EventBus eventBus;
    private EntitySystem entitySystem;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        eventBus = ctx.getEventBus();
        entitySystem = new EntitySystem();
        entitySystem.onInit(ctx);
    }

    @Test
    void instantiate_createsEntityWithAllComponents() {
        int entityId = entitySystem.builder("test")
            .transform(Transform.at(1, 2, 3))
            .instantiate();

        assertTrue(entityId >= 0);
        assertNotNull(registry.getComponent(entityId, Transform.class));
    }

    @Test
    void instantiate_firesEventsAfterAllComponentsPresent() {
        List<Boolean> transformPresentAtPhysicsEvent = new ArrayList<>();

        eventBus.subscribe(ComponentAddedEvent.class, event -> {
            if (event.componentType() == PhysicsBody.class) {
                Transform t = registry.getComponent(event.entityId(), Transform.class);
                transformPresentAtPhysicsEvent.add(t != null);
            }
            return false;
        });

        entitySystem.builder("test")
            .transform(Transform.at(0, 0, 0))
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(1, transformPresentAtPhysicsEvent.size());
        assertTrue(transformPresentAtPhysicsEvent.get(0));
    }

    @Test
    void instantiate_validatesRules_rejectsInvalid() {
        // PhysicsBody requires Transform — omitting Transform should fail
        int entityId = entitySystem.builder("invalid")
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(-1, entityId);
    }

    @Test
    void instantiate_noEventsOnFailure() {
        AtomicInteger eventCount = new AtomicInteger(0);
        eventBus.subscribe(ComponentAddedEvent.class, e -> {
            eventCount.incrementAndGet();
            return false;
        });

        entitySystem.builder("invalid")
            .physics(PhysicsDescriptor.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(0, eventCount.get());
    }

    @Test
    void instantiateBlueprint_worksWithExistingBlueprint() {
        EntityBlueprint blueprint = new EntityBuilder("from-blueprint")
            .transform(Transform.at(5, 5, 5))
            .physics(PhysicsDescriptor.asStatic().shape(CollisionGeometry.box(1, 1, 1)))
            .build();

        int entityId = entitySystem.instantiate(blueprint);

        assertTrue(entityId >= 0);
        assertNotNull(registry.getComponent(entityId, Transform.class));
        assertNotNull(registry.getComponent(entityId, PhysicsBody.class));
    }
}
