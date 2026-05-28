package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

import org.chapzlock.core.asset.RawMeshData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicsUtil {

    /**
     * Convert Euler degrees (Vector3f) -> javax.vecmath.Quat4f
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
     * Convert JOML Quaternionf -> javax.vecmath.Quat4f
     */
    public static Quat4f toQuat4f(Quaternionf q) {
        // javax.vecmath.Quat4f is (x, y, z, w)
        return new Quat4f(q.x, q.y, q.z, q.w);
    }

    /**
     * Convert javax.vecmath.Quat4f -> Euler degrees (X,Y,Z)
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
     * Convert javax.vecmath.Quat4f -> JOML Quaternionf (reusing/setting passed instance is optional)
     */
    public static Quaternionf toQuaternionf(Quat4f q) {
        return new Quaternionf(q.x, q.y, q.z, q.w);
    }


    /**
     * Convenience: return an offset you can use to move the collision mesh so that the mesh's centroid becomes the rigid-body origin.
     * Typically you will pass this offset into a CompoundShape child transform as the child origin.
     * <p>
     * Example: if centroid = (1,2,3), this returns (-1,-2,-3) so the base collision geometry is shifted to put centroid at (0,0,0).
     */
    public static javax.vecmath.Vector3f computeOffsetToCenterByCentroid(RawMeshData raw) {
        javax.vecmath.Vector3f centroid = computeCentroid(raw);
        return new javax.vecmath.Vector3f(-centroid.x, -centroid.y, -centroid.z);
    }

    /**
     * Compute centroid (arithmetic mean) of all unique vertices in the raw mesh.
     * Returns javax.vecmath.Vector3f because Bullet uses that.
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
     * Convenience: return an offset to move collision mesh so that the AABB center becomes the rigid-body origin.
     */
    public static javax.vecmath.Vector3f computeOffsetToCenterByAabb(RawMeshData raw) {
        javax.vecmath.Vector3f center = computeAabbCenter(raw);
        return new javax.vecmath.Vector3f(-center.x, -center.y, -center.z);
    }

    /**
     * Compute the axis-aligned bounding box center of the mesh positions.
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
     * Overload: compute centroid and return it as an org.joml.Vector3f if you prefer using JOML in engine code.
     */
    public static Vector3f computeCentroidJoml(RawMeshData raw) {
        javax.vecmath.Vector3f c = computeCentroid(raw);
        return new Vector3f(c.x, c.y, c.z);
    }
}
