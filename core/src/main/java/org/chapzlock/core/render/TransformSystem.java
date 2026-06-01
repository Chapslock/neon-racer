package org.chapzlock.core.render;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Hierarchy;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.component.WorldTransform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.EntityView;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.ecs.SystemPhase;
import org.joml.Matrix4f;

/**
 * TransformSystem is responsible for computing world-space transformation matrices for all entities
 * in the game. It converts local transforms (relative to a parent) into world transforms (relative
 * to the game world origin).
 * <p>
 * <b>Why This Exists:</b> In a scene graph or hierarchy, entities can be children of other entities.
 * For example, a character's hand is a child of their arm, which is a child of their body. Each of
 * these entities has a Transform component that describes its position, rotation, and scale relative
 * to its parent. But when we render or do physics, we need to know where each entity is in world space.
 * This system performs that conversion by multiplying parent and child transformation matrices together.
 * </p>
 * <p>
 * <b>How It Works:</b> The system processes entities in two groups:
 * <ul>
 *   <li><b>Standalone entities</b> (Transform + WorldTransform, no Hierarchy): These have no parent,
 *       so their world matrix is just a copy of their local matrix.</li>
 *   <li><b>Hierarchical entities</b> (Transform + WorldTransform + Hierarchy): For root entities
 *       (those with no parent), the world matrix equals the local matrix. Then the system recursively
 *       processes all children, multiplying each child's local matrix by its parent's world matrix
 *       to get the child's world matrix.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Example:</b> If a parent entity is at position (10, 0, 0) and a child is at position (0, 5, 0)
 * relative to its parent, the child's world position will be (10, 5, 0).
 * </p>
 * <p>
 * This system runs in the LOGIC phase, before rendering and physics, so that all other systems can
 * use up-to-date world positions.
 * </p>
 */
public class TransformSystem implements GameSystem {

    private ComponentRegistry registry;

    /**
     * Specifies when this system should run in the game loop. TransformSystem runs in the LOGIC phase,
     * which occurs after input processing but before physics and rendering. This ensures that all
     * world matrices are up-to-date before other systems need them.
     *
     * @return SystemPhase.LOGIC
     */
    @Override
    public SystemPhase phase() {
        return SystemPhase.LOGIC;
    }

    /**
     * Initializes the TransformSystem by storing a reference to the component registry. The registry
     * is used to query entities and their components during the update phase.
     *
     * @param ctx the engine context containing all core engine systems and data
     */
    @Override
    public void onInit(EngineContext ctx) {
        this.registry = ctx.getRegistry();
    }

    /**
     * Updates all world transformation matrices every frame. This method processes entities in two passes:
     * <ol>
     *   <li>Standalone entities (no hierarchy): Copy local matrix to world matrix directly.</li>
     *   <li>Hierarchical root entities: Copy local matrix to world matrix, then recursively propagate
     *       to all descendants.</li>
     * </ol>
     * <p>
     * The recursive propagation ensures that if you have a three-level hierarchy (grandparent → parent → child),
     * each level accumulates the transformations of all its ancestors. This means moving the grandparent
     * automatically moves the parent and child along with it.
     * </p>
     *
     * @param deltaTime time elapsed since the last frame, in seconds (not used, but required by the interface)
     */
    @Override
    public void onUpdate(float deltaTime) {
        // Pass 1: Update standalone entities (those without a Hierarchy component)
        // These entities have no parent, so their world matrix is just their local matrix
        for (EntityView entity : registry.view(Transform.class, WorldTransform.class)) {
            Hierarchy hierarchy = registry.getComponent(entity.getId(), Hierarchy.class);
            if (hierarchy == null) {
                Transform transform = entity.get(Transform.class);
                WorldTransform worldTransform = entity.get(WorldTransform.class);
                worldTransform.getWorldMatrix().set(transform.getTransformationMatrix());
            }
        }

        // Pass 2: Update hierarchical entities starting from roots
        // Root entities are those with a Hierarchy component where isRoot() returns true
        for (EntityView entity : registry.view(Hierarchy.class, Transform.class, WorldTransform.class)) {
            Hierarchy hierarchy = entity.get(Hierarchy.class);
            if (hierarchy.isRoot()) {
                Transform transform = entity.get(Transform.class);
                WorldTransform worldTransform = entity.get(WorldTransform.class);

                // Root entities have no parent, so world matrix = local matrix
                worldTransform.getWorldMatrix().set(transform.getTransformationMatrix());

                // Now recursively update all children of this root
                propagateToChildren(hierarchy, worldTransform.getWorldMatrix());
            }
        }
    }

    /**
     * Recursively propagates world transformation matrices from a parent to all its children and their
     * descendants. For each child, this method computes the child's world matrix by multiplying the
     * parent's world matrix by the child's local matrix.
     * <p>
     * <b>Math:</b> childWorldMatrix = parentWorldMatrix × childLocalMatrix
     * </p>
     * <p>
     * This multiplication order is important. In matrix math, transformations are applied right-to-left,
     * so we first apply the child's local transform, then apply the parent's world transform. This gives
     * us the child's position in world space.
     * </p>
     * <p>
     * After updating a child's world matrix, the method checks if that child has its own children. If so,
     * it recursively calls itself to propagate the transformation down the hierarchy tree.
     * </p>
     *
     * @param parentHierarchy the Hierarchy component of the parent entity
     * @param parentWorldMatrix the parent entity's world transformation matrix
     */
    private void propagateToChildren(Hierarchy parentHierarchy, Matrix4f parentWorldMatrix) {
        // Iterate through all child entity IDs stored in the parent's Hierarchy component
        for (int childId : parentHierarchy.getChildren()) {
            // Fetch the child's Transform and WorldTransform components
            Transform childTransform = registry.getComponent(childId, Transform.class);
            WorldTransform childWorldTransform = registry.getComponent(childId, WorldTransform.class);

            // Skip this child if it doesn't have the required components (safety check)
            if (childTransform == null || childWorldTransform == null) {
                continue;
            }

            // Compute child's world matrix: parent world × child local
            // We use set() to copy the parent matrix, then mul() to multiply by the child's local matrix
            childWorldTransform.getWorldMatrix()
                .set(parentWorldMatrix)
                .mul(childTransform.getTransformationMatrix());

            // If this child has children of its own, recursively propagate to them
            Hierarchy childHierarchy = registry.getComponent(childId, Hierarchy.class);
            if (childHierarchy != null && childHierarchy.getChildCount() > 0) {
                propagateToChildren(childHierarchy, childWorldTransform.getWorldMatrix());
            }
        }
    }
}
