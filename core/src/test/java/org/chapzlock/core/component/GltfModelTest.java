package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GltfModelTest {

    @Test
    void implementsComponent() {
        GltfModel model = new GltfModel("glb/car.glb");
        assertInstanceOf(Component.class, model);
    }

    @Test
    void storesResourcePath() {
        GltfModel model = new GltfModel("models/terrain.glb");
        assertEquals("models/terrain.glb", model.resourcePath());
    }
}
