package org.chapzlock.core.render;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.WorldTransform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
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
    void entityWithoutHierarchyGetsWorldMatrixFromTransform() {
        int entity = Entity.create(registry);
        Transform t = Transform.at(5, 10, 15);
        WorldTransform wt = new WorldTransform();
        registry.addComponents(entity, t, wt);

        system.onUpdate(0.016f);

        Matrix4f expected = new Matrix4f().translation(5, 10, 15);
        assertTrue(matricesEqual(expected, wt.getWorldMatrix(), 0.001f));
    }

    @Test
    void childWorldMatrixIsParentTimesLocal() {
        int parent = Entity.create(registry);
        Transform parentT = Transform.at(10, 0, 0);
        WorldTransform parentWt = new WorldTransform();
        Hierarchy parentH = Hierarchy.root();
        registry.addComponents(parent, parentT, parentWt, parentH);

        int child = Entity.create(registry);
        Transform childT = Transform.at(0, 5, 0);
        WorldTransform childWt = new WorldTransform();
        Hierarchy childH = Hierarchy.childOf(parent);
        registry.addComponents(child, childT, childWt, childH);

        parentH.addChild(child);

        system.onUpdate(0.016f);

        Vector3f childWorldPos = new Vector3f();
        childWt.getWorldMatrix().getTranslation(childWorldPos);
        assertEquals(10f, childWorldPos.x, 0.001f);
        assertEquals(5f, childWorldPos.y, 0.001f);
        assertEquals(0f, childWorldPos.z, 0.001f);
    }

    @Test
    void threeLayerHierarchyPropagatesCorrectly() {
        int grandparent = Entity.create(registry);
        Transform gpT = Transform.at(1, 0, 0);
        WorldTransform gpWt = new WorldTransform();
        Hierarchy gpH = Hierarchy.root();
        registry.addComponents(grandparent, gpT, gpWt, gpH);

        int parent = Entity.create(registry);
        Transform pT = Transform.at(0, 2, 0);
        WorldTransform pWt = new WorldTransform();
        Hierarchy pH = Hierarchy.childOf(grandparent);
        registry.addComponents(parent, pT, pWt, pH);
        gpH.addChild(parent);

        int child = Entity.create(registry);
        Transform cT = Transform.at(0, 0, 3);
        WorldTransform cWt = new WorldTransform();
        Hierarchy cH = Hierarchy.childOf(parent);
        registry.addComponents(child, cT, cWt, cH);
        pH.addChild(child);

        system.onUpdate(0.016f);

        Vector3f childWorldPos = new Vector3f();
        cWt.getWorldMatrix().getTranslation(childWorldPos);
        assertEquals(1f, childWorldPos.x, 0.001f);
        assertEquals(2f, childWorldPos.y, 0.001f);
        assertEquals(3f, childWorldPos.z, 0.001f);
    }

    private boolean matricesEqual(Matrix4f a, Matrix4f b, float epsilon) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                if (Math.abs(a.get(col, row) - b.get(col, row)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }
}
