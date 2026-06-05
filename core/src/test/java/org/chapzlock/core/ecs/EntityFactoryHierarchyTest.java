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

    @Test
    void detachChild_removesFromParentAndSetsNoParent() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(5, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(1, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child);
        entityFactory.detachChild(child);

        Hierarchy parentH = registry.getComponent(parent, Hierarchy.class);
        Hierarchy childH = registry.getComponent(child, Hierarchy.class);

        assertEquals(0, parentH.getChildCount());
        assertEquals(Hierarchy.NO_PARENT, childH.getParent());
        assertTrue(childH.isRoot());
    }

    @Test
    void detachChild_bakesWorldTransformIntoLocal() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(10, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(5, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child);

        // Simulate TransformSystem propagation: worldMatrix = parent.local * child.local
        Transform parentT = registry.getComponent(parent, Transform.class);
        Transform childT = registry.getComponent(child, Transform.class);
        parentT.getWorldMatrix().set(parentT.getLocalMatrix());
        childT.getWorldMatrix().set(parentT.getWorldMatrix()).mul(childT.getLocalMatrix());

        entityFactory.detachChild(child);

        // After detach, child's local position should be baked to world position (15, 0, 0)
        assertEquals(15f, childT.getX(), 0.001f);
        assertEquals(0f, childT.getY(), 0.001f);
        assertEquals(0f, childT.getZ(), 0.001f);
    }

    @Test
    void detachAllChildren_removesAllChildren() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(0, 0, 0))
            .instantiate();
        int child1 = entityFactory.builder("child1")
            .transform(Transform.at(1, 0, 0))
            .instantiate();
        int child2 = entityFactory.builder("child2")
            .transform(Transform.at(2, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child1);
        entityFactory.attachChild(parent, child2);

        entityFactory.detachAllChildren(parent);

        Hierarchy parentH = registry.getComponent(parent, Hierarchy.class);
        Hierarchy child1H = registry.getComponent(child1, Hierarchy.class);
        Hierarchy child2H = registry.getComponent(child2, Hierarchy.class);

        assertEquals(0, parentH.getChildCount());
        assertTrue(child1H.isRoot());
        assertTrue(child2H.isRoot());
    }
}
