package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmbientLightTest {

    @Test
    void defaultBuilderProducesDefaultStrength() {
        AmbientLight ambient = AmbientLight.builder().build();
        assertEquals(AmbientLight.DEFAULT_STRENGTH, ambient.getStrength());
    }

    @Test
    void defaultStrengthIsPointTwo() {
        assertEquals(0.2f, AmbientLight.DEFAULT_STRENGTH);
    }

    @Test
    void customStrengthStored() {
        AmbientLight ambient = AmbientLight.builder().strength(0.5f).build();
        assertEquals(0.5f, ambient.getStrength());
    }

    @Test
    void implementsComponent() {
        AmbientLight ambient = AmbientLight.builder().build();
        assertInstanceOf(org.chapzlock.core.ecs.Component.class, ambient);
    }
}
