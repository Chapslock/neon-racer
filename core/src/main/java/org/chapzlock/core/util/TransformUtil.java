package org.chapzlock.core.util;

import org.chapzlock.core.component.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Static helpers for extracting direction vectors from a Transform's world matrix.
 *
 * <p>Use these instead of computing directions from local rotation alone — they account
 * for parent rotations in a hierarchy.
 */
public final class TransformUtil {

    private TransformUtil() {}

    /**
     * Extracts the forward direction (-Z axis) from the transform's world matrix.
     */
    public static Vector3f forward(Transform transform, Vector3f dest) {
        Matrix4f m = transform.getWorldMatrix();
        return dest.set(-m.m20(), -m.m21(), -m.m22()).normalize();
    }

    /**
     * Extracts the right direction (+X axis) from the transform's world matrix.
     */
    public static Vector3f right(Transform transform, Vector3f dest) {
        Matrix4f m = transform.getWorldMatrix();
        return dest.set(m.m00(), m.m01(), m.m02()).normalize();
    }

    /**
     * Extracts the up direction (+Y axis) from the transform's world matrix.
     */
    public static Vector3f up(Transform transform, Vector3f dest) {
        Matrix4f m = transform.getWorldMatrix();
        return dest.set(m.m10(), m.m11(), m.m12()).normalize();
    }
}
