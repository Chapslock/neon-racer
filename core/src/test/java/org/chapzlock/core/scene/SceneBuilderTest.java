package org.chapzlock.core.scene;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;
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
        Scene scene = Scene.builder("test").systems(sys).build();
        assertEquals(1, scene.getSystems().size());
        assertSame(sys, scene.getSystems().get(0));
    }

    @Test
    void directionalLightCreatesBlueprint() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new Vector3f(0, -1, 0)).build();
        Scene scene = Scene.builder("test").directionalLight(light).build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("directional-light", scene.getEntities().get(0).getName());
        assertSame(light, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void pointLightCreatesBlueprint() {
        PointLight light = PointLight.builder()
            .position(new Vector3f(1, 2, 3)).build();
        Scene scene = Scene.builder("test").pointLight(light).build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("point-light", scene.getEntities().get(0).getName());
        assertSame(light, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void fogCreatesBlueprint() {
        Fog fog = Fog.builder().build();
        Scene scene = Scene.builder("test").fog(fog).build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("fog", scene.getEntities().get(0).getName());
        assertSame(fog, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void ambientCreatesBlueprint() {
        AmbientLight ambient = AmbientLight.builder().strength(0.3f).build();
        Scene scene = Scene.builder("test").ambient(ambient).build();

        assertEquals(1, scene.getEntities().size());
        assertEquals("ambient-light", scene.getEntities().get(0).getName());
        assertSame(ambient, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void cameraConvenienceCreatesBlueprint() {
        Scene scene = Scene.builder("test").camera(0, 5, 10).build();
        assertEquals(1, scene.getEntities().size());
        assertEquals("camera", scene.getEntities().get(0).getName());
        assertInstanceOf(Camera.class, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void skyConvenienceCreatesBlueprint() {
        Scene scene = Scene.builder("test").sky().build();
        assertEquals(1, scene.getEntities().size());
        assertEquals("sky", scene.getEntities().get(0).getName());
        assertInstanceOf(Sky.class, scene.getEntities().get(0).getComponents().get(0));
    }

    @Test
    void multipleSystemsViaVarargs() {
        GameSystem s1 = new DummySystem();
        GameSystem s2 = new DummySystem();
        Scene scene = Scene.builder("test").systems(s1, s2).build();
        assertEquals(2, scene.getSystems().size());
    }
}
