package org.chapzlock.core.physics;

import javax.vecmath.Vector3f;

import org.chapzlock.core.geometry.RawMeshData;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

import lombok.experimental.UtilityClass;

/**
 * Utility class for generating CollisionShapes for meshes
 * that correctly account for the mesh’s geometric center offset.
 */
@UtilityClass
public class CollisionShapeFactory {

    /**
     * Creates a simple box collider centered correctly around the mesh geometry.
     * This is a very performant collisionShape. Use it where collision shape accuracy is not that important
     */
    public static CollisionShape createBoxShapeFromMesh(RawMeshData mesh) {
        BoundingBox bounds = computeBoundingBox(mesh);

        Vector3f halfExtents = new Vector3f(
            (bounds.max.x - bounds.min.x) / 2f,
            (bounds.max.y - bounds.min.y) / 2f,
            (bounds.max.z - bounds.min.z) / 2f
        );

        Vector3f center = new Vector3f(
            (bounds.max.x + bounds.min.x) / 2f,
            (bounds.max.y + bounds.min.y) / 2f,
            (bounds.max.z + bounds.min.z) / 2f
        );

        BoxShape box = new BoxShape(halfExtents);

        // If the mesh isn't centered at origin, offset it
        if (center.lengthSquared() > 1e-6f) {
            Transform offset = new Transform();
            offset.setIdentity();
            offset.origin.set(center);

            CompoundShape compound = new CompoundShape();
            compound.addChildShape(offset, box);
            return compound;
        }

        return box;
    }

    /**
     * Computes the AABB (axis-aligned bounding box) of a mesh.
     */
    private static BoundingBox computeBoundingBox(RawMeshData mesh) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < mesh.getPositions().length; i += 3) {
            float x = mesh.getPositions()[i];
            float y = mesh.getPositions()[i + 1];
            float z = mesh.getPositions()[i + 2];
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        }

        return new BoundingBox(
            new Vector3f(minX, minY, minZ),
            new Vector3f(maxX, maxY, maxZ)
        );
    }

    /**
     * Creates a ConvexHullShape that correctly matches the mesh’s geometric center.
     * This CollisionShape is a good balance between performance and collisionShape accuracy.
     * Use this for most CollisionShapes
     */
    public static CollisionShape createConvexHullShapeFromMesh(RawMeshData mesh) {
        BoundingBox bounds = computeBoundingBox(mesh);
        Vector3f center = new Vector3f(
            (bounds.max.x + bounds.min.x) / 2f,
            (bounds.max.y + bounds.min.y) / 2f,
            (bounds.max.z + bounds.min.z) / 2f
        );

        ObjectArrayList<Vector3f> points = new ObjectArrayList<>();
        for (int i = 0; i < mesh.getPositions().length; i += 3) {
            // Subtract the geometric center to center the shape around (0,0,0)
            float x = mesh.getPositions()[i] - center.x;
            float y = mesh.getPositions()[i + 1] - center.y;
            float z = mesh.getPositions()[i + 2] - center.z;
            points.add(new Vector3f(x, y, z));
        }

        ConvexHullShape convex = new ConvexHullShape(points);

        // If the center is offset, wrap it so the shape is placed correctly
        if (center.lengthSquared() > 1e-6f) {
            Transform offset = new Transform();
            offset.setIdentity();
            offset.origin.set(center);

            CompoundShape compound = new CompoundShape();
            compound.addChildShape(offset, convex);
            return compound;
        }

        return convex;
    }

    public static CollisionShape createStaticPlane() {
        return new StaticPlaneShape(new javax.vecmath.Vector3f(0, 1, 0), 0);
    }

    /**
     * Simple utility record to hold min/max of a mesh.
     */
    private static record BoundingBox(Vector3f min, Vector3f max) {
    }
}
