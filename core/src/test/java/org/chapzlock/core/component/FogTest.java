package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FogTest {

    @Test
    void defaultBuilderProducesDefaultValues() {
        Fog fog = Fog.builder().build();
        assertEquals(Fog.DEFAULT_DENSITY, fog.getDensity());
        assertEquals(Fog.DEFAULT_GRADIENT, fog.getGradient());
    }

    @Test
    void defaultConstantsMatchExpectedValues() {
        assertEquals(0.007f, Fog.DEFAULT_DENSITY);
        assertEquals(1.5f, Fog.DEFAULT_GRADIENT);
    }

    @Test
    void customValuesStored() {
        Fog fog = Fog.builder().density(0.01f).gradient(2.5f).build();
        assertEquals(0.01f, fog.getDensity());
        assertEquals(2.5f, fog.getGradient());
    }

    @Test
    void implementsComponent() {
        Fog fog = Fog.builder().build();
        assertInstanceOf(org.chapzlock.core.ecs.Component.class, fog);
    }
}
