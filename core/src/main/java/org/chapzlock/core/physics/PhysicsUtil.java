package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

import org.chapzlock.core.geometry.RawMeshData;
import org.joml.Vector3f;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicsUtil {

    /**
     * Transforms a quaternion rotation used by the physics system to an Euler rotation in degrees.
     * The rotation order in this function must match the rotation order used in the transformation matrix.
     * This helps ensure, that physics simulation and graphics rendering look the same.
     *
     * @param q quaternion to convert.
     * @return Vector3f with the euler rotation in degrees
     */
    public static Vector3f quaternionToEuler(Quat4f q) {
        // Normalize quaternion
        float len = (float) Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w);
        float x = q.x / len;
        float y = q.y / len;
        float z = q.z / len;
        float w = q.w / len;

        // XYZ order
        float sinrCosp = 2 * (w * x - y * z);
        float cosrCosp = 1 - 2 * (x * x + y * y);
        float roll = (float) Math.atan2(sinrCosp, cosrCosp);  // X-axis

        float sinp = 2 * (w * y + z * x);
        float pitch;
        if (Math.abs(sinp) >= 1) {
            pitch = (float) Math.copySign(Math.PI / 2, sinp);
        } else {
            pitch = (float) Math.asin(sinp);  // Y-axis
        }

        float sinyCosp = 2 * (w * z - x * y);
        float cosyCosp = 1 - 2 * (y * y + z * z);
        float yaw = (float) Math.atan2(sinyCosp, cosyCosp);   // Z-axis

        return new Vector3f(
            (float) Math.toDegrees(roll),
            (float) Math.toDegrees(pitch),
            (float) Math.toDegrees(yaw)
        );
    }

    /**
     * Converts an euler rotation to a quaternion rotation.
     * The rotation order in this function must match the rotation order used in the transformation matrix.
     * This helps ensure, that physics simulation and graphics rendering look the same
     *
     * @param eulerRotation
     * @return Quat4f quaternion
     */
    public static Quat4f eulerToQuaternion(Vector3f eulerRotation) {
        float xRad = (float) Math.toRadians(eulerRotation.x);
        float yRad = (float) Math.toRadians(eulerRotation.y);
        float zRad = (float) Math.toRadians(eulerRotation.z);

        float cx = (float) Math.cos(xRad / 2);
        float sx = (float) Math.sin(xRad / 2);
        float cy = (float) Math.cos(yRad / 2);
        float sy = (float) Math.sin(yRad / 2);
        float cz = (float) Math.cos(zRad / 2);
        float sz = (float) Math.sin(zRad / 2);

        // XYZ rotation order
        float w = cx * cy * cz - sx * sy * sz;
        float x = sx * cy * cz + cx * sy * sz;
        float y = cx * sy * cz - sx * cy * sz;
        float z = cx * cy * sz + sx * sy * cz;

        return new Quat4f(x, y, z, w);
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
