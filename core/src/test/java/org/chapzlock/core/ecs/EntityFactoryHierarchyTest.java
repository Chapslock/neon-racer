package org.chapzlock.core.ecs;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityFactoryHierarchyTest {

    private Engine engine;
    private ComponentRegistry registry;
    private EntityFactory entityFactory;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        entityFactory = engine.getEntityFactory();
    }

    @Test
    void attachChild_wiresHierarchyOnBothSides() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(1, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child);

        Hierarchy parentH = registry.getComponent(parent, Hierarchy.class);
        Hierarchy childH = registry.getComponent(child, Hierarchy.class);

        assertNotNull(parentH);
        assertNotNull(childH);
        assertTrue(parentH.isRoot());
        assertArrayEquals(new int[]{child}, parentH.getChildren());
        assertEquals(parent, childH.getParent());
        assertFalse(childH.isRoot());
    }

    @Test
    void attachChild_setsInHierarchyFlag() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(1, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child);

        Transform childTransform = registry.getComponent(child, Transform.class);
        assertTrue(childTransform.isInHierarchy());
    }

    @Test
    void attachChild_reparentsFromOldParent() {
        int oldParent = entityFactory.builder("old-parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int newParent = entityFactory.builder("new-parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(1, 0, 0))
            .instantiate();

        entityFactory.attachChild(oldParent, child);
        entityFactory.attachChild(newParent, child);

        Hierarchy oldH = registry.getComponent(oldParent, Hierarchy.class);
        Hierarchy newH = registry.getComponent(newParent, Hierarchy.class);
        Hierarchy childH = registry.getComponent(child, Hierarchy.class);

        assertEquals(0, oldH.getChildCount());
        assertArrayEquals(new int[]{child}, newH.getChildren());
        assertEquals(newParent, childH.getParent());
    }

    @Test
    void attachChild_addsHierarchyIfMissing() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(0, 0, 0))
            .instantiate();

        assertNull(registry.getComponent(parent, Hierarchy.class));
        assertNull(registry.getComponent(child, Hierarchy.class));

        entityFactory.attachChild(parent, child);

        assertNotNull(registry.getComponent(parent, Hierarchy.class));
        assertNotNull(registry.getComponent(child, Hierarchy.class));
    }
}
