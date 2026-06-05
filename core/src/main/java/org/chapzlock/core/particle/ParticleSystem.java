package org.chapzlock.core.particle;

import java.util.concurrent.ThreadLocalRandom;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;

/**
 * Updates all particle emitters each frame: spawning new particles and advancing existing ones.
 *
 * <p>This system is the "brain" of the particle system — it decides when to spawn particles,
 * moves them according to physics (velocity, gravity, drag), ages them, applies visual
 * modules (color/size over lifetime), and removes dead particles.
 *
 * <p>Registered as a permanent engine system (always active, like TweenSystem). Runs in
 * the LOGIC phase — after game code has moved entities, before rendering.
 *
 * <p>Game code does not call this system directly. It operates automatically on any entity
 * that has both a {@link ParticleEmitter} and a {@link Transform} component.
 *
 * @see ParticleEmitter
 * @see ParticleEffect
 * @see ParticlePass
 */
public class ParticleSystem implements GameSystem {

    private ComponentRegistry registry;

    @Override
    public void onInit(Engine engine) {
        this.registry = engine.getRegistry();
    }

    @Override
    public void onUpdate(float deltaTime) {
        registry.forEach(ParticleEmitter.class, Transform.class, (entityId, emitter, transform) -> {
            if (emitter.isActive()) {
                emit(emitter, transform, deltaTime);
            }
            update(emitter, deltaTime);
        });
    }

    private void emit(ParticleEmitter emitter, Transform transform, float deltaTime) {
        ParticleEffect effect = emitter.getEffect();
        emitter.setEmitAccumulator(emitter.getEmitAccumulator() + effect.getRate() * deltaTime);

        Vector3f emitterPos = new Vector3f();
        transform.getPosition(emitterPos);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Vector3f direction = new Vector3f();

        while (emitter.getEmitAccumulator() >= 1f) {
            emitter.setEmitAccumulator(emitter.getEmitAccumulator() - 1f);

            Particle p = emitter.emit();
            if (p == null) break;

            p.getPosition().set(emitterPos);

            effect.getEmitShape().computeDirection(direction);
            float speed = randomRange(rng, effect.getSpeedMin(), effect.getSpeedMax());
            p.getVelocity().set(direction).mul(speed);

            p.setLifetime(randomRange(rng, effect.getLifetimeMin(), effect.getLifetimeMax()));
            float size = randomRange(rng, effect.getSizeMin(), effect.getSizeMax());
            p.setSize(size);
            p.setInitialSize(size);

            if (effect.hasColorOverLifetime()) {
                Color start = effect.getColorStart();
                p.getColor().set(start.getRed(), start.getGreen(), start.getBlue(), start.getAlpha());
            } else {
                Color c = effect.getInitialColor();
                p.getColor().set(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
            }
        }
    }

    private void update(ParticleEmitter emitter, float deltaTime) {
        ParticleEffect effect = emitter.getEffect();
        Particle[] particles = emitter.getParticles();
        Vector3f gravity = effect.getGravity();
        float drag = effect.getDrag();
        boolean hasGravity = gravity.x != 0 || gravity.y != 0 || gravity.z != 0;
        boolean hasDrag = drag > 0f;
        boolean hasColorOverLifetime = effect.hasColorOverLifetime();
        boolean hasSizeOverLifetime = effect.hasSizeOverLifetime();

        int i = 0;
        while (i < emitter.getAliveCount()) {
            Particle p = particles[i];
            p.setAge(p.getAge() + deltaTime);

            if (!p.isAlive()) {
                emitter.kill(i);
                continue;
            }

            if (hasGravity) {
                p.getVelocity().add(
                    gravity.x * deltaTime,
                    gravity.y * deltaTime,
                    gravity.z * deltaTime);
            }

            if (hasDrag) {
                float factor = 1f - drag * deltaTime;
                if (factor < 0f) factor = 0f;
                p.getVelocity().mul(factor);
            }

            p.getPosition().add(
                p.getVelocity().x * deltaTime,
                p.getVelocity().y * deltaTime,
                p.getVelocity().z * deltaTime);

            if (hasColorOverLifetime) {
                float t = p.normalizedAge();
                Color start = effect.getColorStart();
                Color end = effect.getColorEnd();
                p.getColor().set(
                    lerp(start.getRed(), end.getRed(), t),
                    lerp(start.getGreen(), end.getGreen(), t),
                    lerp(start.getBlue(), end.getBlue(), t),
                    lerp(start.getAlpha(), end.getAlpha(), t));
            }

            if (hasSizeOverLifetime) {
                float t = p.normalizedAge();
                float scale = lerp(effect.getSizeStartScale(), effect.getSizeEndScale(), t);
                p.setSize(p.getInitialSize() * scale);
            }

            i++;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float randomRange(ThreadLocalRandom rng, float min, float max) {
        if (min == max) return min;
        return min + rng.nextFloat() * (max - min);
    }
}
