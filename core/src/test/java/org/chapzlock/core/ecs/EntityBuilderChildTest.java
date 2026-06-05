package org.chapzlock.core.ecs;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityBuilderChildTest {

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
    void child_createsChildEntityAttachedToParent() {
        int parentId = entityFactory.builder("player")
            .transform(Transform.at(0, 0, 0))
            .child("exhaust", c -> c.transform(Transform.at(0, 0, -1)))
            .instantiate();

        int childId = registry.getEntityByName("exhaust");
        assertTrue(childId >= 0);

        Hierarchy parentH = registry.getComponent(parentId, Hierarchy.class);
        Hierarchy childH = registry.getComponent(childId, Hierarchy.class);

        assertNotNull(parentH);
        assertNotNull(childH);
        assertArrayEquals(new int[]{childId}, parentH.getChildren());
        assertEquals(parentId, childH.getParent());
    }

    @Test
    void child_anonymousChildWorks() {
        int parentId = entityFactory.builder("player")
            .transform(Transform.at(0, 0, 0))
            .child(c -> c.transform(Transform.at(0, 1, 0)))
            .instantiate();

        Hierarchy parentH = registry.getComponent(parentId, Hierarchy.class);
        assertNotNull(parentH);
        assertEquals(1, parentH.getChildCount());
    }

    @Test
    void child_multipleChildrenAllAttached() {
        int parentId = entityFactory.builder("player")
            .transform(Transform.at(0, 0, 0))
            .child("left", c -> c.transform(Transform.at(-1, 0, 0)))
            .child("right", c -> c.transform(Transform.at(1, 0, 0)))
            .instantiate();

        Hierarchy parentH = registry.getComponent(parentId, Hierarchy.class);
        assertEquals(2, parentH.getChildCount());

        int leftId = registry.getEntityByName("left");
        int rightId = registry.getEntityByName("right");
        assertTrue(leftId >= 0);
        assertTrue(rightId >= 0);
    }

    @Test
    void child_nestedChildrenSupportArbitraryDepth() {
        int parentId = entityFactory.builder("root")
            .transform(Transform.at(0, 0, 0))
            .child("level1", c -> c
                .transform(Transform.at(1, 0, 0))
                .child("level2", gc -> gc
                    .transform(Transform.at(2, 0, 0))))
            .instantiate();

        int level1Id = registry.getEntityByName("level1");
        int level2Id = registry.getEntityByName("level2");

        assertTrue(level1Id >= 0);
        assertTrue(level2Id >= 0);

        Hierarchy level1H = registry.getComponent(level1Id, Hierarchy.class);
        assertEquals(parentId, level1H.getParent());
        assertArrayEquals(new int[]{level2Id}, level1H.getChildren());

        Hierarchy level2H = registry.getComponent(level2Id, Hierarchy.class);
        assertEquals(level1Id, level2H.getParent());
    }

    @Test
    void child_builderStoresRecipesBeforeInstantiation() {
        EntityBuilder builder = new EntityBuilder("test")
            .transform(Transform.at(0, 0, 0))
            .child("a", c -> c.transform(Transform.at(1, 0, 0)))
            .child("b", c -> c.transform(Transform.at(2, 0, 0)));

        assertEquals(2, builder.getChildRecipes().size());
        assertEquals("a", builder.getChildRecipes().get(0).name());
        assertEquals("b", builder.getChildRecipes().get(1).name());
    }
}
