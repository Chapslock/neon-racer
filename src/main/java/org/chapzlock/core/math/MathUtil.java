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
    public static Matrix4f createTransformationMatrix(Vector3f translation, Vector3f rotationDegrees, float scale) {
        Matrix4f matrix = new Matrix4f(); // identity

        // Convert rotation from degrees to radians
        float rotX = (float) Math.toRadians(rotationDegrees.x);
        float rotY = (float) Math.toRadians(rotationDegrees.y);
        float rotZ = (float) Math.toRadians(rotationDegrees.z);

        // Apply translation
        matrix.translate(translation);

        // Apply rotations (order: X → Y → Z, can adjust if needed)
        matrix.rotate(rotX, 1, 0, 0)
            .rotate(rotY, 0, 1, 0)
            .rotate(rotZ, 0, 0, 1);

        // Apply uniform scale
        matrix.scale(scale);

        return matrix;
    }

    /**
     * Creates a view matrix (camera transform).
     *
     * @param position Camera position in world space
     * @param rotation Camera rotation in degrees (x = pitch, y = yaw, z = roll)
     * @return View matrix
     */
    public static Matrix4f createViewMatrix(Vector3f position, Vector3f rotationDegrees) {
        Matrix4f view = new Matrix4f();

        // Convert to radians
        float pitch = (float) Math.toRadians(rotationDegrees.x);
        float yaw = (float) Math.toRadians(rotationDegrees.y);
        float roll = (float) Math.toRadians(rotationDegrees.z);

        // Apply rotations: yaw first, then pitch, then roll
        view.rotate(-pitch, 1, 0, 0);
        view.rotate(-yaw, 0, 1, 0);
        view.rotate(-roll, 0, 0, 1);

        // Apply translation (inverse of position)
        view.translate(-position.x, -position.y, -position.z);

        return view;
    }
}
