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
}
