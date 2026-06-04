package org.chapzlock.core.ecs;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformSystemTest {

    private EngineContext ctx;
    private ComponentRegistry registry;
    private TransformSystem system;

    @BeforeEach
    void setUp() {
        ctx = EngineContext.testContext();
        registry = ctx.getRegistry();
        system = new TransformSystem();
        system.onInit(ctx);
    }

    @Test
    void standaloneEntityWorldMatrixEqualsLocalMatrix() {
        int entity = Entity.create(registry);
        Transform t = Transform.at(5, 10, 15);
        registry.addComponent(entity, t);

        // No hierarchy — getWorldMatrix() returns getLocalMatrix() directly
        assertSame(t.getLocalMatrix(), t.getWorldMatrix());
    }

    @Test
    void childWorldMatrixIsParentTimesLocal() {
        int parent = Entity.create(registry);
        Transform parentT = Transform.at(10, 0, 0);
        Hierarchy parentH = Hierarchy.root();
        registry.addComponents(parent, parentT, parentH);

        int child = Entity.create(registry);
        Transform childT = Transform.at(0, 5, 0);
        Hierarchy childH = Hierarchy.childOf(parent);
        registry.addComponents(child, childT, childH);
        parentH.addChild(child);

        system.onUpdate(0.016f);

        Vector3f childWorldPos = new Vector3f();
        childT.getWorldMatrix().getTranslation(childWorldPos);
        assertEquals(10f, childWorldPos.x, 0.001f);
        assertEquals(5f, childWorldPos.y, 0.001f);
        assertEquals(0f, childWorldPos.z, 0.001f);
        assertTrue(childT.isInHierarchy());
    }

    @Test
    void threeLayerHierarchyPropagatesCorrectly() {
        int grandparent = Entity.create(registry);
        Transform gpT = Transform.at(1, 0, 0);
        Hierarchy gpH = Hierarchy.root();
        registry.addComponents(grandparent, gpT, gpH);

        int parent = Entity.create(registry);
        Transform pT = Transform.at(0, 2, 0);
        Hierarchy pH = Hierarchy.childOf(grandparent);
        registry.addComponents(parent, pT, pH);
        gpH.addChild(parent);

        int child = Entity.create(registry);
        Transform cT = Transform.at(0, 0, 3);
        Hierarchy cH = Hierarchy.childOf(parent);
        registry.addComponents(child, cT, cH);
        pH.addChild(child);

        system.onUpdate(0.016f);

        Vector3f childWorldPos = new Vector3f();
        cT.getWorldMatrix().getTranslation(childWorldPos);
        assertEquals(1f, childWorldPos.x, 0.001f);
        assertEquals(2f, childWorldPos.y, 0.001f);
        assertEquals(3f, childWorldPos.z, 0.001f);
    }

    @Test
    void rootEntityWorldMatrixEqualsLocalMatrix() {
        int root = Entity.create(registry);
        Transform t = Transform.at(7, 8, 9);
        Hierarchy h = Hierarchy.root();
        registry.addComponents(root, t, h);

        system.onUpdate(0.016f);

        assertTrue(t.isInHierarchy());
        Matrix4f expected = new Matrix4f().translation(7, 8, 9);
        assertMatrixEquals(expected, t.getWorldMatrix(), 0.001f);
    }

    private void assertMatrixEquals(Matrix4f expected, Matrix4f actual, float epsilon) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(col, row), actual.get(col, row), epsilon,
                    "Mismatch at col=" + col + ", row=" + row);
            }
        }
    }
}
