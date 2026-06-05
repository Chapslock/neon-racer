package org.chapzlock.core.particle;

import org.chapzlock.core.application.Engine;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.ecs.Entity;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleSystemTest {

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
    void emitsParticlesBasedOnRate() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(10f)
            .lifetime(5f)
            .speed(1f, 1f)
            .maxParticles(100)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.5f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertEquals(5, emitter.getAliveCount());
    }

    @Test
    void particlesSpawnAtEmitterPosition() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(5f)
            .speed(0f, 0f)
            .maxParticles(100)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(5f, 10f, 15f));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.1f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertTrue(emitter.getAliveCount() > 0);
        Particle p = emitter.getParticles()[0];
        assertEquals(5f, p.getPosition().x, 0.001f);
        assertEquals(10f, p.getPosition().y, 0.001f);
        assertEquals(15f, p.getPosition().z, 0.001f);
    }

    @Test
    void particlesAdvanceAge() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(5f)
            .speed(0f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.1f);
        system.onUpdate(0.3f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        Particle p = emitter.getParticles()[0];
        assertTrue(p.getAge() >= 0.3f);
    }

    @Test
    void deadParticlesAreRemoved() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(0.1f)
            .speed(0f, 0f)
            .maxParticles(100)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.05f);
        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        int countAfterFirstFrame = emitter.getAliveCount();
        assertTrue(countAfterFirstFrame > 0);

        system.onUpdate(0.2f);
        assertTrue(emitter.getAliveCount() <= countAfterFirstFrame + 20);
    }

    @Test
    void gravityAcceleratesParticles() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(5f)
            .speed(0f, 0f)
            .gravity(0f, -10f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.01f);
        system.onUpdate(1.0f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        Particle p = emitter.getParticles()[0];
        assertTrue(p.getVelocity().y < 0f, "Gravity should pull velocity downward");
    }

    @Test
    void dragSlowsParticles() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(5f)
            .speed(10f, 10f)
            .drag(5f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.01f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        Particle p = emitter.getParticles()[0];
        float initialSpeed = p.getVelocity().length();

        system.onUpdate(0.5f);

        float afterSpeed = p.getVelocity().length();
        assertTrue(afterSpeed < initialSpeed, "Drag should reduce speed");
    }

    @Test
    void colorOverLifetime_interpolatesColor() {
        Color start = new Color(1f, 0f, 0f, 1f);
        Color end = new Color(0f, 0f, 1f, 0f);

        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(1f)
            .speed(0f, 0f)
            .colorOverLifetime(start, end)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.01f);
        system.onUpdate(0.5f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        Particle p = emitter.getParticles()[0];
        assertTrue(p.getColor().x < 1f, "Red should decrease");
        assertTrue(p.getColor().z > 0f, "Blue should increase");
        assertTrue(p.getColor().w < 1f, "Alpha should decrease");
    }

    @Test
    void inactiveEmitter_doesNotEmit() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(5f)
            .maxParticles(100)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        ParticleEmitter emitter = new ParticleEmitter(effect);
        emitter.setActive(false);
        registry.addComponent(entity, emitter);

        system.onUpdate(1.0f);

        assertEquals(0, emitter.getAliveCount());
    }

    @Test
    void poolCap_preventsOverflow() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(1000f)
            .lifetime(999f)
            .speed(0f, 0f)
            .maxParticles(5)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(1.0f);

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        assertEquals(5, emitter.getAliveCount());
    }

    @Test
    void sizeOverLifetime_linearInterpolation() {
        ParticleEffect effect = ParticleEffect.builder()
            .rate(100f)
            .lifetime(1f)
            .speed(0f, 0f)
            .size(1f, 1f)
            .sizeOverLifetime(1f, 0f)
            .maxParticles(10)
            .build();

        int entity = Entity.create(registry);
        registry.addComponent(entity, Transform.at(0, 0, 0));
        registry.addComponent(entity, new ParticleEmitter(effect));

        system.onUpdate(0.01f); // Emit
        system.onUpdate(0.5f);  // Advance to ~50% lifetime

        ParticleEmitter emitter = registry.getComponent(entity, ParticleEmitter.class);
        Particle p = emitter.getParticles()[0];
        assertEquals(0.5f, p.getSize(), 0.1f);
    }
}
