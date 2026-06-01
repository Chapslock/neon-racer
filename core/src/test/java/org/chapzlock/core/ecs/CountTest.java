package org.chapzlock.core.ecs;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    record Position(float x, float y) implements Component {}
    record Velocity(float dx, float dy) implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @Test
    void count_returnsZeroWhenNoEntities() {
        assertEquals(0, registry.count(Position.class));
    }

    @Test
    void count_returnsNumberOfMatchingEntities() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e2, new Position(3, 4));

        assertEquals(2, registry.count(Position.class));
    }

    @Test
    void count_multiComponent_requiresAll() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));
        registry.addComponent(e2, new Position(5, 6));

        assertEquals(1, registry.count(Position.class, Velocity.class));
    }

    @Test
    void count_unregisteredType_returnsZero() {
        assertEquals(0, registry.count(Velocity.class));
    }

    @Test
    void count_reflectsMutations() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        assertEquals(1, registry.count(Position.class));

        registry.removeComponent(e1, Position.class);
        assertEquals(0, registry.count(Position.class));
    }
}
