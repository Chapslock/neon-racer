package org.chapzlock.core.ecs;

import org.chapzlock.core.component.Name;
import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameComponentTest {

    private EventBus eventBus;
    private ComponentRegistry registry;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        registry = new ComponentRegistry(eventBus);
    }

    @Test
    void getEntityByName_returnsEntityId() {
        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new Name("player"));

        assertEquals(entityId, registry.getEntityByName("player"));
    }

    @Test
    void getEntityByName_returnsMinusOneWhenNotFound() {
        assertEquals(-1, registry.getEntityByName("nonexistent"));
    }

    @Test
    void hasEntity_returnsTrueWhenExists() {
        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new Name("camera"));

        assertTrue(registry.hasEntity("camera"));
    }

    @Test
    void hasEntity_returnsFalseWhenNotExists() {
        assertFalse(registry.hasEntity("ghost"));
    }

    @Test
    void removeNameComponent_removesFromIndex() {
        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new Name("player"));
        registry.removeComponent(entityId, Name.class);

        assertEquals(-1, registry.getEntityByName("player"));
        assertFalse(registry.hasEntity("player"));
    }

    @Test
    void destroyEntity_removesFromIndex() {
        int entityId = Entity.create(registry);
        registry.addComponent(entityId, new Name("player"));
        registry.destroyEntity(entityId);

        assertEquals(-1, registry.getEntityByName("player"));
    }

    @Test
    void duplicateName_lastWriteWins() {
        int entity1 = Entity.create(registry);
        int entity2 = Entity.create(registry);
        registry.addComponent(entity1, new Name("shared"));
        registry.addComponent(entity2, new Name("shared"));

        assertEquals(entity2, registry.getEntityByName("shared"));
    }

    @Test
    void multipleNamedEntities_eachFindable() {
        int player = Entity.create(registry);
        int enemy = Entity.create(registry);
        registry.addComponent(player, new Name("player"));
        registry.addComponent(enemy, new Name("enemy"));

        assertEquals(player, registry.getEntityByName("player"));
        assertEquals(enemy, registry.getEntityByName("enemy"));
    }
}
