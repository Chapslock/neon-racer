package org.chapzlock.core.scene;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.GameSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneBuilderTest {

    static class DummySystem implements GameSystem {}

    @Test
    void builderCreatesSceneWithName() {
        Scene scene = Scene.builder("my-scene").build();
        assertEquals("my-scene", scene.getName());
    }

    @Test
    void builderAddsEntitiesViaConsumer() {
        Scene scene = Scene.builder("test")
            .entity("player", e -> e.transform(Transform.at(1, 2, 3)))
            .entity("enemy", e -> e.transform(Transform.at(4, 5, 6)))
            .build();

        assertEquals(2, scene.getEntities().size());
        assertEquals("player", scene.getEntities().get(0).getName());
        assertEquals("enemy", scene.getEntities().get(1).getName());
    }

    @Test
    void builderAddsSystems() {
        GameSystem sys = new DummySystem();
        Scene scene = Scene.builder("test")
            .systems(sys)
            .build();

        assertEquals(1, scene.getSystems().size());
        assertSame(sys, scene.getSystems().get(0));
    }

    @Test
    void pointLightConvenienceCreatesBlueprint() {
        Scene scene = Scene.builder("test")
            .pointLight(1, 2, 3)
            .build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("point-light", scene.getEntities().get(0).getName());
        assertInstanceOf(PointLight.class, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void cameraConvenienceCreatesBlueprint() {
        Scene scene = Scene.builder("test")
            .camera(0, 5, 10)
            .build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("camera", scene.getEntities().get(0).getName());
        assertInstanceOf(Camera.class, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void skyConvenienceCreatesBlueprint() {
        Scene scene = Scene.builder("test")
            .sky()
            .build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("sky", scene.getEntities().get(0).getName());
        assertInstanceOf(Sky.class, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void fogDefaultValues() {
        Scene scene = Scene.builder("test").build();
        assertEquals(0.007f, scene.getFogDensity(), 0.0001f);
        assertEquals(1.5f, scene.getFogGradient(), 0.0001f);
    }

    @Test
    void fogCustomValues() {
        Scene scene = Scene.builder("test").fog(0.01f, 2.0f).build();
        assertEquals(0.01f, scene.getFogDensity(), 0.0001f);
        assertEquals(2.0f, scene.getFogGradient(), 0.0001f);
    }

    @Test
    void multipleSystemsViaVarargs() {
        GameSystem s1 = new DummySystem();
        GameSystem s2 = new DummySystem();
        Scene scene = Scene.builder("test")
            .systems(s1, s2)
            .build();

        assertEquals(2, scene.getSystems().size());
    }
}
