package org.chapzlock.core.math;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtil {

    /**
     * Creates a transformation matrix.
     * @param translation
     * @param rotation
     * @param scale
     * @return
     */
    public static Matrix4f createTransformationMatrix(Vector3f translation, Vector3f rotation, float scale) {
        Matrix4f matrix = new Matrix4f(); // identity by default

        // Apply translation
        Matrix4f translationMatrix = new Matrix4f().translate(translation.x, translation.y, translation.z);

        // Apply rotations (order: X, Y, Z for example)
        Matrix4f rotX = new Matrix4f().rotate(rotation.x, 1, 0, 0);
        Matrix4f rotY = new Matrix4f().rotate(rotation.y, 0, 1, 0);
        Matrix4f rotZ = new Matrix4f().rotate(rotation.z, 0, 0, 1);

        // Apply scale
        Matrix4f scaleMatrix = new Matrix4f().scale(scale, scale, scale);

        // Combine them: T * Rz * Ry * Rx * S
        matrix = translationMatrix
            .mul(rotZ)
            .mul(rotY)
            .mul(rotX)
            .mul(scaleMatrix);

        return matrix;
    }

    /**
     * Creates a view matrix (camera transform).
     *
     * @param position Camera position in world space
     * @param rotation Camera rotation in degrees (x = pitch, y = yaw, z = roll)
     * @return View matrix
     */
    public static Matrix4f createViewMatrix(Vector3f position, Vector3f rotation) {
        Matrix4f view = new Matrix4f();

        // Apply rotations (note: order matters!)
        Matrix4f pitch = new Matrix4f().rotate(rotation.x, 1, 0, 0);
        Matrix4f yaw = new Matrix4f().rotate(rotation.y, 0, 1, 0);
        Matrix4f roll = new Matrix4f().rotate(rotation.z, 0, 0, 1);

        // Apply in order: roll * pitch * yaw
        view = roll.mul(pitch).mul(yaw);

        // Apply translation (inverse of camera position)
        Matrix4f translation = new Matrix4f().translate(-position.x, -position.y, -position.z);

        view = view.mul(translation);

        return view;
    }
}
