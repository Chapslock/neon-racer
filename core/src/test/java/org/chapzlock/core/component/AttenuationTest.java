package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttenuationTest {

    @Test
    void defaultPresetValues() {
        Attenuation a = Attenuation.DEFAULT;
        assertEquals(1.0f, a.constant());
        assertEquals(0.014f, a.linear());
        assertEquals(0.0007f, a.quadratic());
    }

    @Test
    void shortRangePresetValues() {
        Attenuation a = Attenuation.SHORT_RANGE;
        assertEquals(1.0f, a.constant());
        assertEquals(0.7f, a.linear());
        assertEquals(1.8f, a.quadratic());
    }

    @Test
    void longRangePresetValues() {
        Attenuation a = Attenuation.LONG_RANGE;
        assertEquals(1.0f, a.constant());
        assertEquals(0.007f, a.linear());
        assertEquals(0.0002f, a.quadratic());
    }

    @Test
    void customConstruction() {
        Attenuation a = new Attenuation(1.0f, 0.5f, 0.1f);
        assertEquals(0.5f, a.linear());
        assertEquals(0.1f, a.quadratic());
    }
}
