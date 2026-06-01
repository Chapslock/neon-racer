package org.chapzlock.core.component;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectionalLightTest {

    @Test
    void builderWithDirectionUsesDefaultColor() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new Vector3f(0, -1, 0))
            .build();

        assertEquals(new Vector3f(0, -1, 0), light.getDirection());
        assertSame(Color.WHITE, light.getColor());
    }

    @Test
    void builderWithCustomDirectionAndColor() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new Vector3f(-0.5f, -1f, 0.3f))
            .color(Color.RED)
            .build();

        assertEquals(new Vector3f(-0.5f, -1f, 0.3f), light.getDirection());
        assertSame(Color.RED, light.getColor());
    }

    @Test
    void builderWithoutDirectionThrows() {
        assertThrows(IllegalStateException.class, () ->
            DirectionalLight.builder().build()
        );
    }

    @Test
    void implementsComponent() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new Vector3f(0, -1, 0))
            .build();
        assertInstanceOf(org.chapzlock.core.ecs.Component.class, light);
    }
}
