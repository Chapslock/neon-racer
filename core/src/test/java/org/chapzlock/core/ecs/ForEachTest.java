package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForEachTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    record Position(float x, float y) implements Component {}
    record Velocity(float dx, float dy) implements Component {}
    record Health(int hp) implements Component {}
    record Tag1() implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @Test
    void forEach1_iteratesMatchingEntities() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e2, new Position(3, 4));

        List<Float> xs = new ArrayList<>();
        registry.forEach(Position.class, (entityId, pos) -> xs.add(pos.x()));

        assertEquals(2, xs.size());
        assertTrue(xs.contains(1f));
        assertTrue(xs.contains(3f));
    }

    @Test
    void forEach1_skipsNonMatchingEntities() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e2, new Velocity(5, 6));

        List<Integer> visited = new ArrayList<>();
        registry.forEach(Position.class, (entityId, pos) -> visited.add(entityId));

        assertEquals(1, visited.size());
        assertEquals(e1, visited.get(0).intValue());
    }

    @Test
    void forEach2_iteratesEntitiesWithBothComponents() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));
        registry.addComponent(e2, new Position(5, 6));

        List<Integer> visited = new ArrayList<>();
        registry.forEach(Position.class, Velocity.class, (entityId, pos, vel) -> {
            visited.add(entityId);
            assertEquals(1f, pos.x());
            assertEquals(3f, vel.dx());
        });

        assertEquals(1, visited.size());
        assertEquals(e1, visited.get(0).intValue());
    }

    @Test
    void forEach3_iteratesEntitiesWithAllThreeComponents() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));
        registry.addComponent(e1, new Health(100));
        registry.addComponent(e2, new Position(5, 6));
        registry.addComponent(e2, new Velocity(7, 8));

        List<Integer> visited = new ArrayList<>();
        registry.forEach(Position.class, Velocity.class, Health.class,
            (entityId, pos, vel, health) -> {
                visited.add(entityId);
                assertEquals(100, health.hp());
            });

        assertEquals(1, visited.size());
        assertEquals(e1, visited.get(0).intValue());
    }

    @Test
    void forEach4_iteratesEntitiesWithAllFourComponents() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));
        registry.addComponent(e1, new Velocity(3, 4));
        registry.addComponent(e1, new Health(100));
        registry.addComponent(e1, new Tag1());
        registry.addComponent(e2, new Position(5, 6));
        registry.addComponent(e2, new Velocity(7, 8));
        registry.addComponent(e2, new Health(50));

        List<Integer> visited = new ArrayList<>();
        registry.forEach(Position.class, Velocity.class, Health.class, Tag1.class,
            (entityId, pos, vel, health, tag) -> {
                visited.add(entityId);
                assertEquals(1f, pos.x());
                assertEquals(100, health.hp());
            });

        assertEquals(1, visited.size());
        assertEquals(e1, visited.get(0).intValue());
    }

    @Test
    void forEach_passesCorrectEntityId() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(0, 0));

        registry.forEach(Position.class, (entityId, pos) -> {
            assertEquals(e1, entityId);
        });
    }

    @Test
    void forEach_emptyRegistry_doesNothing() {
        List<Integer> visited = new ArrayList<>();
        registry.forEach(Position.class, (entityId, pos) -> visited.add(entityId));

        assertTrue(visited.isEmpty());
    }

    @Test
    void forEach_unregisteredType_doesNothing() {
        int e1 = Entity.create(registry);
        registry.addComponent(e1, new Position(1, 2));

        List<Integer> visited = new ArrayList<>();
        registry.forEach(Health.class, (entityId, h) -> visited.add(entityId));

        assertTrue(visited.isEmpty());
    }
}
