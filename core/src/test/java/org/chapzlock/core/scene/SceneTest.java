package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.GameSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneTest {

    static class DummySystem implements GameSystem {}

    @Test
    void sceneHoldsNameEntitiesAndSystems() {
        GameSystem system = new DummySystem();

        Scene scene = Scene.builder("test-scene")
            .entity("player", e -> e.transform(Transform.at(0, 0, 0)))
            .systems(system)
            .build();

        assertEquals("test-scene", scene.getName());
        assertEquals(1, scene.getEntities().size());
        assertEquals("player", scene.getEntities().get(0).name());
        assertEquals(1, scene.getSystems().size());
        assertSame(system, scene.getSystems().get(0));
    }

    @Test
    void sceneIsImmutable() {
        Scene scene = Scene.builder("empty").build();

        assertThrows(UnsupportedOperationException.class, () ->
            scene.getEntities().add(new EntityRecipe("x", e -> {})));
        assertThrows(UnsupportedOperationException.class, () ->
            scene.getSystems().add(new DummySystem()));
    }

    @Test
    void sceneNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new Scene(null, List.of(), List.of(), null));
    }
}
