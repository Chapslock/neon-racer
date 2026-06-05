package org.chapzlock.core.physics;

import org.chapzlock.core.asset.RawMeshData;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.linearmath.Transform;

/**
 * Converts {@link CollisionGeometry} descriptors into JBullet {@link CollisionShape} objects.
 *
 * <h3>When this runs</h3>
 * <p>Shape resolution happens when {@link PhysicsSystem} drains its pending registration
 * queue at the start of {@code onUpdate()}. By this point in the frame, all scene loading
 * (including glTF child entity creation) is complete, guaranteeing that mesh data is
 * available for mesh-derived shapes.</p>
 *
 * <h3>Why mesh-derived shapes need raw vertex data</h3>
 * <p>A ConvexHull shape is computed by finding the smallest convex polyhedron that
 * encloses all vertices of a mesh. A TriangleMesh shape uses the exact triangle geometry.
 * Both need access to the original vertex positions (not the GPU-uploaded buffers, which
 * are inaccessible from the CPU after upload).</p>
 *
 * @see CollisionGeometry
 * @see PhysicsWorld
 */
class CollisionShapeResolver {

    /**
     * Resolves a {@link CollisionGeometry} descriptor into a concrete JBullet {@link CollisionShape}.
     *
     * <p>The mapping from geometry types to JBullet shapes is:</p>
     * <ul>
     *   <li>{@link CollisionGeometry.Box} to {@link BoxShape} — axis-aligned box defined by half-extents</li>
     *   <li>{@link CollisionGeometry.Sphere} to {@link SphereShape} — sphere defined by radius</li>
     *   <li>{@link CollisionGeometry.Capsule} to {@link CapsuleShape} — cylinder capped with hemispheres</li>
     *   <li>{@link CollisionGeometry.ConvexHull} to {@link com.bulletphysics.collision.shapes.ConvexHullShape}
     *       — smallest convex polyhedron enclosing the mesh vertices (requires mesh data)</li>
     *   <li>{@link CollisionGeometry.TriangleMesh} to {@link com.bulletphysics.collision.shapes.BvhTriangleMeshShape}
     *       — exact triangle geometry with BVH acceleration (requires mesh data, static only)</li>
     *   <li>{@link CollisionGeometry.Compound} to {@link CompoundShape} — multiple child shapes each
     *       with their own local offset, forming a complex composite collider</li>
     *   <li>{@link CollisionGeometry.Offset} to {@link CompoundShape} — wraps a single inner shape
     *       with a translation offset (used to shift the collision center away from the visual origin)</li>
     * </ul>
     *
     * @param geometry the collision geometry descriptor specifying the desired shape type and parameters
     * @param meshData the raw mesh data needed for mesh-derived shapes (ConvexHull, TriangleMesh);
     *                 may be {@code null} for primitive shapes (Box, Sphere, Capsule)
     * @return the resolved JBullet collision shape ready for use in a rigid body
     * @throws IllegalStateException if a mesh-derived shape is requested but meshData is null
     */
    CollisionShape resolve(CollisionGeometry geometry, RawMeshData meshData) {
        return switch (geometry) {
            case CollisionGeometry.Box b ->
                new BoxShape(PhysicsUtil.toVecmath(b.halfExtents()));
            case CollisionGeometry.Sphere s ->
                new SphereShape(s.radius());
            case CollisionGeometry.Capsule c ->
                new CapsuleShape(c.radius(), c.height());
            case CollisionGeometry.ConvexHull h ->
                resolveConvexHull(h, meshData);
            case CollisionGeometry.TriangleMesh t ->
                resolveTriangleMesh(meshData);
            case CollisionGeometry.Compound c ->
                resolveCompound(c, meshData);
            case CollisionGeometry.Offset o ->
                resolveOffset(o, meshData);
        };
    }

    /**
     * Resolves a convex hull shape from the entity's mesh vertex data.
     *
     * @param hull     the convex hull geometry descriptor
     * @param meshData the raw mesh data providing vertex positions for hull computation
     * @return a convex hull collision shape
     * @throws IllegalStateException if meshData is null
     */
    private CollisionShape resolveConvexHull(CollisionGeometry.ConvexHull hull, RawMeshData meshData) {
        if (meshData == null) {
            throw new IllegalStateException(
                "ConvexHull geometry requires mesh data but none was available. " +
                "Ensure the entity has a Mesh component or glTF model loaded before physics registration.");
        }
        return CollisionShapeFactory.createConvexHullShape(meshData);
    }

    /**
     * Resolves an exact triangle mesh shape with BVH acceleration for static geometry.
     *
     * @param meshData the raw mesh data providing vertex positions and indices
     * @return a BVH triangle mesh collision shape
     * @throws IllegalStateException if meshData is null
     */
    private CollisionShape resolveTriangleMesh(RawMeshData meshData) {
        if (meshData == null) {
            throw new IllegalStateException(
                "TriangleMesh geometry requires mesh data but none was available. " +
                "Ensure the entity has a Mesh component or glTF model loaded before physics registration.");
        }
        return CollisionShapeFactory.createBvhTriangleMeshShapeFromRaw(meshData, true);
    }

    /**
     * Resolves a compound shape by recursively resolving each child geometry and positioning
     * it at the specified local offset within the compound.
     *
     * <p>Compound shapes are used when an entity's collision volume cannot be represented by
     * a single primitive — for example, a car body (box) with wheel arches (additional boxes)
     * or a character with separate torso and head colliders.</p>
     *
     * @param compound the compound geometry descriptor listing positioned child shapes
     * @param meshData the raw mesh data passed to children that need it (e.g., convex hull children)
     * @return a compound collision shape containing all resolved children
     */
    private CollisionShape resolveCompound(CollisionGeometry.Compound compound, RawMeshData meshData) {
        CompoundShape compoundShape = new CompoundShape();
        for (CollisionGeometry.Positioned child : compound.children()) {
            CollisionShape childShape = resolve(child.shape(), meshData);
            Transform childTransform = new Transform();
            childTransform.setIdentity();
            childTransform.origin.set(PhysicsUtil.toVecmath(child.offset()));
            compoundShape.addChildShape(childTransform, childShape);
        }
        return compoundShape;
    }

    /**
     * Resolves an offset shape by wrapping the inner geometry in a compound with a single
     * translated child. This shifts the collision shape's effective center relative to the
     * rigid body origin without modifying the underlying shape.
     *
     * <p>Common use case: a model whose visual origin is at the bottom (feet), but the physics
     * center-of-mass should be at the geometric center. The offset shifts the shape down by
     * half the model height.</p>
     *
     * @param offset   the offset geometry descriptor containing the inner shape and translation
     * @param meshData the raw mesh data passed to the inner shape if it requires it
     * @return a compound shape with the inner shape positioned at the specified offset
     */
    private CollisionShape resolveOffset(CollisionGeometry.Offset offset, RawMeshData meshData) {
        CollisionShape innerShape = resolve(offset.inner(), meshData);
        CompoundShape compound = new CompoundShape();
        Transform childTransform = new Transform();
        childTransform.setIdentity();
        childTransform.origin.set(PhysicsUtil.toVecmath(offset.offset()));
        compound.addChildShape(childTransform, innerShape);
        return compound;
    }
}
