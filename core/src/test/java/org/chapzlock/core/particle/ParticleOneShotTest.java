package org.chapzlock.core.particle;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleOneShotTest {

    private Engine engine;
    private ComponentRegistry registry;
    private ParticleSystem system;

    @BeforeEach
    void setUp() {
        engine = Engine.testEngine();
        registry = engine.getRegistry();
        system = new ParticleSystem();
        system.onInit(engine);
    }

    @Test
    void burstCount_defaultsToZero() {
        ParticleEffect effect = ParticleEffect.builder().build();
        assertEquals(0, effect.getBurstCount());
    }

    @Test
    void burstCount_setViaBuilder() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(20)
            .build();
        assertEquals(20, effect.getBurstCount());
    }

    @Test
    void duration_defaultsToZero() {
        ParticleEffect effect = ParticleEffect.builder().build();
        assertEquals(0f, effect.getDuration());
    }

    @Test
    void duration_setViaBuilder() {
        ParticleEffect effect = ParticleEffect.builder()
            .duration(0.5f)
            .build();
        assertEquals(0.5f, effect.getDuration());
    }

    @Test
    void isSelfTerminating_trueWhenBurstSet() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(10)
            .build();
        assertTrue(effect.isSelfTerminating());
    }

    @Test
    void isSelfTerminating_trueWhenDurationSet() {
        ParticleEffect effect = ParticleEffect.builder()
            .duration(1f)
            .build();
        assertTrue(effect.isSelfTerminating());
    }

    @Test
    void isSelfTerminating_falseWhenNeither() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(10)
            .build();
        assertFalse(effect.isSelfTerminating());
    }

    @Test
    void emitter_selfTerminatingDerivedFromEffect() {
        ParticleEffect burstEffect = ParticleEffect.builder()
            .burst(10)
            .lifetime(1f)
            .maxParticles(20)
            .build();
        ParticleEmitter emitter = new ParticleEmitter(burstEffect);
        assertTrue(emitter.isSelfTerminating());
    }

    @Test
    void emitter_notSelfTerminatingForContinuous() {
        ParticleEffect continuous = ParticleEffect.builder()
            .rate(10)
            .lifetime(1f)
            .maxParticles(20)
            .build();
        ParticleEmitter emitter = new ParticleEmitter(continuous);
        assertFalse(emitter.isSelfTerminating());
    }

    @Test
    void emitter_emissionCompleteDefaultsFalse() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(10)
            .maxParticles(20)
            .build();
        ParticleEmitter emitter = new ParticleEmitter(effect);
        assertFalse(emitter.isEmissionComplete());
    }

    @Test
    void emitter_elapsedDefaultsZero() {
        ParticleEffect effect = ParticleEffect.builder()
            .duration(1f)
            .maxParticles(20)
            .build();
        ParticleEmitter emitter = new ParticleEmitter(effect);
        assertEquals(0f, emitter.getElapsed());
    }

    @Test
    void burst_emitsAllParticlesOnFirstFrame() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(15)
            .lifetime(1f)
            .speed(1f, 1f)
            .maxParticles(20)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.016f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertEquals(15, emitter.getAliveCount());
        assertTrue(emitter.isEmissionComplete());
    }

    @Test
    void burst_doesNotEmitOnSubsequentFrames() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(5)
            .lifetime(2f)
            .speed(1f, 1f)
            .maxParticles(20)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.016f);
        system.onUpdate(0.016f);
        system.onUpdate(0.016f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertEquals(5, emitter.getAliveCount());
    }

    @Test
    void duration_stopsEmittingAfterElapsed() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .duration(0.1f)
            .lifetime(2f)
            .speed(0f, 0f)
            .maxParticles(200)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.05f);
        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        int countMidway = emitter.getAliveCount();
        assertTrue(countMidway > 0);
        assertFalse(emitter.isEmissionComplete());

        system.onUpdate(0.1f);
        assertTrue(emitter.isEmissionComplete());

        int countAfterStop = emitter.getAliveCount();
        system.onUpdate(0.5f);
        assertTrue(emitter.getAliveCount() <= countAfterStop);
    }

    @Test
    void continuousEmitter_neverMarksEmissionComplete() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(10f)
            .lifetime(1f)
            .speed(0f, 0f)
            .maxParticles(100)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(1.0f);
        system.onUpdate(1.0f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertFalse(emitter.isEmissionComplete());
    }

    @Test
    void selfTerminating_destroysEntityWhenAllParticlesDead() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(5)
            .lifetime(0.1f)
            .speed(0f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        // Frame 1: burst emits 5 particles
        system.onUpdate(0.016f);
        assertTrue(registry.entityExists(entity));

        // Advance past particle lifetime so all die
        system.onUpdate(0.2f);

        assertFalse(registry.entityExists(entity));
    }

    @Test
    void selfTerminating_doesNotDestroyWhileParticlesAlive() {
        ParticleEffect effect = ParticleEffect.builder()
            .burst(5)
            .lifetime(1f)
            .speed(0f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.016f);
        system.onUpdate(0.5f);

        assertTrue(registry.entityExists(entity));
    }

    @Test
    void continuousEmitter_neverAutoDestroys() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(0.01f)
            .speed(0f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.1f);
        system.onUpdate(0.1f);

        assertTrue(registry.entityExists(entity));
    }
}
