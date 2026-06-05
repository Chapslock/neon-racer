package org.chapzlock.core.particle;

import org.joml.Vector3f;
import org.joml.Vector4f;

import lombok.Getter;
import lombok.Setter;

/**
 * A single particle's runtime state within an emitter's pool.
 *
 * <p>A particle is a tiny visual element (a glowing dot, a spark, a smoke puff) that
 * lives for a short time and then disappears. Many particles together create effects
 * like fire, exhaust trails, or explosions.
 *
 * <p>This is a plain data object — it holds position, velocity, age, etc. but has no
 * logic of its own. The {@link ParticleSystem} updates these fields each frame, and the
 * {@link ParticleRenderer} reads them to draw billboarded quads.
 *
 * <p>Particles live in a fixed-size array owned by {@link ParticleEmitter}. They are
 * never allocated individually — the pool is pre-allocated and particles are recycled
 * via {@link #reset()} when they die.
 */
@Getter
@Setter
public class Particle {

    private final Vector3f position = new Vector3f(0, 0, 0);
    private final Vector3f velocity = new Vector3f(0, 0, 0);
    private final Vector4f color = new Vector4f(1, 1, 1, 1);
    private float age;
    private float lifetime;
    private float size;
    private float initialSize;

    /**
     * Resets all fields to default state, preparing this particle for reuse.
     *
     * <p>Called when a dead particle's slot is recycled for a newly emitted particle.
     */
    public void reset() {
        position.set(0, 0, 0);
        velocity.set(0, 0, 0);
        color.set(1, 1, 1, 1);
        age = 0f;
        lifetime = 0f;
        size = 0f;
        initialSize = 0f;
    }

    /**
     * Returns true if this particle is still alive (age has not exceeded its lifetime).
     */
    public boolean isAlive() {
        return age < lifetime;
    }

    /**
     * Returns the particle's age as a 0-to-1 ratio of its total lifetime.
     *
     * <p>Used by "over lifetime" modules to interpolate properties. Returns 0 at birth,
     * 1 at death. Clamped to 1.0 if age exceeds lifetime.
     */
    public float normalizedAge() {
        if (lifetime <= 0f) return 1f;
        return Math.min(age / lifetime, 1f);
    }
}
