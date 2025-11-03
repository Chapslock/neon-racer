package org.chapzlock.core.physics;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.geometry.RawMeshData;
import org.joml.Vector3f;

import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

import lombok.experimental.UtilityClass;


/**
 * Factory utilities for creating Bullet collision shapes from RawMeshData.
 *
 * - createConvexHullShapeFromRaw: creates a ConvexHullShape from raw vertex positions.
 * - createCompoundFromRawWithOffset: wraps the base shape in a CompoundShape so you can apply an offset
 *   (useful to cancel visual pivot offsets without modifying vertex data).
 *
 * Notes:
 * - The returned shapes should be cached/reused where appropriate (MeshSystem-like caching).
 * - Convex hull creation may be slow for very large meshes; consider simplified collision meshes for complex models.
 */
@UtilityClass
public final class CollisionShapeFactory {

    /**
     * Overload: accepts an org.joml.Vector3f offset (your engine uses JOML in transforms).
     */
    public static CompoundShape createCompoundShapeWithOffset(RawMeshData raw, Vector3f jomlOffset) {
        javax.vecmath.Vector3f off = new javax.vecmath.Vector3f(jomlOffset.x, jomlOffset.y, jomlOffset.z);
        return createCompoundShapeWithOffset(raw, off);
    }

    /**
     * Wraps a collision shape produced from raw mesh data into a CompoundShape with a translation offset
     * given as javax.vecmath.Vector3f.
     *
     * The offset is the translation applied to the child shape relative to the rigid body's local origin.
     */
    public static CompoundShape createCompoundShapeWithOffset(RawMeshData raw, javax.vecmath.Vector3f offset) {
        CollisionShape base = createConvexHullShape(raw);
        CompoundShape compound = new CompoundShape();
        Transform child = new Transform();
        child.setIdentity();
        child.origin.set(offset); // place the base shape at the requested local offset
        compound.addChildShape(child, base);
        return compound;
    }

    /**
     * Create a ConvexHullShape from raw mesh positions.
     * This uses the raw positions array (assumed x,y,z triples) and will ignore normals/uvs.
     */
    public static CollisionShape createConvexHullShape(RawMeshData raw) {
        List<javax.vecmath.Vector3f> points = extractVertices(raw);
        if (points.isEmpty()) {
            throw new IllegalArgumentException("RawMeshData contains no position vertices");
        }
        // Convert to array - ConvexHullShape can accept an array of javax.vecmath.Vector3f
        ObjectArrayList<javax.vecmath.Vector3f> pts = new ObjectArrayList<>();
        pts.addAll(points);
        return new ConvexHullShape(pts);
    }

    /**
     * Extracts vertex positions from RawMeshData into a list of javax.vecmath.Vector3f.
     * If indices are present, we use indexed vertices (and keep duplicates if they are present in positions).
     */
    private static List<javax.vecmath.Vector3f> extractVertices(RawMeshData raw) {
        float[] positions = raw.getPositions();
        int[] indices = raw.getIndices();

        if (positions == null || positions.length == 0) {
            return List.of();
        }
        if (positions.length % 3 != 0) {
            throw new IllegalArgumentException("RawMeshData.positions length must be multiple of 3 (x,y,z)");
        }

        List<javax.vecmath.Vector3f> out = new ArrayList<>();

        if (indices != null && indices.length > 0) {
            // Use indexed positions (common when you want the exact used vertex set)
            for (int idx : indices) {
                int base = idx * 3;
                if (base + 2 >= positions.length) {
                    // defensive: skip invalid index
                    continue;
                }
                out.add(new javax.vecmath.Vector3f(
                    positions[base],
                    positions[base + 1],
                    positions[base + 2]
                ));
            }
        } else {
            // Non-indexed: positions is a flat array x,y,z,x,y,z...
            for (int i = 0; i < positions.length; i += 3) {
                out.add(new javax.vecmath.Vector3f(
                    positions[i],
                    positions[i + 1],
                    positions[i + 2]
                ));
            }
        }
        return out;
    }

    /**
     * Creates a triangle-mesh BVH shape from raw mesh data. This is the recommended shape for static terrain.
     * <p>
     * Uses TriangleIndexVertexArray / IndexedMesh which is the standard JBullet wrapper for triangle meshes.
     */
    public static BvhTriangleMeshShape createBvhTriangleMeshShapeFromRaw(RawMeshData raw, boolean useQuantizedAabbCompression) {
        float[] positions = raw.getPositions();
        int[] indices = raw.getIndices();

        if (positions == null || positions.length == 0) {
            throw new IllegalArgumentException("Empty positions in RawMeshData");
        }
        if (indices == null || indices.length == 0) {
            // If there are no indices, generate a simple index list assuming positions are triangles (x,y,z repeated per vertex)
            int triCount = positions.length / 9; // 3 vertices per triangle, 3 floats per vertex => 9 floats per triangle
            if (triCount <= 0) {
                throw new IllegalArgumentException("RawMeshData has no indices and cannot be interpreted as triangles");
            }
            int[] autoIndices = new int[triCount * 3];
            int write = 0;
            for (int t = 0; t < triCount; t++) {
                autoIndices[write++] = t * 3;
                autoIndices[write++] = t * 3 + 1;
                autoIndices[write++] = t * 3 + 2;
            }
            indices = autoIndices;
        }

        // Create an IndexedMesh and fill it
        IndexedMesh mesh = new IndexedMesh();
        mesh.numTriangles = indices.length / 3;
        mesh.numVertices = positions.length / 3;
        mesh.triangleIndexBase = java.nio.ByteBuffer
            .allocateDirect(indices.length * Integer.BYTES)
            .order(java.nio.ByteOrder.nativeOrder());
        mesh.triangleIndexBase.asIntBuffer().put(indices);
        mesh.triangleIndexStride = 3 * Integer.BYTES;

        mesh.vertexBase = java.nio.ByteBuffer
            .allocateDirect(positions.length * Float.BYTES)
            .order(java.nio.ByteOrder.nativeOrder());
        mesh.vertexBase.asFloatBuffer().put(positions);
        mesh.vertexStride = 3 * Float.BYTES;

        TriangleIndexVertexArray triangleIndexVertexArray = new TriangleIndexVertexArray();
        triangleIndexVertexArray.addIndexedMesh(mesh, ScalarType.INTEGER);

        return new BvhTriangleMeshShape(triangleIndexVertexArray, useQuantizedAabbCompression);
    }
}
