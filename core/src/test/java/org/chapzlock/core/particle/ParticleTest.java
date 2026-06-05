package org.chapzlock.core.particle;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleTest {

    @Test
    void newParticle_hasZeroState() {
        Particle p = new Particle();
        assertEquals(0f, p.getAge());
        assertEquals(0f, p.getLifetime());
        assertEquals(0f, p.getSize());
        assertEquals(new Vector3f(0, 0, 0), p.getPosition());
        assertEquals(new Vector3f(0, 0, 0), p.getVelocity());
        assertEquals(new Vector4f(1, 1, 1, 1), p.getColor());
    }

    @Test
    void reset_clearsAllFields() {
        Particle p = new Particle();
        p.getPosition().set(5, 5, 5);
        p.getVelocity().set(1, 2, 3);
        p.setAge(0.5f);
        p.setLifetime(1f);
        p.setSize(2f);
        p.getColor().set(1, 0, 0, 0.5f);
        p.reset();
        assertEquals(0f, p.getAge());
        assertEquals(0f, p.getLifetime());
        assertEquals(0f, p.getSize());
        assertEquals(new Vector3f(0, 0, 0), p.getPosition());
        assertEquals(new Vector3f(0, 0, 0), p.getVelocity());
        assertEquals(new Vector4f(1, 1, 1, 1), p.getColor());
    }

    @Test
    void isAlive_trueWhenAgeUnderLifetime() {
        Particle p = new Particle();
        p.setLifetime(1.0f);
        p.setAge(0.5f);
        assertTrue(p.isAlive());
    }

    @Test
    void isAlive_falseWhenAgeExceedsLifetime() {
        Particle p = new Particle();
        p.setLifetime(1.0f);
        p.setAge(1.5f);
        assertFalse(p.isAlive());
    }

    @Test
    void normalizedAge_returnsRatioClamped() {
        Particle p = new Particle();
        p.setLifetime(2.0f);
        p.setAge(1.0f);
        assertEquals(0.5f, p.normalizedAge(), 0.001f);
    }

    @Test
    void normalizedAge_clampsToOne() {
        Particle p = new Particle();
        p.setLifetime(1.0f);
        p.setAge(2.0f);
        assertEquals(1.0f, p.normalizedAge(), 0.001f);
    }
}
