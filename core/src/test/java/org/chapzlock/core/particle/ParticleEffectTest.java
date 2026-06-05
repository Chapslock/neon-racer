package org.chapzlock.core.particle;

import org.chapzlock.core.component.Color;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleEffectTest {

    @Test
    void builder_defaultValues() {
        ParticleEffect effect = ParticleEffect.builder().build();

        assertEquals(10f, effect.getRate());
        assertEquals(1f, effect.getLifetimeMin());
        assertEquals(1f, effect.getLifetimeMax());
        assertEquals(1f, effect.getSpeedMin());
        assertEquals(1f, effect.getSpeedMax());
        assertEquals(0.1f, effect.getSizeMin());
        assertEquals(0.1f, effect.getSizeMax());
        assertEquals(100, effect.getMaxParticles());
        assertNotNull(effect.getInitialColor());
        assertNotNull(effect.getEmitShape());
        assertNull(effect.getTexturePath());
    }

    @Test
    void builder_customValues() {
        Color startColor = new Color(1f, 0f, 0f, 1f);
        Color endColor = new Color(0f, 0f, 1f, 0f);
        Vector3f gravity = new Vector3f(0, -9.8f, 0);

        ParticleEffect effect = ParticleEffect.builder()
            .rate(50f)
            .lifetime(0.8f, 1.2f)
            .speed(3f, 5f)
            .size(0.1f, 0.3f)
            .color(startColor)
            .emitShape(EmitShape.cone(15f))
            .maxParticles(200)
            .colorOverLifetime(startColor, endColor)
            .sizeOverLifetime(1f, 0f)
            .gravity(gravity)
            .drag(0.5f)
            .texture("textures/spark.png")
            .build();

        assertEquals(50f, effect.getRate());
        assertEquals(0.8f, effect.getLifetimeMin());
        assertEquals(1.2f, effect.getLifetimeMax());
        assertEquals(3f, effect.getSpeedMin());
        assertEquals(5f, effect.getSpeedMax());
        assertEquals(0.1f, effect.getSizeMin());
        assertEquals(0.3f, effect.getSizeMax());
        assertEquals(200, effect.getMaxParticles());
        assertSame(startColor, effect.getInitialColor());
        assertSame(startColor, effect.getColorStart());
        assertSame(endColor, effect.getColorEnd());
        assertEquals(1f, effect.getSizeStartScale());
        assertEquals(0f, effect.getSizeEndScale());
        assertEquals(gravity, effect.getGravity());
        assertEquals(0.5f, effect.getDrag());
        assertEquals("textures/spark.png", effect.getTexturePath());
    }

    @Test
    void builder_lifetimeWithSingleValue_setsMinAndMax() {
        ParticleEffect effect = ParticleEffect.builder()
            .lifetime(2f)
            .build();

        assertEquals(2f, effect.getLifetimeMin());
        assertEquals(2f, effect.getLifetimeMax());
    }

    @Test
    void builder_gravityFromComponents() {
        ParticleEffect effect = ParticleEffect.builder()
            .gravity(0f, -2f, 0f)
            .build();

        assertEquals(new Vector3f(0, -2f, 0), effect.getGravity());
    }

    @Test
    void hasColorOverLifetime_falseByDefault() {
        ParticleEffect effect = ParticleEffect.builder().build();
        assertFalse(effect.hasColorOverLifetime());
    }

    @Test
    void hasColorOverLifetime_trueWhenSet() {
        ParticleEffect effect = ParticleEffect.builder()
            .colorOverLifetime(new Color(1, 0, 0), new Color(0, 0, 1))
            .build();
        assertTrue(effect.hasColorOverLifetime());
    }

    @Test
    void hasSizeOverLifetime_falseByDefault() {
        ParticleEffect effect = ParticleEffect.builder().build();
        assertFalse(effect.hasSizeOverLifetime());
    }

    @Test
    void hasSizeOverLifetime_trueWhenSet() {
        ParticleEffect effect = ParticleEffect.builder()
            .sizeOverLifetime(1f, 0f)
            .build();
        assertTrue(effect.hasSizeOverLifetime());
    }
}
