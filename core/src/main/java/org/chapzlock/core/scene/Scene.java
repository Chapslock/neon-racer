package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;

import lombok.Getter;

/**
 * Immutable blueprint describing entities, systems, and input bindings for a scene.
 *
 * <p>A Scene is a pure data description that can be loaded, unloaded, and reloaded without
 * side effects. All content (terrain, lights, players) is defined as entities through the
 * {@link SceneBuilder}. Use {@link EntityBlueprintBuilder#gltfModel(String)} to load 3D
 * models as part of an entity.
 *
 * <p><strong>Immutability:</strong> Once built via {@link SceneBuilder#build()}, a Scene
 * cannot be modified. All lists are defensive copies.
 */
@Getter
public class Scene {
    private final String name;
    private final List<EntityBlueprint> entities;
    private final List<GameSystem> systems;
    private final InputMap inputMap;

    /**
     * Creates a scene with the specified configuration.
     *
     * <p>This constructor is typically called by {@link SceneBuilder#build()}, not directly
     * by game code. Use the builder for a cleaner API.
     *
     * @param name     unique name identifying this scene (used in logs and events)
     * @param entities list of entity blueprints to instantiate when the scene loads
     * @param systems  list of game systems active while this scene is loaded
     * @param inputMap optional input bindings (null if scene has no input handling)
     */
    public Scene(String name, List<EntityBlueprint> entities, List<GameSystem> systems, InputMap inputMap) {
        if (name == null) {
            throw new IllegalArgumentException(
                "Scene name cannot be null. Pass a name to identify the scene in logs and error messages.");
        }
        this.name = name;
        this.entities = List.copyOf(entities);
        this.systems = List.copyOf(systems);
        this.inputMap = inputMap;
    }

    public static SceneBuilder builder(String name) {
        return new SceneBuilder(name);
    }
}
