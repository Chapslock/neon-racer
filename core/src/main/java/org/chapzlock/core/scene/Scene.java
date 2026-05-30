package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;

import lombok.Getter;

/**
 * Immutable blueprint describing entities, systems, and input bindings for a scene.
 *
 * <p>Scenes are built via a fluent builder:
 * <pre>{@code
 * Scene.builder("MainMenu")
 *     .camera(0, 5, 10)
 *     .entity("player", e -> e.model("player.obj").transform(0, 0, 0))
 *     .systems(new PlayerControllerSystem())
 *     .inputMap(myInputMap)
 *     .build();
 * }</pre>
 */
@Getter
public class Scene {
    private final String name;
    private final List<EntityBlueprint> entities;
    private final List<GameSystem> systems;
    private final InputMap inputMap;
    private final float fogDensity;
    private final float fogGradient;

    public Scene(String name, List<EntityBlueprint> entities, List<GameSystem> systems,
                 InputMap inputMap, float fogDensity, float fogGradient) {
        if (name == null) {
            throw new IllegalArgumentException(
                "Scene name cannot be null. Pass a name to identify the scene in logs and error messages.");
        }
        this.name = name;
        this.entities = List.copyOf(entities);
        this.systems = List.copyOf(systems);
        this.inputMap = inputMap;
        this.fogDensity = fogDensity;
        this.fogGradient = fogGradient;
    }

    /**
     * Creates a new scene builder.
     *
     * @param name scene name (used in logs and events)
     * @return a new scene builder
     */
    public static SceneBuilder builder(String name) {
        return new SceneBuilder(name);
    }
}
