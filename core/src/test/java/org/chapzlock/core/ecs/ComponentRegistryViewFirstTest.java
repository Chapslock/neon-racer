package org.chapzlock.core.ecs;

import java.util.Optional;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRegistryViewFirstTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    record TagA() implements Component {}
    record TagB() implements Component {}

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @AfterEach
    void tearDown() {
        eventBus.clearAllListeners();
    }

    @Test
    void viewFirstReturnsEmptyWhenNoMatch() {
        Optional<EntityView> result = registry.viewFirst(TagB.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void viewFirstReturnsFirstMatchingEntity() {
        int entityId = 8000;
        registry.addComponent(entityId, new TagA());

        Optional<EntityView> result = registry.viewFirst(TagA.class);

        assertTrue(result.isPresent());
        assertEquals(entityId, result.get().getId());

        registry.destroyEntity(entityId);
    }

    @Test
    void viewFirstWithUnregisteredTypeReturnsEmpty() {
        Optional<EntityView> result = registry.viewFirst(TagB.class);
        assertTrue(result.isEmpty());
    }
}
