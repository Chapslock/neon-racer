package org.chapzlock.core.ecs;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.physics.CollisionGeometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EntityFactoryTest {

    private Engine engine;
    private ComponentRegistry registry;
    private EventBus eventBus;
    private EntityFactory entityFactory;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        eventBus = engine.getEventBus();
        entityFactory = engine.getEntityFactory();
    }

    @Test
    void instantiate_createsEntityWithAllComponents() {
        int entityId = entityFactory.builder("test")
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

        entityFactory.builder("test")
            .transform(Transform.at(0, 0, 0))
            .component(PhysicsBody.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(1, transformPresentAtPhysicsEvent.size());
        assertTrue(transformPresentAtPhysicsEvent.get(0));
    }

    @Test
    void instantiate_validatesRules_rejectsInvalid() {
        // PhysicsBody requires Transform — omitting Transform should fail
        int entityId = entityFactory.builder("invalid")
            .component(PhysicsBody.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
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

        entityFactory.builder("invalid")
            .component(PhysicsBody.dynamic(1f).shape(CollisionGeometry.sphere(1f)))
            .instantiate();

        assertEquals(0, eventCount.get());
    }

    @Test
    void instantiateDirectCall_worksWithComponentsAndTags() {
        List<Component> components = List.of(
            Transform.at(5, 5, 5),
            PhysicsBody.asStatic().shape(CollisionGeometry.box(1, 1, 1))
        );
        List<Tag> tags = List.of();

        int entityId = entityFactory.instantiate("from-direct", components, tags);

        assertTrue(entityId >= 0);
        assertNotNull(registry.getComponent(entityId, Transform.class));
        assertNotNull(registry.getComponent(entityId, PhysicsBody.class));
    }

    @Test
    void instantiateBuilder_createsEntityWithAllComponents() {
        EntityBuilder builder = EntityBuilder.create("test")
            .transform(Transform.at(1, 2, 3));

        int entityId = entityFactory.instantiate(builder);

        assertTrue(entityId >= 0);
        assertNotNull(registry.getComponent(entityId, Transform.class));
    }

    @Test
    void instantiateBuilder_processesChildrenRecursively() {
        EntityBuilder builder = EntityBuilder.create("parent")
            .transform(Transform.at(0, 0, 0))
            .child("child", c -> c.transform(Transform.at(1, 0, 0)));

        int parentId = entityFactory.instantiate(builder);

        assertTrue(parentId >= 0);
        int childId = registry.getEntityByName("child");
        assertTrue(childId >= 0);

        Hierarchy parentH = registry.getComponent(parentId, Hierarchy.class);
        assertNotNull(parentH);
        assertArrayEquals(new int[]{childId}, parentH.getChildren());
    }
}
