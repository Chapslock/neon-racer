package org.chapzlock.core.scene;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.EntityBuilder;
import org.chapzlock.core.ecs.GameSystem;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneBuilderTest {

    static class DummySystem implements GameSystem {}

    /**
     * Helper: applies a recipe's configurator to a fresh EntityBuilder and returns it.
     */
    private EntityBuilder applyRecipe(EntityRecipe recipe) {
        EntityBuilder builder = EntityBuilder.create(recipe.name());
        recipe.configurator().accept(builder);
        return builder;
    }

    @Test
    void builderCreatesSceneWithName() {
        Scene scene = Scene.builder("my-scene").build();
        assertEquals("my-scene", scene.getName());
    }

    @Test
    void namedEntityAddsRecipe() {
        Scene scene = Scene.builder("test")
            .entity("player", e -> e.transform(Transform.at(1, 2, 3)))
            .entity("enemy", e -> e.transform(Transform.at(4, 5, 6)))
            .build();

        assertEquals(2, scene.getEntityRecipes().size());
        assertEquals("player", scene.getEntityRecipes().get(0).name());
        assertEquals("enemy", scene.getEntityRecipes().get(1).name());
    }

    @Test
    void namelessEntityGetsInternalName() {
        Scene scene = Scene.builder("test")
            .entity(e -> e.transform(Transform.at(0, 5, 10)).component(new Camera()))
            .build();

        assertEquals(1, scene.getEntityRecipes().size());
        assertTrue(scene.getEntityRecipes().get(0).name().startsWith("_entity_"));
    }

    @Test
    void builderAddsSystems() {
        GameSystem sys = new DummySystem();
        Scene scene = Scene.builder("test").systems(sys).build();
        assertEquals(1, scene.getSystems().size());
        assertSame(sys, scene.getSystems().get(0));
    }

    @Test
    void entityBuilderDirectionalLight() {
        DirectionalLight light = DirectionalLight.builder()
            .direction(new Vector3f(0, -1, 0)).build();
        Scene scene = Scene.builder("test")
            .entity("sun", e -> e.component(light))
            .build();

        assertEquals(1, scene.getEntityRecipes().size());
        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().contains(light));
    }

    @Test
    void entityBuilderPointLight() {
        PointLight light = PointLight.builder()
            .color(new Color(1, 0, 0)).build();
        Scene scene = Scene.builder("test")
            .entity("torch", e -> e.component(light))
            .build();

        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().contains(light));
    }

    @Test
    void entityBuilderFog() {
        Fog fog = Fog.builder().build();
        Scene scene = Scene.builder("test")
            .entity(e -> e.component(fog))
            .build();

        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().contains(fog));
    }

    @Test
    void entityBuilderAmbient() {
        AmbientLight ambient = AmbientLight.builder().strength(0.3f).build();
        Scene scene = Scene.builder("test")
            .entity(e -> e.component(ambient))
            .build();

        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().contains(ambient));
    }

    @Test
    void entityBuilderCamera() {
        Camera cam = new Camera();
        Scene scene = Scene.builder("test")
            .entity(e -> e.transform(Transform.at(0, 5, 10)).component(cam))
            .build();

        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().contains(cam));
    }

    @Test
    void entityBuilderSky() {
        Scene scene = Scene.builder("test")
            .entity(e -> e.component(new Sky()))
            .build();

        EntityBuilder applied = applyRecipe(scene.getEntityRecipes().get(0));
        assertTrue(applied.getComponents().stream()
            .anyMatch(c -> c instanceof Sky));
    }

    @Test
    void multipleSystemsViaVarargs() {
        GameSystem s1 = new DummySystem();
        GameSystem s2 = new DummySystem();
        Scene scene = Scene.builder("test").systems(s1, s2).build();
        assertEquals(2, scene.getSystems().size());
    }
}
