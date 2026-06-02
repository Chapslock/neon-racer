package org.chapzlock.core.render;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.joml.Matrix4f;

/**
 * Computes world-space transformation matrices for entities in a hierarchy.
 *
 * <p>Standalone entities (no Hierarchy component) are never touched — their
 * {@link Transform#getWorldMatrix()} returns the local matrix directly. This system
 * only processes hierarchy roots and recursively propagates to all descendants.
 *
 * <p>Runs in the LOGIC phase, after animation and before physics/rendering.
 */
public class TransformSystem implements GameSystem {

    private ComponentRegistry registry;

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(Hierarchy.class, Transform.class, (entityId, hierarchy, transform) -> {
            if (hierarchy.isRoot()) {
                transform.setInHierarchy(true);
                transform.getWorldMatrix().set(transform.getLocalMatrix());
                propagateToChildren(hierarchy, transform.getWorldMatrix());
            }
        });
    }

    private void propagateToChildren(Hierarchy parentHierarchy, Matrix4f parentWorldMatrix) {
        for (int childId : parentHierarchy.getChildren()) {
            Transform childTransform = registry.getComponent(childId, Transform.class);
            if (childTransform == null) continue;

            childTransform.setInHierarchy(true);
            childTransform.getWorldMatrix()
                .set(parentWorldMatrix)
                .mul(childTransform.getLocalMatrix());

            Hierarchy childHierarchy = registry.getComponent(childId, Hierarchy.class);
            if (childHierarchy != null && childHierarchy.getChildCount() > 0) {
                propagateToChildren(childHierarchy, childTransform.getWorldMatrix());
            }
        }
    }
}
