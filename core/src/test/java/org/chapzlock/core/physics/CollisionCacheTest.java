package org.chapzlock.core.physics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionCacheTest {

    private CollisionCache cache;

    @BeforeEach
    void setUp() {
        cache = new CollisionCache();
    }

    @Test
    void emptyCache_areColliding_returnsFalse() {
        assertFalse(cache.areColliding(1, 2));
    }

    @Test
    void afterRecordingContact_areColliding_returnsTrue() {
        cache.recordContact(1, 2);
        assertTrue(cache.areColliding(1, 2));
        assertTrue(cache.areColliding(2, 1));
    }

    @Test
    void afterClear_areColliding_returnsFalse() {
        cache.recordContact(1, 2);
        cache.clear();
        assertFalse(cache.areColliding(1, 2));
    }

    @Test
    void isColliding_returnsTrueIfEntityHasAnyContact() {
        cache.recordContact(1, 2);
        cache.recordContact(1, 3);
        assertTrue(cache.isColliding(1));
        assertTrue(cache.isColliding(2));
        assertFalse(cache.isColliding(99));
    }

    @Test
    void getContactEntities_returnsAllContacts() {
        cache.recordContact(5, 10);
        cache.recordContact(5, 20);
        var contacts = cache.getContactEntities(5);
        assertEquals(2, contacts.size());
        assertTrue(contacts.contains(10));
        assertTrue(contacts.contains(20));
    }

    @Test
    void getContactEntities_returnsEmptySetForUnknownEntity() {
        var contacts = cache.getContactEntities(99);
        assertTrue(contacts.isEmpty());
    }

    @Test
    void isCollidingWithLayer_checksLayerMask() {
        int layerA = 0b0010;
        int layerB = 0b0100;
        cache.recordContact(1, 2);
        cache.setEntityLayer(1, layerA);
        cache.setEntityLayer(2, layerB);

        assertTrue(cache.isCollidingWithLayer(1, 0b0100));
        assertFalse(cache.isCollidingWithLayer(1, 0b1000));
    }
}
