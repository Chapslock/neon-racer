package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HierarchyTest {

    @Test
    void rootNodeHasNoParentAndNoChildren() {
        Hierarchy h = Hierarchy.root();
        assertEquals(-1, h.getParent());
        assertEquals(0, h.getChildren().length);
        assertTrue(h.isRoot());
    }

    @Test
    void childNodeStoresParentId() {
        Hierarchy h = Hierarchy.childOf(42);
        assertEquals(42, h.getParent());
        assertFalse(h.isRoot());
    }

    @Test
    void addChildUpdatesChildrenArray() {
        Hierarchy h = Hierarchy.root();
        h.addChild(10);
        h.addChild(20);
        assertArrayEquals(new int[]{10, 20}, h.getChildren());
    }

    @Test
    void removeChildUpdatesChildrenArray() {
        Hierarchy h = Hierarchy.root();
        h.addChild(10);
        h.addChild(20);
        h.addChild(30);
        h.removeChild(20);
        assertArrayEquals(new int[]{10, 30}, h.getChildren());
    }

    @Test
    void removeNonexistentChildIsNoOp() {
        Hierarchy h = Hierarchy.root();
        h.addChild(10);
        h.removeChild(99);
        assertArrayEquals(new int[]{10}, h.getChildren());
    }
}
