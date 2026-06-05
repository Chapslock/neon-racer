package org.chapzlock.core.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleOneShotTest {

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
}
