package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;

import lombok.Getter;

/**
 * Immutable blueprint describing entities, systems, and input bindings for a scene.
 *
 * <p>A Scene can optionally be backed by a glTF file. When a glTF path is set (via
 * {@link SceneBuilder#fromGltf(String)}), the SceneManager will automatically load
 * and instantiate the 3D model's nodes as entities alongside any entities defined
 * programmatically through the builder.
 *
 * <p><strong>Immutability:</strong> Once built via {@link SceneBuilder#build()}, a Scene
 * cannot be modified. All lists are defensive copies. The Scene is a pure data description
 * that can be loaded, unloaded, and reloaded without side effects.
 */
@Getter
public class Scene {
    private final String name;
    private final List<EntityBlueprint> entities;
    private final List<GameSystem> systems;
    private final InputMap inputMap;

    /**
     * Optional path to a glTF/GLB file that provides additional entities for this scene.
     * When non-null, SceneManager will load and instantiate the glTF node hierarchy
     * during scene instantiation. Null means this scene has no glTF base.
     */
    private final String gltfPath;

    /**
     * Creates a scene with all configuration options.
     *
     * <p>This constructor is typically called by {@link SceneBuilder#build()}, not directly
     * by game code. Use the builder for a cleaner API.
     *
     * @param name     unique name identifying this scene (used in logs and events)
     * @param entities list of entity blueprints to instantiate when the scene loads
     * @param systems  list of game systems active while this scene is loaded
     * @param inputMap optional input bindings (null if scene has no input handling)
     * @param gltfPath optional path to a glTF/GLB file (null if no glTF base)
     */
    public Scene(String name, List<EntityBlueprint> entities, List<GameSystem> systems,
                 InputMap inputMap, String gltfPath) {
        if (name == null) {
            throw new IllegalArgumentException(
                "Scene name cannot be null. Pass a name to identify the scene in logs and error messages.");
        }
        this.name = name;
        this.entities = List.copyOf(entities);
        this.systems = List.copyOf(systems);
        this.inputMap = inputMap;
        this.gltfPath = gltfPath;
    }

    /**
     * Creates a scene without a glTF base.
     *
     * <p>This overload maintains backwards compatibility for existing code that constructs
     * Scene objects without specifying a glTF path. Equivalent to passing {@code null}
     * for the gltfPath parameter.
     *
     * @param name     unique name identifying this scene
     * @param entities list of entity blueprints to instantiate
     * @param systems  list of game systems active while this scene is loaded
     * @param inputMap optional input bindings (null if scene has no input handling)
     */
    public Scene(String name, List<EntityBlueprint> entities, List<GameSystem> systems, InputMap inputMap) {
        this(name, entities, systems, inputMap, null);
    }

    public static SceneBuilder builder(String name) {
        return new SceneBuilder(name);
    }
}
