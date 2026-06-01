package org.chapzlock.core.ecs;

import java.util.Optional;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetSingletonTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    record GameState(int score) implements Component {}
    record Health(int hp) implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @Test
    void getSingleton_returnsComponentWhenOneExists() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new GameState(42));

        Optional<GameState> result = registry.getSingleton(GameState.class);

        assertTrue(result.isPresent());
        assertEquals(42, result.get().score());
    }

    @Test
    void getSingleton_returnsEmptyWhenNoneExist() {
        Optional<GameState> result = registry.getSingleton(GameState.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSingleton_returnsFirstWhenMultipleExist() {
        int e1 = Entity.create(registry);
        int e2 = Entity.create(registry);
        registry.addComponent(e1, new GameState(10));
        registry.addComponent(e2, new GameState(20));

        Optional<GameState> result = registry.getSingleton(GameState.class);

        assertTrue(result.isPresent());
        assertTrue(result.get().score() == 10 || result.get().score() == 20);
    }

    @Test
    void getSingleton_returnsEmptyForUnregisteredType() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new GameState(1));

        Optional<Health> result = registry.getSingleton(Health.class);

        assertTrue(result.isEmpty());
    }
}
