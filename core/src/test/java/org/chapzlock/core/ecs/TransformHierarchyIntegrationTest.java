package org.chapzlock.core.ecs;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformHierarchyIntegrationTest {

    private Engine engine;
    private ComponentRegistry registry;
    private EntityFactory entityFactory;
    private TransformSystem transformSystem;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        entityFactory = engine.getEntityFactory();
        transformSystem = new TransformSystem();
        transformSystem.onInit(engine);
    }

    @Test
    void childWorldTransformFollowsParent() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(10, 0, 0))
            .child("child", c -> c.transform(Transform.at(5, 0, 0)))
            .instantiate();

        int child = registry.getEntityByName("child");

        // Simulate one frame
        transformSystem.onUpdate(0.016f);

        Transform childT = registry.getComponent(child, Transform.class);
        // Child world position = parent(10,0,0) + child_local(5,0,0) = (15,0,0)
        assertEquals(15f, childT.getWorldMatrix().m30(), 0.001f);
        assertEquals(0f, childT.getWorldMatrix().m31(), 0.001f);
        assertEquals(0f, childT.getWorldMatrix().m32(), 0.001f);
    }

    @Test
    void movingParentMovesChild() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(0, 0, 0))
            .child("child", c -> c.transform(Transform.at(5, 0, 0)))
            .instantiate();

        int child = registry.getEntityByName("child");

        transformSystem.onUpdate(0.016f);

        // Move parent
        Transform parentT = registry.getComponent(parent, Transform.class);
        parentT.setPosition(new Vector3f(20, 0, 0));

        transformSystem.onUpdate(0.016f);

        Transform childT = registry.getComponent(child, Transform.class);
        // Child world position = parent(20,0,0) + child_local(5,0,0) = (25,0,0)
        assertEquals(25f, childT.getWorldMatrix().m30(), 0.001f);
    }

    @Test
    void deepHierarchyPropagatesCorrectly() {
        int root = entityFactory.builder("root")
            .transform(Transform.at(1, 0, 0))
            .child("mid", c -> c
                .transform(Transform.at(2, 0, 0))
                .child("leaf", gc -> gc.transform(Transform.at(3, 0, 0))))
            .instantiate();

        transformSystem.onUpdate(0.016f);

        int leaf = registry.getEntityByName("leaf");
        Transform leafT = registry.getComponent(leaf, Transform.class);
        // leaf world position = 1 + 2 + 3 = 6
        assertEquals(6f, leafT.getWorldMatrix().m30(), 0.001f);
    }

    @Test
    void runtimeAttachChildFollowsParent() {
        int parent = entityFactory.builder("parent")
            .transform(Transform.at(10, 0, 0))
            .instantiate();
        int child = entityFactory.builder("child")
            .transform(Transform.at(3, 0, 0))
            .instantiate();

        entityFactory.attachChild(parent, child);
        transformSystem.onUpdate(0.016f);

        Transform childT = registry.getComponent(child, Transform.class);
        assertEquals(13f, childT.getWorldMatrix().m30(), 0.001f);
    }
}
