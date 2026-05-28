package org.chapzlock.core.component;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformTest {

    @Test
    void getForwardAndRightVectorsDoNotAlias() {
        Transform transform = new Transform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 45, 0)
        );

        Vector3f forward = transform.getForwardVector();
        Vector3f right = transform.getRightVector();

        float dot = forward.dot(right);
        assertEquals(0f, dot, 0.01f, "Forward and right should be perpendicular");

        Vector3f freshForward = transform.getForwardVector();
        assertEquals(freshForward.x, forward.x, 0.001f);
        assertEquals(freshForward.y, forward.y, 0.001f);
        assertEquals(freshForward.z, forward.z, 0.001f);
    }

    @Test
    void getForwardVectorWithDestDoesNotAllocate() {
        Transform transform = new Transform();
        Vector3f dest = new Vector3f();

        Vector3f result = transform.getForwardVector(dest);

        assertSame(dest, result, "Should return the dest parameter");
    }

    @Test
    void getRightVectorWithDestDoesNotAllocate() {
        Transform transform = new Transform();
        Vector3f dest = new Vector3f();

        Vector3f result = transform.getRightVector(dest);

        assertSame(dest, result, "Should return the dest parameter");
    }

    @Test
    void getForwardVectorDefaultRotationPointsPositiveZ() {
        Transform transform = new Transform();

        Vector3f forward = transform.getForwardVector();

        assertEquals(0f, forward.x, 0.001f);
        assertEquals(0f, forward.y, 0.001f);
        assertEquals(1f, forward.z, 0.001f);
    }

    @Test
    void getRightVectorDefaultRotationPointsPositiveX() {
        Transform transform = new Transform();

        Vector3f right = transform.getRightVector();

        assertEquals(1f, right.x, 0.001f);
        assertEquals(0f, right.y, 0.001f);
        assertEquals(0f, right.z, 0.001f);
    }
}
