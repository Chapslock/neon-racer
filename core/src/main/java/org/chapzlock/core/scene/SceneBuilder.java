package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;
import org.joml.Vector3f;

/**
 * Fluent builder for creating scenes.
 */
public class SceneBuilder {

    private final String name;
    private final List<EntityBlueprint> entities = new ArrayList<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private InputMap inputMap;

    public SceneBuilder(String name) {
        this.name = name;
    }

    public SceneBuilder entity(String entityName, Consumer<EntityBlueprintBuilder> configurator) {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(entityName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    /** Adds a directional light entity to the scene. */
    public SceneBuilder directionalLight(DirectionalLight light) {
        entities.add(new EntityBlueprint("directional-light", null, null, null, null, null,
            List.of(), List.of(light)));
        return this;
    }

    /** Adds a point light entity to the scene. */
    public SceneBuilder pointLight(PointLight light) {
        entities.add(new EntityBlueprint("point-light", null, null, null, null, null,
            List.of(), List.of(light)));
        return this;
    }

    /** Adds a fog entity to the scene. No call = fog disabled. */
    public SceneBuilder fog(Fog fog) {
        entities.add(new EntityBlueprint("fog", null, null, null, null, null,
            List.of(), List.of(fog)));
        return this;
    }

    /** Adds an ambient light entity to the scene. No call = uses AmbientLight.DEFAULT_STRENGTH. */
    public SceneBuilder ambient(AmbientLight ambient) {
        entities.add(new EntityBlueprint("ambient-light", null, null, null, null, null,
            List.of(), List.of(ambient)));
        return this;
    }

    /** Adds a camera entity at the given position. */
    public SceneBuilder camera(float x, float y, float z) {
        entities.add(new EntityBlueprint("camera", null, null, null, null, null,
            List.of(), List.of(new Camera(new Vector3f(x, y, z)))));
        return this;
    }

    /** Adds a sky entity (background color). */
    public SceneBuilder sky() {
        entities.add(new EntityBlueprint("sky", null, null, null, null, null,
            List.of(), List.of(new Sky())));
        return this;
    }

    public SceneBuilder systems(GameSystem... gameSystems) {
        systems.addAll(List.of(gameSystems));
        return this;
    }

    public SceneBuilder inputMap(InputMap inputMap) {
        this.inputMap = inputMap;
        return this;
    }

    public Scene build() {
        return new Scene(name, entities, systems, inputMap);
    }
}
