package org.chapzlock.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointLightTest {

    @Test
    void builderWithDefaultsUsesWhiteAndDefaultAttenuation() {
        PointLight light = PointLight.builder().build();

        assertSame(Color.WHITE, light.getColor());
        assertSame(Attenuation.DEFAULT, light.getAttenuation());
    }

    @Test
    void builderWithCustomValues() {
        PointLight light = PointLight.builder()
            .color(Color.BLUE)
            .attenuation(Attenuation.SHORT_RANGE)
            .build();

        assertSame(Color.BLUE, light.getColor());
        assertSame(Attenuation.SHORT_RANGE, light.getAttenuation());
    }

    @Test
    void maxCountIsEight() {
        assertEquals(8, PointLight.MAX_COUNT);
    }

    @Test
    void implementsComponent() {
        PointLight light = PointLight.builder().build();
        assertInstanceOf(org.chapzlock.core.ecs.Component.class, light);
    }
}
