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
}
