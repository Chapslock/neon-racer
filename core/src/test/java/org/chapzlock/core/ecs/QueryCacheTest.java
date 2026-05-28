package org.chapzlock.core.ecs;

import java.util.List;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryCacheTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    record Position(float x, float y) implements Component {}
    record Velocity(float dx, float dy) implements Component {}
    record Health(int hp) implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @Test
    void repeatedViewCallsReturnEquivalentResults() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));

        List<EntityView> first = registry.view(Position.class, Velocity.class);
        List<EntityView> second = registry.view(Position.class, Velocity.class);

        assertEquals(first.size(), second.size());
        assertEquals(first.get(0).getId(), second.get(0).getId());
    }

    @Test
    void addingRelevantComponentInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        List<EntityView> before = registry.view(Position.class, Velocity.class);
        assertTrue(before.isEmpty());

        registry.addComponent(e1, new Velocity(3, 4));
        List<EntityView> after = registry.view(Position.class, Velocity.class);
        assertEquals(1, after.size());
    }

    @Test
    void removingRelevantComponentInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));

        List<EntityView> before = registry.view(Position.class, Velocity.class);
        assertEquals(1, before.size());

        registry.removeComponent(e1, Velocity.class);
        List<EntityView> after = registry.view(Position.class, Velocity.class);
        assertTrue(after.isEmpty());
    }

    @Test
    void destroyEntityInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        List<EntityView> before = registry.view(Position.class);
        assertEquals(1, before.size());

        registry.destroyEntity(e1);
        List<EntityView> after = registry.view(Position.class);
        assertTrue(after.isEmpty());
    }

    @Test
    void unrelatedComponentChangeDoesNotAffectQuery() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        List<EntityView> before = registry.view(Position.class);
        assertEquals(1, before.size());

        int e2 = Entity.create(registry);
        registry.addComponent(e2, new Health(100));

        List<EntityView> after = registry.view(Position.class);
        assertEquals(1, after.size());
        assertEquals(before.get(0).getId(), after.get(0).getId());
    }

    @Test
    void newEntityMatchingCachedQueryAppearsAfterInvalidation() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        List<EntityView> before = registry.view(Position.class);
        assertEquals(1, before.size());

        int e2 = Entity.create(registry);
        registry.addComponent(e2, new Position(5, 6));

        List<EntityView> after = registry.view(Position.class);
        assertEquals(2, after.size());
    }
}
