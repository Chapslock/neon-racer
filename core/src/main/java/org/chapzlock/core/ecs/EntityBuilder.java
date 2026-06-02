package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.component.*;
import org.chapzlock.core.scene.EntityBlueprint;

/**
 * Fluent builder for constructing entities, used both in scene declarations and at runtime.
 *
 * <p>This builder has two terminal operations:
 * <ul>
 *   <li>{@link #build()} — produces an immutable {@link EntityBlueprint} for scene declarations</li>
 *   <li>{@code instantiate()} — spawns a live entity into the world (added by EntitySystem)</li>
 * </ul>
 *
 * <p>Usage in scene declarations:</p>
 * <pre>{@code
 * Scene.builder("race")
 *     .entity("player", e -> e
 *         .transform(Transform.at(0, 10, -5))
 *         .gltfModel("glb/car.glb")
 *         .tag(new PlayerTag())
 *         .component(new PlayerInputComponent()))
 *     .build();
 * }</pre>
 *
 * <p>Usage for runtime spawning (requires EntitySystem):</p>
 * <pre>{@code
 * entitySystem.builder("bullet")
 *     .transform(Transform.at(x, y, z))
 *     .component(new Velocity(dir))
 *     .instantiate();
 * }</pre>
 */
public class EntityBuilder {

    private final String name;
    private SpriteDescriptor spriteDescriptor;
    private PhysicsDescriptor physicsDescriptor;
    private final List<Tag> tags = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private String gltfModelPath;

    /** Package-private: set by EntitySystem to enable the instantiate() terminal. */
    EntitySystem entitySystem;

    /**
     * Creates a new builder for an entity with the given name.
     *
     * @param name the entity name (nullable for anonymous entities)
     */
    public EntityBuilder(String name) {
        this.name = name;
    }

    /**
     * Sets the initial transform for this entity.
     *
     * @param transform the spatial transform (position, rotation, scale)
     * @return this builder for chaining
     */
    public EntityBuilder transform(Transform transform) {
        return component(transform);
    }

    /**
     * Declares a 2D sprite for this entity.
     *
     * <p>During scene instantiation, the sprite descriptor is resolved into
     * runtime rendering components (SpriteMaterial, Mesh, etc.).
     *
     * @param descriptor the sprite configuration
     * @return this builder for chaining
     */
    public EntityBuilder sprite(SpriteDescriptor descriptor) {
        this.spriteDescriptor = descriptor;
        return this;
    }

    /**
     * Declares physics body configuration for this entity.
     *
     * <p>During scene instantiation, the physics descriptor is resolved into
     * a PhysicsBody component and registered with the physics world.
     *
     * @param descriptor the physics body configuration
     * @return this builder for chaining
     */
    public EntityBuilder physics(PhysicsDescriptor descriptor) {
        this.physicsDescriptor = descriptor;
        return this;
    }

    /**
     * Adds a tag for query filtering.
     *
     * <p>Tags are empty marker components used to group entities for ECS queries.
     * For example, adding a {@code PlayerTag} allows systems to query all player entities.
     *
     * @param tagInstance the tag to add
     * @return this builder for chaining
     */
    public EntityBuilder tag(Tag tagInstance) {
        this.tags.add(tagInstance);
        return this;
    }

    /**
     * Adds one or more game-logic components directly.
     *
     * <p>Components added here are attached to the entity as-is during instantiation,
     * without any descriptor resolution step.
     *
     * @param components the components to add
     * @return this builder for chaining
     */
    public EntityBuilder component(Component... components) {
        this.components.addAll(List.of(components));
        return this;
    }

    /**
     * Adds a directional light component.
     *
     * @param light the directional light configuration
     * @return this builder for chaining
     */
    public EntityBuilder directionalLight(DirectionalLight light) {
        return component(light);
    }

    /**
     * Adds a point light component.
     *
     * @param light the point light configuration
     * @return this builder for chaining
     */
    public EntityBuilder pointLight(PointLight light) {
        return component(light);
    }

    /**
     * Adds a fog component.
     *
     * @param fog the fog configuration
     * @return this builder for chaining
     */
    public EntityBuilder fog(Fog fog) {
        return component(fog);
    }

    /**
     * Adds an ambient light component.
     *
     * @param ambient the ambient light configuration
     * @return this builder for chaining
     */
    public EntityBuilder ambient(AmbientLight ambient) {
        return component(ambient);
    }

    /**
     * Adds a camera component.
     *
     * @param camera the camera configuration
     * @return this builder for chaining
     */
    public EntityBuilder camera(Camera camera) {
        return component(camera);
    }

    /**
     * Adds a sky component with default settings.
     *
     * <p>The sky controls background clear color and distance fog rendering.
     * Only one sky entity should exist per scene.
     *
     * @return this builder for chaining
     */
    public EntityBuilder sky() {
        return component(new Sky());
    }

    /**
     * Declares a glTF model to instantiate as children of this entity.
     *
     * <p>When the scene is instantiated, the glTF file is loaded and its node tree
     * is parented under this entity. This makes the entity the "owner" of the model
     * — moving it will move the entire model. Animation clips from the glTF are also
     * attached to this entity via AnimationClipSet and AnimationState components.
     *
     * @param resourcePath path to the glTF/GLB file relative to the resources root
     * @return this builder for chaining
     */
    public EntityBuilder gltfModel(String resourcePath) {
        this.gltfModelPath = resourcePath;
        return this;
    }

    /**
     * Produces an immutable {@link EntityBlueprint} from the current builder state.
     *
     * <p>This is the terminal operation for scene declarations. The returned blueprint
     * is stored in a Scene and instantiated later by SceneManager.
     *
     * @return an immutable entity blueprint
     */
    public EntityBlueprint build() {
        return new EntityBlueprint(name, spriteDescriptor, physicsDescriptor, tags, components, gltfModelPath);
    }

    /**
     * Spawns a live entity into the world using the linked {@link EntitySystem}.
     *
     * <p>This is the terminal operation for runtime entity creation. It builds the blueprint
     * from the current builder state, then passes it through the EntitySystem's phased
     * assembly pipeline (resolve, validate, commit).
     *
     * <p>Requires that this builder was created via {@link EntitySystem#builder(String)}.
     * Builders created directly (e.g., for scene declarations) do not have an EntitySystem
     * linked and should use {@link #build()} instead.
     *
     * @return the new entity ID, or -1 if validation failed
     * @throws IllegalStateException if no EntitySystem is linked to this builder
     */
    public int instantiate() {
        if (entitySystem == null) {
            throw new IllegalStateException(
                "Cannot call instantiate() without an EntitySystem. " +
                "Use EntitySystem.builder() for runtime spawning, or call build() for scene declarations.");
        }
        return entitySystem.instantiate(build());
    }

    /**
     * Package-private setter for the EntitySystem reference.
     *
     * <p>Called by EntitySystem when creating builders, enabling the {@code instantiate()}
     * terminal operation.
     *
     * @param entitySystem the entity system that will handle instantiation
     */
    void setEntitySystem(EntitySystem entitySystem) {
        this.entitySystem = entitySystem;
    }
}
