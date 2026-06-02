package org.chapzlock.core.ecs;

import org.chapzlock.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityExistsTest {

    private ComponentRegistry registry;

    record Health(int hp) implements Component {}
    record Armor(int def) implements Component {}

    @BeforeEach
    void setUp() {
        registry = new ComponentRegistry(new EventBus());
    }

    @Test
    void entityExistsReturnsFalseForUnknownId() {
        assertFalse(registry.entityExists(99999));
    }

    @Test
    void entityExistsReturnsTrueAfterAddComponent() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new Health(100));
        assertTrue(registry.entityExists(entity));
    }

    @Test
    void entityExistsReturnsFalseAfterDestroy() {
        int entity = Entity.create(registry);
        registry.addComponent(entity, new Health(100));
        registry.destroyEntity(entity);
        assertFalse(registry.entityExists(entity));
    }

    @Test
    void addComponentToNonExistentEntityIsNoOp() {
        registry.addComponent(99999, new Health(50));
        assertFalse(registry.entityExists(99999));
        assertNull(registry.getComponent(99999, Health.class));
    }

    @Test
    void addComponentsToNonExistentEntityIsNoOp() {
        registry.addComponents(99999, new Health(50), new Armor(10));
        assertFalse(registry.entityExists(99999));
        assertNull(registry.getComponent(99999, Health.class));
        assertNull(registry.getComponent(99999, Armor.class));
    }
}
