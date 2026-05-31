package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.joml.Matrix4f;

/**
 * Stores the skeletal rig data needed for skinned mesh animation.
 *
 * <p><strong>What is skeletal animation?</strong> Instead of moving every vertex by hand,
 * you define a "skeleton" — a hierarchy of bones (called "joints"). Each vertex in the mesh
 * is influenced by one or more joints, weighted by how strongly each joint affects it.
 * When joints move (driven by animation), the vertices follow.
 *
 * <p><strong>What is an inverse bind matrix?</strong> The "bind pose" is the skeleton's
 * default pose (usually a T-pose). The inverse bind matrix transforms a vertex FROM
 * world space INTO the joint's local space in the bind pose. Combined with the joint's
 * current world matrix, this produces the final skin matrix:
 * {@code skinMatrix[j] = currentWorldMatrix[j] * inverseBindMatrix[j]}
 *
 * <p>This component is placed on the mesh entity that has skinned vertices.
 * The joint entities are separate entities in the hierarchy.
 *
 * @see AnimationState
 */
public class Skeleton implements Component {

    private final int[] jointEntities;
    private final Matrix4f[] inverseBindMatrices;

    /**
     * Creates a skeleton component.
     *
     * @param jointEntities entity IDs for each joint (indices match JOINTS_0 vertex attribute)
     * @param inverseBindMatrices one matrix per joint, transforming from world to joint-local bind space
     */
    public Skeleton(int[] jointEntities, Matrix4f[] inverseBindMatrices) {
        this.jointEntities = jointEntities;
        this.inverseBindMatrices = inverseBindMatrices;
    }

    /** Returns the entity IDs of all joints in this skeleton. */
    public int[] getJointEntities() {
        return jointEntities;
    }

    /** Returns the inverse bind matrices (one per joint). */
    public Matrix4f[] getInverseBindMatrices() {
        return inverseBindMatrices;
    }

    /** Returns the number of joints in this skeleton. */
    public int jointCount() {
        return jointEntities.length;
    }
}
