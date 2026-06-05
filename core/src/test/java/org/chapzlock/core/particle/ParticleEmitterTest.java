package org.chapzlock.core.particle;

import org.chapzlock.core.ecs.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleEmitterTest {

    @Test
    void implementsComponent() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);
        assertInstanceOf(Component.class, emitter);
    }

    @Test
    void constructorAllocatesPool() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(50).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        assertEquals(50, emitter.getParticles().length);
        assertEquals(0, emitter.getAliveCount());
        assertTrue(emitter.isActive());
    }

    @Test
    void poolPreallocatesParticleInstances() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        for (int i = 0; i < 10; i++) {
            assertNotNull(emitter.getParticles()[i], "Pool slot " + i + " should be pre-allocated");
        }
    }

    @Test
    void emit_incrementsAliveCount() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        Particle p = emitter.emit();

        assertNotNull(p);
        assertEquals(1, emitter.getAliveCount());
    }

    @Test
    void emit_returnsNullWhenPoolFull() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(2).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        emitter.emit();
        emitter.emit();
        Particle p = emitter.emit();

        assertNull(p);
        assertEquals(2, emitter.getAliveCount());
    }

    @Test
    void kill_swapsWithLastAlive() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        Particle first = emitter.emit();
        first.getPosition().set(1, 0, 0);
        Particle second = emitter.emit();
        second.getPosition().set(2, 0, 0);
        Particle third = emitter.emit();
        third.getPosition().set(3, 0, 0);

        // Kill the first particle (index 0) — should swap with last alive (index 2)
        emitter.kill(0);

        assertEquals(2, emitter.getAliveCount());
        // The particle that was at index 2 (position 3,0,0) is now at index 0
        assertEquals(3f, emitter.getParticles()[0].getPosition().x, 0.001f);
    }

    @Test
    void kill_lastAlive_justDecrementsCount() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        emitter.emit();
        Particle second = emitter.emit();
        second.getPosition().set(99, 0, 0);

        emitter.kill(1); // kill last alive — no swap needed

        assertEquals(1, emitter.getAliveCount());
    }

    @Test
    void setActive_controlsEmission() {
        ParticleEffect effect = ParticleEffect.builder().maxParticles(10).build();
        ParticleEmitter emitter = new ParticleEmitter(effect);

        emitter.setActive(false);
        assertFalse(emitter.isActive());

        emitter.setActive(true);
        assertTrue(emitter.isActive());
    }
}
