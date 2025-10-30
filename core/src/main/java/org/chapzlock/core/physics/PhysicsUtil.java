package org.chapzlock.core.physics;

import javax.vecmath.Quat4f;

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
}
