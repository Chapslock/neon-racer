package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.input.InputMap;

/**
 * Fluent builder for creating scenes.
 *
 * <p>A scene is a collection of entity blueprints, game systems, and optional input bindings.
 * Every piece of scene content (lights, cameras, fog, terrain, players) is defined as an entity
 * using the {@link EntityBlueprintBuilder}.
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * Scene race = Scene.builder("race-track")
 *     .entity("terrain", e -> e.gltfModel("models/track.glb"))
 *     .entity("sun", e -> e.directionalLight(DirectionalLight.builder()...build()))
 *     .entity("player", e -> e.transform(Transform.at(0, 0, 0)).gltfModel("glb/car.glb"))
 *     .entity(e -> e.camera(new Camera(new Vector3f(0, 5, 10))))
 *     .entity(e -> e.sky())
 *     .systems(new PlayerSystem(), new AISystem())
 *     .inputMap(raceInputs)
 *     .build();
 * }</pre>
 */
public class SceneBuilder {

    private final String name;
    private final List<EntityBlueprint> entities = new ArrayList<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private InputMap inputMap;
    private int namelessCounter = 0;

    public SceneBuilder(String name) {
        this.name = name;
    }

    /**
     * Adds a named entity to the scene.
     *
     * <p>Named entities get a {@link org.chapzlock.core.component.Name} component during
     * instantiation, allowing lookup via {@code registry.getEntityByName("name")}.
     *
     * @param entityName the entity name (used for lookup and debugging)
     * @param configurator lambda to configure the entity's components
     * @return this builder for chaining
     */
    public SceneBuilder entity(String entityName, Consumer<EntityBlueprintBuilder> configurator) {
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(entityName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    /**
     * Adds a nameless entity to the scene.
     *
     * <p>Nameless entities do not get a {@link org.chapzlock.core.component.Name} component.
     * They are found via component-type queries ({@code registry.getSingleton(T.class)} or
     * {@code registry.forEach(T.class, ...)}) rather than by name.
     *
     * @param configurator lambda to configure the entity's components
     * @return this builder for chaining
     */
    public SceneBuilder entity(Consumer<EntityBlueprintBuilder> configurator) {
        String internalName = "_entity_" + namelessCounter++;
        EntityBlueprintBuilder builder = new EntityBlueprintBuilder(internalName);
        configurator.accept(builder);
        entities.add(builder.build());
        return this;
    }

    /**
     * Adds game systems that are active while this scene is loaded.
     *
     * @param gameSystems the systems to add
     * @return this builder for chaining
     */
    public SceneBuilder systems(GameSystem... gameSystems) {
        systems.addAll(List.of(gameSystems));
        return this;
    }

    /**
     * Sets the input bindings for this scene.
     *
     * @param inputMap the input map to activate when this scene loads
     * @return this builder for chaining
     */
    public SceneBuilder inputMap(InputMap inputMap) {
        this.inputMap = inputMap;
        return this;
    }

    /**
     * Builds the immutable Scene from the current builder state.
     *
     * @return a new immutable Scene instance
     */
    public Scene build() {
        return new Scene(name, entities, systems, inputMap);
    }
}
