package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.component.*;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;
import org.joml.Vector3f;

/**
 * Fluent builder for creating scenes.
 *
 * <p>This builder provides a declarative way to define what a scene contains: entities,
 * systems, input bindings, and optionally a glTF file as the scene's 3D base.
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * Scene race = Scene.builder("race-track")
 *     .fromGltf("models/track.glb")           // Load a 3D model as the base
 *     .entity("HUD", e -> e.sprite(...))      // Add programmatic entities
 *     .directionalLight(new DirectionalLight(...))
 *     .systems(new PlayerSystem(), new AISystem())
 *     .inputMap(raceInputs)
 *     .build();
 * }</pre>
 *
 * <p><strong>glTF integration:</strong> When {@link #fromGltf(String)} is called, the built
 * Scene carries the path so that SceneManager can load the glTF nodes alongside the
 * programmatic entities during scene instantiation.
 */
public class SceneBuilder {

    private final String name;
    private final List<EntityBlueprint> entities = new ArrayList<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private InputMap inputMap;
    private int namelessCounter = 0;

    /**
     * Optional path to a glTF/GLB file that provides additional entities for this scene.
     * Set via {@link #fromGltf(String)}. Null means no glTF base.
     */
    private String gltfPath;

    public SceneBuilder(String name) {
        this.name = name;
    }

    public SceneBuilder entity(String entityName, Consumer<EntityBlueprintBuilder> configurator) {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(entityName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    /**
     * Adds a nameless entity with the given components.
     *
     * <p>Nameless entities do not get a {@link org.chapzlock.core.component.Name} component
     * during instantiation. They are found via component-type queries
     * ({@code registry.getSingleton(T.class)} or {@code registry.forEach(T.class, ...)})
     * rather than by name.
     *
     * <p>Use this for singleton-style game state that doesn't need a human-readable identifier.
     *
     * @param components the components to attach to the entity
     * @return this builder for chaining
     */
    public SceneBuilder entity(Component... components) {
        String internalName = "_entity_" + namelessCounter++;
        entities.add(new EntityBlueprint(internalName, null, null, null,
            List.of(), List.of(components)));
        return this;
    }

    /** Adds a directional light entity to the scene. */
    public SceneBuilder directionalLight(DirectionalLight light) {
        entities.add(new EntityBlueprint("directional-light", null, null, null,
            List.of(), List.of(light)));
        return this;
    }

    /** Adds a point light entity to the scene. */
    public SceneBuilder pointLight(PointLight light) {
        entities.add(new EntityBlueprint("point-light", null, null, null,
            List.of(), List.of(light)));
        return this;
    }

    /** Adds a fog entity to the scene. No call = fog disabled. */
    public SceneBuilder fog(Fog fog) {
        entities.add(new EntityBlueprint("fog", null, null, null,
            List.of(), List.of(fog)));
        return this;
    }

    /** Adds an ambient light entity to the scene. No call = uses AmbientLight.DEFAULT_STRENGTH. */
    public SceneBuilder ambient(AmbientLight ambient) {
        entities.add(new EntityBlueprint("ambient-light", null, null, null,
            List.of(), List.of(ambient)));
        return this;
    }

    /** Adds a camera entity at the given position. */
    public SceneBuilder camera(float x, float y, float z) {
        entities.add(new EntityBlueprint("camera", null, null, null,
            List.of(), List.of(new Camera(new Vector3f(x, y, z)))));
        return this;
    }

    /** Adds a sky entity (background color). */
    public SceneBuilder sky() {
        entities.add(new EntityBlueprint("sky", null, null, null,
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

    /**
     * Marks this scene as loading from a glTF file as its 3D base.
     *
     * <p>When the scene is instantiated by the SceneManager, the glTF file will be loaded
     * and its nodes will be created as entities alongside any entities defined via
     * {@link #entity(String, Consumer)}. This allows combining a glTF world/level with
     * programmatic entities like HUD elements, triggers, or cameras.
     *
     * <p><strong>How it works:</strong> The built Scene stores the path, and the SceneManager
     * detects it during instantiation. The glTF nodes are loaded first, then the programmatic
     * entity blueprints are instantiated on top.
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * Scene.builder("race")
     *     .fromGltf("models/track.glb")
     *     .entity("HUD", e -> e.sprite(...))  // Additional programmatic entities
     *     .systems(new PlayerSystem())
     *     .build();
     * }</pre>
     *
     * @param resourcePath path to the glTF/GLB file relative to the resources root
     *                     (e.g., "models/track.glb")
     * @return this builder for chaining
     */
    public SceneBuilder fromGltf(String resourcePath) {
        this.gltfPath = resourcePath;
        return this;
    }

    /**
     * Returns the glTF path if this scene is glTF-based, or null otherwise.
     *
     * <p>This is used internally by SceneManager to detect whether it needs to
     * load a glTF file during scene instantiation.
     *
     * @return the resource path set via {@link #fromGltf(String)}, or null
     */
    public String getGltfPath() {
        return gltfPath;
    }

    /**
     * Builds the immutable Scene from the current builder state.
     *
     * <p>After calling this, the Scene is fully configured and ready to be loaded
     * by the SceneManager. The builder should not be reused after this call.
     *
     * @return a new immutable Scene instance
     */
    public Scene build() {
        return new Scene(name, entities, systems, inputMap, gltfPath);
    }
}
