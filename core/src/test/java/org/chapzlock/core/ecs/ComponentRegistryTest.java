package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.event.ComponentAddedEvent;
import org.chapzlock.core.event.ComponentRemovedEvent;
import org.chapzlock.core.event.EntityDestroyedEvent;
import org.chapzlock.core.event.EventBus;
import org.chapzlock.core.event.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRegistryTest {

    private EventBus eventBus;
    private ComponentRegistry registry;
    private List<Subscription> subscriptions;

    record PositionComponent(float x, float y) implements Component {}
    record VelocityComponent(float dx, float dy) implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
        subscriptions = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        subscriptions.forEach(Subscription::unsubscribe);
        eventBus.clearAllListeners();
    }

    @Test
    void addComponentPublishesComponentAddedEvent() {
        List<ComponentAddedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentAddedEvent.class, event -> {
            events.add(event);
            return false;
        }));

        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new PositionComponent(1, 2));

        assertEquals(1, events.size());
        assertEquals(entityId, events.get(0).entityId());
        assertEquals(PositionComponent.class, events.get(0).componentType());

        registry.destroyEntity(entityId);
    }

    @Test
    void addComponentsPublishesEventsAfterAllStored() {
        List<ComponentAddedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentAddedEvent.class, event -> {
            events.add(event);
            if (event.componentType() == PositionComponent.class) {
                assertNotNull(registry.getComponent(event.entityId(), VelocityComponent.class),
                    "VelocityComponent should already be stored when PositionComponent event fires");
            }
            return false;
        }));

        int entityId = Entity.create(registry);
        registry.addComponents(entityId, new PositionComponent(1, 2), new VelocityComponent(3, 4));

        assertEquals(2, events.size());

        registry.destroyEntity(entityId);
    }

    @Test
    void removeComponentPublishesEventBeforeRemoval() {
        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new PositionComponent(5, 6));

        List<ComponentRemovedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentRemovedEvent.class, event -> {
            events.add(event);
            assertNotNull(registry.getComponent(event.entityId(), PositionComponent.class),
                "Component should still be accessible during removal event");
            return false;
        }));

        registry.removeComponent(entityId, PositionComponent.class);

        assertEquals(1, events.size());
        assertEquals(entityId, events.get(0).entityId());
        assertEquals(PositionComponent.class, events.get(0).componentType());

        assertNull(registry.getComponent(entityId, PositionComponent.class));
    }

    @Test
    void destroyEntityPublishesComponentRemovedThenEntityDestroyed() {
        int entityId = Entity.create(registry);
        registry.addComponents(entityId, new PositionComponent(1, 1), new VelocityComponent(2, 2));

        List<Object> eventOrder = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentRemovedEvent.class, event -> {
            if (event.entityId() == entityId) eventOrder.add(event);
            return false;
        }));
        subscriptions.add(eventBus.subscribe(EntityDestroyedEvent.class, event -> {
            if (event.entityId() == entityId) eventOrder.add(event);
            return false;
        }));

        registry.destroyEntity(entityId);

        assertEquals(3, eventOrder.size());
        assertInstanceOf(ComponentRemovedEvent.class, eventOrder.get(0));
        assertInstanceOf(ComponentRemovedEvent.class, eventOrder.get(1));
        assertInstanceOf(EntityDestroyedEvent.class, eventOrder.get(2));

        assertNull(registry.getComponent(entityId, PositionComponent.class));
        assertNull(registry.getComponent(entityId, VelocityComponent.class));
        assertEquals(0, registry.count(PositionComponent.class));
    }

    @Test
    void destroyEntityWithNoComponentsIsNoOp() {
        List<Object> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(EntityDestroyedEvent.class, event -> {
            events.add(event);
            return false;
        }));

        registry.destroyEntity(99999);

        assertTrue(events.isEmpty());
    }

    @Test
    void addComponentsSilent_addsComponentsWithoutFiringEvents() {
        List<ComponentAddedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentAddedEvent.class, event -> {
            events.add(event);
            return false;
        }));

        int entityId = Entity.create(registry);
        registry.addComponentsSilent(entityId, List.of(
            new PositionComponent(10, 20),
            new VelocityComponent(1, 2)
        ));

        assertEquals(0, events.size(), "No ComponentAddedEvent should be fired by addComponentsSilent");

        PositionComponent pos = registry.getComponent(entityId, PositionComponent.class);
        assertNotNull(pos, "PositionComponent should be retrievable after silent add");
        assertEquals(10, pos.x());
        assertEquals(20, pos.y());

        VelocityComponent vel = registry.getComponent(entityId, VelocityComponent.class);
        assertNotNull(vel, "VelocityComponent should be retrievable after silent add");
        assertEquals(1, vel.dx());
        assertEquals(2, vel.dy());

        registry.destroyEntity(entityId);
    }

    @Test
    void addComponentsSilent_invalidatesQueryCache() {
        int existingEntity = Entity.create(registry);
        registry.addComponent(existingEntity, new PositionComponent(0, 0));

        // Prime the query cache by running a forEach
        List<Integer> foundBefore = new ArrayList<>();
        registry.forEach(PositionComponent.class, (entityId, pos) -> foundBefore.add(entityId));
        assertEquals(1, foundBefore.size());

        // Now add a component silently to a new entity
        int newEntity = Entity.create(registry);
        registry.addComponentsSilent(newEntity, List.of(new PositionComponent(5, 5)));

        // The query should now find both entities (cache was invalidated)
        List<Integer> foundAfter = new ArrayList<>();
        registry.forEach(PositionComponent.class, (entityId, pos) -> foundAfter.add(entityId));
        assertEquals(2, foundAfter.size(), "Query cache should be invalidated so new entity is found");

        registry.destroyEntity(existingEntity);
        registry.destroyEntity(newEntity);
    }

    @Test
    void addComponentsSilent_warnsAndReturnsEarlyForNonExistentEntity() {
        List<ComponentAddedEvent> events = new ArrayList<>();
        subscriptions.add(eventBus.subscribe(ComponentAddedEvent.class, event -> {
            events.add(event);
            return false;
        }));

        // Should not throw, just warn and return
        registry.addComponentsSilent(99999, List.of(new PositionComponent(1, 1)));

        assertEquals(0, events.size());
        assertNull(registry.getComponent(99999, PositionComponent.class));
    }
}
