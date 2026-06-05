package org.chapzlock.core.particle;

import org.chapzlock.core.ecs.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * A component that spawns and manages particles from the entity it's attached to.
 *
 * <p>A particle emitter works like a fountain: it continuously spawns small visual
 * elements (particles) at a configured rate, each with a limited lifespan. Particles
 * fly outward, change color/size over time, and disappear when their lifetime ends.
 *
 * <p>Attach this component to any entity with a {@link org.chapzlock.core.component.Transform}
 * to start emitting particles from that entity's position. The {@link ParticleSystem} reads
 * the entity's current position each frame, so moving the entity moves the emission point.
 *
 * <p>The emitter owns a fixed-size pool of {@link Particle} objects (pre-allocated, never
 * individually allocated at runtime). When a particle dies, its slot is recycled for new
 * particles via the swap-with-last-alive strategy.
 *
 * <p>Usage:
 * <pre>{@code
 * ParticleEffect fire = ParticleEffect.builder()
 *     .rate(30)
 *     .lifetime(0.5f, 1.0f)
 *     .speed(1f, 3f)
 *     .color(new Color(1f, 0.5f, 0f, 1f))
 *     .build();
 *
 * registry.addComponent(entityId, new ParticleEmitter(fire));
 * }</pre>
 *
 * @see ParticleEffect
 * @see ParticleSystem
 */
@Getter
public class ParticleEmitter implements Component {

    private final ParticleEffect effect;
    private final Particle[] particles;
    @Setter
    private int aliveCount;
    @Setter
    private float emitAccumulator;
    @Setter
    private boolean active;
    @Setter
    private float elapsed;
    @Setter
    private boolean emissionComplete;
    private final boolean selfTerminating;

    /**
     * Creates an emitter with a pre-allocated particle pool sized to the effect's maxParticles.
     *
     * @param effect the immutable configuration defining particle behavior
     */
    public ParticleEmitter(ParticleEffect effect) {
        this.effect = effect;
        this.particles = new Particle[effect.getMaxParticles()];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
        this.aliveCount = 0;
        this.emitAccumulator = 0f;
        this.active = true;
        this.elapsed = 0f;
        this.emissionComplete = false;
        this.selfTerminating = effect.isSelfTerminating();
    }

    /**
     * Activates the next dead particle slot and returns it for initialization.
     *
     * <p>Returns null if the pool is full (aliveCount == maxParticles). The returned
     * particle is already in the alive range — the caller should set its position,
     * velocity, lifetime, etc.
     *
     * @return the newly activated particle, or null if pool is full
     */
    public Particle emit() {
        if (aliveCount >= particles.length) {
            return null;
        }
        Particle p = particles[aliveCount];
        p.reset();
        aliveCount++;
        return p;
    }

    /**
     * Kills the particle at the given index by swapping it with the last alive particle.
     *
     * <p>This is O(1) and keeps all alive particles contiguous in indices [0, aliveCount).
     * After this call, the caller should NOT advance the loop index (the swapped-in particle
     * at this index still needs processing).
     *
     * @param index index of the particle to kill (must be less than aliveCount)
     */
    public void kill(int index) {
        aliveCount--;
        if (index != aliveCount) {
            Particle temp = particles[index];
            particles[index] = particles[aliveCount];
            particles[aliveCount] = temp;
        }
    }
}
