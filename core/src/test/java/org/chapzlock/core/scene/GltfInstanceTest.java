package org.chapzlock.core.scene;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GltfInstanceTest {

    @Test
    void entityByNameReturnsCorrectId() {
        GltfInstance instance = new GltfInstance(
            42, Map.of("Player", 42, "Camera", 43, "Wheel", 44));

        assertEquals(42, instance.entity("Player"));
        assertEquals(43, instance.entity("Camera"));
    }

    @Test
    void entityByNameThrowsOnUnknownName() {
        GltfInstance instance = new GltfInstance(42, Map.of("Player", 42));
        assertThrows(IllegalArgumentException.class, () -> instance.entity("Nonexistent"));
    }

    @Test
    void rootEntityIdAccessible() {
        GltfInstance instance = new GltfInstance(42, Map.of());
        assertEquals(42, instance.rootEntity());
    }

    @Test
    void hasEntityChecksExistence() {
        GltfInstance instance = new GltfInstance(42, Map.of("Player", 42));
        assertTrue(instance.hasEntity("Player"));
        assertFalse(instance.hasEntity("Enemy"));
    }
}
