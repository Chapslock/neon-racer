package org.chapzlock.core.physics;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.asset.RawMeshData;
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
 * Factory for creating JBullet {@link CollisionShape} objects from engine-side {@link RawMeshData}.
 *
 * <p>This class converts raw vertex/index data into physics collision primitives. It supports
 * two fundamental shape types:</p>
 * <ul>
 *   <li><strong>ConvexHullShape</strong> — the smallest convex polyhedron enclosing all mesh vertices.
 *       Suitable for dynamic rigid bodies (vehicles, props, characters).</li>
 *   <li><strong>BvhTriangleMeshShape</strong> — an exact triangle mesh wrapped in a bounding-volume
 *       hierarchy for efficient ray/contact tests. Suitable for static geometry (terrain, walls)
 *       where concave surfaces must be represented exactly.</li>
 * </ul>
 *
 * <p>Additionally, {@code createCompoundShapeWithOffset} wraps any generated shape inside a
 * {@link CompoundShape} with a translation offset. This is used to cancel visual pivot offsets
 * (e.g., a model whose origin is at the bottom of its feet rather than its center of mass)
 * without modifying vertex data.</p>
 *
 * <h3>Performance considerations</h3>
 * <p>Convex hull and BVH construction involve full mesh traversal and are expensive. Results
 * should be cached (see {@link CollisionCache}) and reused across entities that share the same
 * mesh. For complex visual models, consider authoring a simplified collision mesh in your DCC
 * tool rather than using the full-detail render mesh.</p>
 *
 * @see CollisionShapeResolver
 * @see CollisionGeometry
 */
@UtilityClass
final class CollisionShapeFactory {

    /**
     * Creates a compound shape containing a convex hull derived from the given mesh, offset by
     * the specified translation. This overload accepts a JOML vector, which is the standard
     * vector type used throughout the engine's transform system.
     *
     * @param raw        the raw mesh data whose vertices define the convex hull
     * @param jomlOffset the local-space translation to apply to the child shape relative to
     *                   the rigid body's origin (e.g., negated centroid to center the shape)
     * @return a compound shape with a single child positioned at the given offset
     */
    static CompoundShape createCompoundShapeWithOffset(RawMeshData raw, Vector3f jomlOffset) {
        javax.vecmath.Vector3f off = new javax.vecmath.Vector3f(jomlOffset.x, jomlOffset.y, jomlOffset.z);
        return createCompoundShapeWithOffset(raw, off);
    }

    /**
     * Creates a compound shape containing a convex hull derived from the given mesh, offset by
     * the specified translation. This overload accepts a vecmath vector for direct interop with
     * JBullet APIs.
     *
     * @param raw    the raw mesh data whose vertices define the convex hull
     * @param offset the local-space translation applied to the child shape relative to the
     *               rigid body's origin, in JBullet's vecmath format
     * @return a compound shape with a single child positioned at the given offset
     */
    static CompoundShape createCompoundShapeWithOffset(RawMeshData raw, javax.vecmath.Vector3f offset) {
        CollisionShape base = createConvexHullShape(raw);
        CompoundShape compound = new CompoundShape();
        Transform child = new Transform();
        child.setIdentity();
        child.origin.set(offset); // place the base shape at the requested local offset
        compound.addChildShape(child, base);
        return compound;
    }

    /**
     * Creates a {@link ConvexHullShape} from raw mesh vertex positions.
     *
     * <p>The convex hull is the smallest convex polyhedron that encloses all vertices. Only
     * the position data (x, y, z triples) is used; normals, UVs, and other vertex attributes
     * are ignored.</p>
     *
     * @param raw the raw mesh data containing at least one vertex position
     * @return a convex hull collision shape enclosing all vertices
     * @throws IllegalArgumentException if the mesh contains no position data
     */
    static CollisionShape createConvexHullShape(RawMeshData raw) {
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
     * Extracts vertex positions from raw mesh data into a list of vecmath vectors suitable
     * for JBullet shape construction.
     *
     * <p>If index data is present, only indexed vertices are extracted (which may include
     * duplicates). Otherwise, all positions are treated as a flat x,y,z sequence.</p>
     *
     * @param raw the raw mesh data to extract positions from
     * @return a list of vertex positions in JBullet's vecmath format, or an empty list if no
     *         positions exist
     * @throws IllegalArgumentException if the positions array length is not a multiple of 3
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
     * Creates a {@link BvhTriangleMeshShape} from raw mesh data, suitable for static (non-moving)
     * collision geometry such as terrain, walls, and track surfaces.
     *
     * <p>Unlike convex hulls, BVH triangle meshes preserve concave detail — meaning holes,
     * overhangs, and tunnels are represented exactly. The BVH (bounding volume hierarchy) tree
     * built internally by JBullet accelerates collision queries against the potentially large
     * triangle count.</p>
     *
     * <p>If no index data is present in the mesh, sequential triangle indices are generated
     * automatically (every 3 consecutive vertices form a triangle).</p>
     *
     * @param raw                          the raw mesh data containing positions and optionally indices
     * @param useQuantizedAabbCompression  whether to use quantized AABB compression in the BVH tree;
     *                                     {@code true} reduces memory usage at a small precision cost
     *                                     (recommended for most static geometry)
     * @return a BVH triangle mesh collision shape
     * @throws IllegalArgumentException if positions are empty or cannot be interpreted as triangles
     */
    static BvhTriangleMeshShape createBvhTriangleMeshShapeFromRaw(RawMeshData raw, boolean useQuantizedAabbCompression) {
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
