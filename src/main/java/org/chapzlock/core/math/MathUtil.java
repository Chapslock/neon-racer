package org.chapzlock.core.math;

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
        Matrix4f translationMatrix = Matrix4f.translate(translation.x, translation.y, translation.z);

        // Apply rotations (order: X, Y, Z for example)
        Matrix4f rotX = Matrix4f.rotate(rotation.x, 1, 0, 0);
        Matrix4f rotY = Matrix4f.rotate(rotation.y, 0, 1, 0);
        Matrix4f rotZ = Matrix4f.rotate(rotation.z, 0, 0, 1);

        // Apply scale
        Matrix4f scaleMatrix = Matrix4f.scale(scale, scale, scale);

        // Combine them: T * Rz * Ry * Rx * S
        matrix = translationMatrix
            .multiply(rotZ)
            .multiply(rotY)
            .multiply(rotX)
            .multiply(scaleMatrix);

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

        // Start with identity
        view.setIdentity();

        // Apply rotations (note: order matters!)
        Matrix4f pitch = Matrix4f.rotate(rotation.x, 1, 0, 0);
        Matrix4f yaw   = Matrix4f.rotate(rotation.y, 0, 1, 0);
        Matrix4f roll  = Matrix4f.rotate(rotation.z, 0, 0, 1);

        // Apply in order: roll * pitch * yaw
        view = roll.multiply(pitch).multiply(yaw);

        // Apply translation (inverse of camera position)
        Matrix4f translation = Matrix4f.translate(-position.x, -position.y, -position.z);

        view = view.multiply(translation);

        return view;
    }
}
