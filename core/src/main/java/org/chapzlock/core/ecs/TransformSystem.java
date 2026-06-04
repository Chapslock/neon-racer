package org.chapzlock.core.ecs;

import java.util.HashSet;
import java.util.Set;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.logging.Log;
import org.joml.Matrix4f;

/**
 * Computes world-space transformation matrices for entities in a parent-child hierarchy.
 *
 * <p><strong>Algorithm:</strong> Each frame, iterates all entities with both {@link Hierarchy}
 * and {@link Transform}. For root entities (no parent), copies the local matrix as the world
 * matrix. Then recursively propagates down the tree: each child's world matrix = parent's
 * world matrix * child's local matrix.
 *
 * <p><strong>Important assumption:</strong> Entities without a {@link Hierarchy} component
 * never get their world matrix updated by this system. Their
 * {@link Transform#getWorldMatrix()} returns the local matrix directly (identity world
 * transform). This is correct for standalone entities that don't participate in a scene graph.
 *
 * <p>Includes cycle detection — if a hierarchy has a cycle (A→B→C→A), the cycle is broken
 * with an error log rather than causing a stack overflow.
 *
 * <p>Runs in the LOGIC phase, after animation and before physics/rendering.
 */
public class TransformSystem implements GameSystem {

    private static final Log log = Log.create(TransformSystem.class);

    private ComponentRegistry registry;
    private final Set<Integer> visited = new HashSet<>();

    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    @Override
    public void onInit(Engine engine) {
        this.registry = engine.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(Hierarchy.class, Transform.class, (entityId, hierarchy, transform) -> {
            if (hierarchy.isRoot()) {
                visited.clear();
                visited.add(entityId);
                transform.setInHierarchy(true);
                transform.getWorldMatrix().set(transform.getLocalMatrix());
                propagateToChildren(hierarchy, transform.getWorldMatrix());
            }
        });
    }

    private void propagateToChildren(Hierarchy parentHierarchy, Matrix4f parentWorldMatrix) {
        for (int childId : parentHierarchy.getChildren()) {
            if (!visited.add(childId)) {
                log.error("Cycle detected in hierarchy: entity %d is visited twice. Skipping to prevent stack overflow.", childId);
                continue;
            }

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
