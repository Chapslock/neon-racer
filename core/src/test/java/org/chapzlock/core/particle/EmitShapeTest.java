package org.chapzlock.core.particle;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmitShapeTest {

    @Test
    void point_offsetIsZero() {
        EmitShape shape = EmitShape.point();
        Vector3f offset = new Vector3f();
        shape.computeSpawnOffset(offset);
        assertEquals(new Vector3f(0, 0, 0), offset);
    }

    @Test
    void point_directionIsForward() {
        EmitShape shape = EmitShape.point();
        Vector3f direction = new Vector3f();
        shape.computeDirection(direction);
        assertEquals(new Vector3f(0, 0, -1), direction);
    }

    @Test
    void cone_directionWithinHalfAngle() {
        EmitShape shape = EmitShape.cone(30f);
        Vector3f forward = new Vector3f(0, 0, -1);

        for (int i = 0; i < 100; i++) {
            Vector3f direction = new Vector3f();
            shape.computeDirection(direction);

            float angleDeg = (float) Math.toDegrees(forward.angle(direction));
            assertTrue(angleDeg <= 30f + 0.01f,
                "Direction angle " + angleDeg + " exceeds half-angle 30");
            assertEquals(1f, direction.length(), 0.001f, "Direction must be normalized");
        }
    }

    @Test
    void cone_zeroAngle_producesForwardDirection() {
        EmitShape shape = EmitShape.cone(0f);
        Vector3f direction = new Vector3f();
        shape.computeDirection(direction);
        assertEquals(0f, new Vector3f(0, 0, -1).angle(direction), 0.001f);
    }
}
