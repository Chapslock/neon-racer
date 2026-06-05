package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

import org.chapzlock.core.asset.RawMeshData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import lombok.experimental.UtilityClass;

/**
 * Conversion utilities between JOML math types and javax.vecmath types used by JBullet.
 *
 * <h3>Why two math libraries?</h3>
 * <p>The engine uses <a href="https://github.com/JOML-CI/JOML">JOML</a> (Java OpenGL Math Library)
 * for all rendering and transform math — it is modern, well-optimized, and designed for OpenGL/Vulkan
 * workflows. However, JBullet (the Java port of Bullet Physics) predates JOML and uses the older
 * {@code javax.vecmath} package for its vector/quaternion types.</p>
 *
 * <p>Rather than forking JBullet to use JOML (which would prevent upstream updates), this utility
 * class provides zero-allocation-style converters at the physics boundary. Conversions happen only
 * when data crosses between the physics world and the engine's transform/rendering systems.</p>
 *
 * <p>This class also provides mesh geometry helpers (centroid, AABB center, offset computation)
 * that assist in aligning collision shapes with visual meshes.</p>
 *
 * @see PhysicsSystem
 * @see CollisionShapeFactory
 */
@UtilityClass
public class PhysicsUtil {

    /**
     * Converts Euler angles in degrees to a JBullet quaternion.
     *
     * <p>Rotation order is X then Y then Z (intrinsic). This matches the convention used by
     * the engine's {@code Transform} component for Euler-based rotation.</p>
     *
     * @param eulerDeg a vector containing rotation angles in degrees (x=pitch, y=yaw, z=roll)
     * @return the equivalent rotation as a JBullet {@link Quat4f}
     */
    public static Quat4f eulerDegreesToQuat4f(Vector3f eulerDeg) {
        // Use JOML quaternion conversion to avoid re-implementing: build quaternion from radians then convert to Quat4f
        float rx = (float) Math.toRadians(eulerDeg.x);
        float ry = (float) Math.toRadians(eulerDeg.y);
        float rz = (float) Math.toRadians(eulerDeg.z);
        Quaternionf jq = new Quaternionf().identity().rotateX(rx).rotateY(ry).rotateZ(rz);
        return toQuat4f(jq);
    }

    /**
     * Converts a JOML quaternion to JBullet's vecmath quaternion format.
     *
     * @param q the JOML quaternion (x, y, z, w)
     * @return the equivalent {@link Quat4f} for use with JBullet APIs
     */
    public static Quat4f toQuat4f(Quaternionf q) {
        // javax.vecmath.Quat4f is (x, y, z, w)
        return new Quat4f(q.x, q.y, q.z, q.w);
    }

    /**
     * Converts a JBullet quaternion to Euler angles in degrees.
     *
     * <p>Uses the same decomposition as the engine's {@code Transform} component, producing
     * angles in the range suitable for direct assignment to transform rotation fields.</p>
     *
     * @param q4f the JBullet quaternion to decompose
     * @return a JOML vector containing Euler angles in degrees (x=pitch, y=yaw, z=roll)
     */
    public static Vector3f quat4fToEulerDegrees(Quat4f q4f) {
        Quaternionf jq = toQuaternionf(q4f);
        // compute euler as in Transform.eulerFromQuat()
        float x = jq.x;
        float y = jq.y;
        float z = jq.z;
        float w = jq.w;

        double t0 = 2.0 * (w * x + y * z);
        double t1 = 1.0 - 2.0 * (x * x + y * y);
        double rx = Math.atan2(t0, t1);

        double t2 = 2.0 * (w * y - z * x);
        t2 = Math.max(-1.0, Math.min(1.0, t2));
        double ry = Math.asin(t2);

        double t3 = 2.0 * (w * z + x * y);
        double t4 = 1.0 - 2.0 * (y * y + z * z);
        double rz = Math.atan2(t3, t4);

        return new Vector3f(
            (float) Math.toDegrees(rx),
            (float) Math.toDegrees(ry),
            (float) Math.toDegrees(rz)
        );
    }

    /**
     * Converts a JBullet vecmath quaternion to a JOML quaternion.
     *
     * @param q the JBullet quaternion (x, y, z, w)
     * @return a new JOML {@link Quaternionf} with the same rotation
     */
    public static Quaternionf toQuaternionf(Quat4f q) {
        return new Quaternionf(q.x, q.y, q.z, q.w);
    }


    /**
     * Computes a translation offset that, when applied to a collision shape, shifts the mesh so
     * that its centroid (arithmetic mean of all vertices) becomes the rigid body's local origin.
     *
     * <p>This is useful when a model's visual origin does not coincide with its center of mass.
     * For example, if a mesh's centroid is at (1, 2, 3), this method returns (-1, -2, -3) so the
     * collision shape is shifted to place the centroid at (0, 0, 0) in body-local space.</p>
     *
     * @param raw the raw mesh data to compute the centroid from
     * @return the negated centroid as a vecmath vector, suitable for use as a CompoundShape child offset
     */
    public static javax.vecmath.Vector3f computeOffsetToCenterByCentroid(RawMeshData raw) {
        javax.vecmath.Vector3f centroid = computeCentroid(raw);
        return new javax.vecmath.Vector3f(-centroid.x, -centroid.y, -centroid.z);
    }

