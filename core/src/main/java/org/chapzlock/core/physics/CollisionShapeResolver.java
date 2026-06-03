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

    private CollisionShape resolveConvexHull(CollisionGeometry.ConvexHull hull, RawMeshData meshData) {
        if (meshData == null) {
            throw new IllegalStateException(
                "ConvexHull geometry requires mesh data but none was available. " +
                "Ensure the entity has a Mesh component or glTF model loaded before physics registration.");
        }
        return CollisionShapeFactory.createConvexHullShape(meshData);
    }

    private CollisionShape resolveTriangleMesh(RawMeshData meshData) {
        if (meshData == null) {
            throw new IllegalStateException(
                "TriangleMesh geometry requires mesh data but none was available. " +
                "Ensure the entity has a Mesh component or glTF model loaded before physics registration.");
        }
        return CollisionShapeFactory.createBvhTriangleMeshShapeFromRaw(meshData, true);
    }

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
