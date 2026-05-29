package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.GameSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneTest {

    static class DummySystem implements GameSystem {}

    @Test
    void sceneHoldsNameEntitiesAndSystems() {
        GameSystem system = new DummySystem();

        Scene scene = new Scene(
            "test-scene",
            List.of(new EntityBlueprint("player", List.of(
                ctx -> new Component[]{ new Transform() }
            ))),
            List.of(system),
            null
        );

        assertEquals("test-scene", scene.getName());
        assertEquals(1, scene.getEntities().size());
        assertEquals("player", scene.getEntities().get(0).getName());
        assertEquals(1, scene.getSystems().size());
        assertSame(system, scene.getSystems().get(0));
    }

    @Test
    void sceneIsImmutable() {
        Scene scene = new Scene("empty", List.of(), List.of(), null);

        assertThrows(UnsupportedOperationException.class, () ->
            scene.getEntities().add(new EntityBlueprint("x", List.of())));
        assertThrows(UnsupportedOperationException.class, () ->
            scene.getSystems().add(new DummySystem()));
    }

    @Test
    void entityBlueprintFactoriesProduceComponents() {
        EntityBlueprint blueprint = new EntityBlueprint("entity1", List.of(
            ctx -> new Component[]{ new Transform() }
        ));

        EngineContext testCtx = EngineContext.testContext();
        Component[] components = blueprint.getFactories().get(0).create(testCtx);

        assertEquals(1, components.length);
        assertInstanceOf(Transform.class, components[0]);
    }

    @Test
    void sceneNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new Scene(null, List.of(), List.of(), null));
    }
}