    /**
     * Computes the centroid (arithmetic mean of all vertex positions) of the given mesh.
     *
     * <p>If index data is present, indexed vertices are used (which may weight some positions
     * more heavily if they appear in multiple triangles). Returns a vecmath vector for direct
     * use with JBullet transforms.</p>
     *
     * @param raw the raw mesh data containing position and optionally index data
     * @return the centroid position in vecmath format, or (0,0,0) if no vertices exist
     * @throws IllegalArgumentException if positions array length is not a multiple of 3
     */
    public static javax.vecmath.Vector3f computeCentroid(RawMeshData raw) {
        float[] positions = raw.getPositions();
        int[] indices = raw.getIndices();

        if (positions == null || positions.length == 0) {
            return new javax.vecmath.Vector3f(0f, 0f, 0f);
        }
        if (positions.length % 3 != 0) {
            throw new IllegalArgumentException("RawMeshData.positions length must be a multiple of 3");
        }

        double sx = 0, sy = 0, sz = 0;
        int count = 0;

        if (indices != null && indices.length > 0) {
            for (int idx : indices) {
                int base = idx * 3;
                if (base + 2 >= positions.length) {
                    continue;
                }
                sx += positions[base];
                sy += positions[base + 1];
                sz += positions[base + 2];
                count++;
            }
        } else {
            count = positions.length / 3;
            for (int i = 0; i < positions.length; i += 3) {
                sx += positions[i];
                sy += positions[i + 1];
                sz += positions[i + 2];
            }
        }

        if (count == 0) {
            return new javax.vecmath.Vector3f(0f, 0f, 0f);
        }

        float cx = (float) (sx / count);
        float cy = (float) (sy / count);
        float cz = (float) (sz / count);

        return new javax.vecmath.Vector3f(cx, cy, cz);
    }

    /**
     * Computes a translation offset that shifts the collision shape so that the mesh's AABB
     * (axis-aligned bounding box) center becomes the rigid body's local origin.
     *
     * <p>AABB centering is simpler than centroid centering and works well for symmetric models.
     * For asymmetric models (e.g., an L-shaped building), centroid centering may produce a more
     * natural physics pivot point.</p>
     *
     * @param raw the raw mesh data to compute the AABB center from
     * @return the negated AABB center as a vecmath vector
     */
    public static javax.vecmath.Vector3f computeOffsetToCenterByAabb(RawMeshData raw) {
        javax.vecmath.Vector3f center = computeAabbCenter(raw);
        return new javax.vecmath.Vector3f(-center.x, -center.y, -center.z);
    }

    /**
     * Computes the center of the axis-aligned bounding box (AABB) enclosing all mesh vertices.
     *
     * <p>The AABB center is the midpoint between the minimum and maximum extents along each axis.
     * This is useful for centering collision shapes on models whose visual origin is offset.</p>
     *
     * @param raw the raw mesh data containing position and optionally index data
     * @return the AABB center in vecmath format, or (0,0,0) if no vertices exist
     * @throws IllegalArgumentException if positions array length is not a multiple of 3
     */
    public static javax.vecmath.Vector3f computeAabbCenter(RawMeshData raw) {
        float[] positions = raw.getPositions();
        int[] indices = raw.getIndices();

        if (positions == null || positions.length == 0) {
            return new javax.vecmath.Vector3f(0f, 0f, 0f);
        }
        if (positions.length % 3 != 0) {
            throw new IllegalArgumentException("RawMeshData.positions length must be a multiple of 3");
        }

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        if (indices != null && indices.length > 0) {
            for (int idx : indices) {
                int base = idx * 3;
                if (base + 2 >= positions.length) {
                    continue;
                }
                float x = positions[base];
                float y = positions[base + 1];
                float z = positions[base + 2];
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
        } else {
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
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
        }

        // handle degenerate case (no vertices)
        if (minX == Float.POSITIVE_INFINITY) {
            return new javax.vecmath.Vector3f(0f, 0f, 0f);
        }

        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        float cz = (minZ + maxZ) * 0.5f;
        return new javax.vecmath.Vector3f(cx, cy, cz);
    }

    /**
     * Computes the centroid of the given mesh and returns it as a JOML vector.
     *
     * <p>Convenience overload for use in engine code that works with JOML types (transforms,
     * rendering). Internally delegates to {@link #computeCentroid(RawMeshData)}.</p>
     *
     * @param raw the raw mesh data to compute the centroid from
     * @return the centroid position as a JOML vector
     */
    public static Vector3f computeCentroidJoml(RawMeshData raw) {
        javax.vecmath.Vector3f c = computeCentroid(raw);
        return new Vector3f(c.x, c.y, c.z);
    }

    /**
     * Converts a JOML 3D vector to JBullet's vecmath format.
     *
     * @param v the JOML vector to convert
     * @return a new {@code javax.vecmath.Vector3f} with the same x, y, z values
     */
    public static javax.vecmath.Vector3f toVecmath(org.joml.Vector3f v) {
        return new javax.vecmath.Vector3f(v.x, v.y, v.z);
    }

    /**
     * Converts a JBullet vecmath 3D vector to JOML format.
     *
     * @param v the vecmath vector to convert
     * @return a new JOML {@link Vector3f} with the same x, y, z values
     */
    public static org.joml.Vector3f toJoml(javax.vecmath.Vector3f v) {
        return new org.joml.Vector3f(v.x, v.y, v.z);
    }
}
