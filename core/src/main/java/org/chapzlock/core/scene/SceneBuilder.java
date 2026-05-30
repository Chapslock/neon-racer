package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;
import org.joml.Vector3f;

/**
 * Fluent builder for creating scenes.
 *
 * <p>Convenience methods: {@code camera()}, {@code pointLight()}, {@code sky()}.
 * For custom entities, use {@code entity(name, configurator)}.
 */
public class SceneBuilder {

    private final String name;
    private final List<EntityBlueprint> entities = new ArrayList<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private InputMap inputMap;

    public SceneBuilder(String name) {
        this.name = name;
    }

    /**
     * Adds a custom entity to the scene.
     *
     * @param entityName entity name (for debugging)
     * @param configurator builder callback
     * @return this builder
     */
    public SceneBuilder entity(String entityName, Consumer<EntityBlueprintBuilder> configurator) {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(entityName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    /** Adds a white point light at the given position. */
    public SceneBuilder pointLight(float x, float y, float z) {
        return pointLight(x, y, z, Color.WHITE);
    }

    /** Adds a point light at the given position with the specified color. */
    public SceneBuilder pointLight(float x, float y, float z, Color color) {
        // Temporary: creates a blueprint with the new signature, passes component directly
        entities.add(new EntityBlueprint("point-light", null, null, null, null, null,
            List.of(), List.of(new PointLight(new Vector3f(x, y, z), color))));
        return this;
    }

    /** Adds a camera entity at the given position. */
    public SceneBuilder camera(float x, float y, float z) {
        // Temporary: creates a blueprint with the new signature, passes component directly
        entities.add(new EntityBlueprint("camera", null, null, null, null, null,
            List.of(), List.of(new Camera(new Vector3f(x, y, z)))));
        return this;
    }

    /** Adds a sky entity (background color). */
    public SceneBuilder sky() {
        // Temporary: creates a blueprint with the new signature, passes component directly
        entities.add(new EntityBlueprint("sky", null, null, null, null, null,
            List.of(), List.of(new Sky())));
        return this;
    }

    /**
     * Adds game-specific systems to the scene. These are removed when the scene unloads.
     */
    public SceneBuilder systems(GameSystem... gameSystems) {
        systems.addAll(List.of(gameSystems));
        return this;
    }

    /**
     * Sets the input map for this scene. Automatically activated on load, deactivated on unload.
     */
    public SceneBuilder inputMap(InputMap inputMap) {
        this.inputMap = inputMap;
        return this;
    }

    /** Builds the scene. */
    public Scene build() {
        return new Scene(name, entities, systems, inputMap);
    }
}
