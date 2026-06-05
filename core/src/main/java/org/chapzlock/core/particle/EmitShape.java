package org.chapzlock.core.particle;

import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Defines the shape from which particles are emitted — controlling where they spawn
 * and in which direction they fly.
 *
 * <p>Think of it like the nozzle of a hose: a {@link #point()} nozzle shoots everything
 * straight forward from a single point, while a {@link #cone(float)} nozzle sprays
 * particles outward in a cone shape (like a flashlight beam or an exhaust pipe).
 *
 * <p>Usage:
 * <pre>{@code
 * // Tight exhaust cone (15-degree spread)
 * EmitShape.cone(15f)
 *
 * // Single point, no spread
 * EmitShape.point()
 * }</pre>
 */
public sealed interface EmitShape permits EmitShape.Point, EmitShape.Cone {

    /**
     * Computes a position offset from the emitter center where the particle spawns.
     *
     * @param out the vector to store the offset in (mutated in place)
     */
    void computeSpawnOffset(Vector3f out);

    /**
     * Computes a normalized direction vector for the particle's initial velocity.
     *
     * @param out the vector to store the direction in (mutated in place, always length 1)
     */
    void computeDirection(Vector3f out);

    /**
     * Creates a point emitter — all particles spawn at the exact emitter position
     * and fly straight forward (negative Z in local space).
     */
    static EmitShape point() {
        return new Point();
    }

    /**
     * Creates a cone emitter — particles spawn at the emitter position but fly
     * in random directions within a cone.
     *
     * <p>A half-angle of 15 degrees gives a tight focused beam (like a rocket exhaust).
     * A half-angle of 45 degrees gives a wide spray (like a fountain).
     * A half-angle of 90 degrees gives a hemisphere.
     *
     * @param halfAngleDegrees the maximum angle from the forward direction (0 = straight, 90 = hemisphere)
     */
    static EmitShape cone(float halfAngleDegrees) {
        return new Cone(halfAngleDegrees);
    }

    record Point() implements EmitShape {
        @Override
        public void computeSpawnOffset(Vector3f out) {
            out.set(0, 0, 0);
        }

        @Override
        public void computeDirection(Vector3f out) {
            out.set(0, 0, -1);
        }
    }

    record Cone(float halfAngleDegrees) implements EmitShape {
        @Override
        public void computeSpawnOffset(Vector3f out) {
            out.set(0, 0, 0);
        }

        @Override
        public void computeDirection(Vector3f out) {
            float halfAngleRad = (float) Math.toRadians(halfAngleDegrees);
            ThreadLocalRandom rng = ThreadLocalRandom.current();

            float cosAngle = (float) Math.cos(halfAngleRad);
            float z = -(cosAngle + rng.nextFloat() * (1f - cosAngle));
            float phi = rng.nextFloat() * 2f * (float) Math.PI;
            float sinTheta = (float) Math.sqrt(1f - z * z);
            out.set(sinTheta * (float) Math.cos(phi), sinTheta * (float) Math.sin(phi), z);
            out.normalize();
        }
    }
}
