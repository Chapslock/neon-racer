package org.chapzlock.core.component;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointLightTest {

    @Test
    void builderWithPositionUsesDefaults() {
        PointLight light = PointLight.builder()
            .position(new Vector3f(1, 2, 3))
            .build();

        assertEquals(new Vector3f(1, 2, 3), light.getPosition());
        assertSame(Color.WHITE, light.getColor());
        assertSame(Attenuation.DEFAULT, light.getAttenuation());
    }

    @Test
    void builderWithCustomValues() {
        PointLight light = PointLight.builder()
            .position(new Vector3f(5, 10, 15))
            .color(Color.BLUE)
            .attenuation(Attenuation.SHORT_RANGE)
            .build();

        assertEquals(new Vector3f(5, 10, 15), light.getPosition());
        assertSame(Color.BLUE, light.getColor());
        assertSame(Attenuation.SHORT_RANGE, light.getAttenuation());
    }

    @Test
    void builderWithoutPositionThrows() {
        assertThrows(IllegalStateException.class, () ->
            PointLight.builder().build()
        );
    }

    @Test
    void maxCountIsEight() {
        assertEquals(8, PointLight.MAX_COUNT);
    }

    @Test
    void implementsComponent() {
        PointLight light = PointLight.builder()
            .position(new Vector3f(0, 0, 0))
            .build();
        assertInstanceOf(org.chapzlock.core.ecs.Component.class, light);
    }
}
