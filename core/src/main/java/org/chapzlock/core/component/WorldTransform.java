package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Matrix4f;

/**
 * Holds the computed world-space transformation matrix for an entity.
 *
 * <p>In a scene hierarchy, each entity's {@link Transform} is in <em>local space</em>
 * (relative to its parent). The engine needs a final <em>world-space</em> matrix for
 * rendering and physics. That's what this component stores.
 *
 * <p>The {@link org.chapzlock.core.render.TransformSystem} computes this every frame by
 * walking the hierarchy tree: for root entities, world = local. For child entities,
 * world = parent's world matrix multiplied by local matrix.
 *
 * <p>If an entity has no {@link Hierarchy} component, TransformSystem uses its
 * {@link Transform#getTransformationMatrix()} directly as the world matrix.
 *
 * <p><strong>Do not modify this component directly from game code</strong> — it is
 * recomputed each frame by TransformSystem. Modify the entity's {@link Transform}
 * instead and let the system propagate the change.
 *
 * @see Hierarchy
 * @see Transform
 * @see org.chapzlock.core.render.TransformSystem
 */
public class WorldTransform implements Component {

    private final Matrix4f worldMatrix = new Matrix4f();

    /**
     * Returns the world-space transformation matrix.
     * <p>
     * This matrix includes the accumulated translation, rotation, and scale of this entity
     * AND all of its ancestors in the hierarchy. Use this for rendering and physics — never
     * use Transform.getTransformationMatrix() directly when hierarchy is involved.
     *
     * @return the computed world matrix (do not cache — it may change each frame)
     */
    public Matrix4f getWorldMatrix() {
        return worldMatrix;
    }

    /**
     * Sets the world matrix. Called by TransformSystem each frame — do not call from game code.
     *
     * @param matrix the computed world matrix to store
     */
    public void set(Matrix4f matrix) {
        this.worldMatrix.set(matrix);
    }
}
