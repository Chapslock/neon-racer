package org.chapzlock.core.ecs;

import java.util.ArrayList;
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
    void repeatedCountCallsReturnSameResult() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));

        assertEquals(1, registry.count(Position.class, Velocity.class));
        assertEquals(1, registry.count(Position.class, Velocity.class));
    }

    @Test
    void addingRelevantComponentInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        assertEquals(0, registry.count(Position.class, Velocity.class));

        registry.addComponent(e1, new Velocity(3, 4));
        assertEquals(1, registry.count(Position.class, Velocity.class));
    }

    @Test
    void removingRelevantComponentInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));

        assertEquals(1, registry.count(Position.class, Velocity.class));

        registry.removeComponent(e1, Velocity.class);
        assertEquals(0, registry.count(Position.class, Velocity.class));
    }

    @Test
    void destroyEntityInvalidatesCache() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        assertEquals(1, registry.count(Position.class));

        registry.destroyEntity(e1);
        assertEquals(0, registry.count(Position.class));
    }

    @Test
    void unrelatedComponentChangeDoesNotAffectQuery() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        assertEquals(1, registry.count(Position.class));

        int e2 = Entity.create(registry);
        registry.addComponent(e2, new Health(100));

        assertEquals(1, registry.count(Position.class));
    }

    @Test
    void newEntityMatchingCachedQueryAppearsAfterInvalidation() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        assertEquals(1, registry.count(Position.class));

        int e2 = Entity.create(registry);
        registry.addComponent(e2, new Position(5, 6));

        assertEquals(2, registry.count(Position.class));
    }

    @Test
    void forEachSharesCacheWithCount() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        assertEquals(1, registry.count(Position.class));

        List<Integer> ids = new ArrayList<>();
        registry.forEach(Position.class, (entityId, pos) -> ids.add(entityId));
        assertEquals(1, ids.size());
        assertEquals(e1, ids.get(0).intValue());
    }
}
