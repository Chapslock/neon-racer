package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chapzlock.core.component.Transform;

/**
 * Fluent builder for constructing entities, used both in scene declarations and at runtime.
 *
 * <p>Components are added directly via {@link #component(Component...)} or convenience
 * methods that delegate to it. Tags are added via {@link #tag(Tag)}.
 *
 * <p>Usage in scene declarations:</p>
 * <pre>{@code
 * Scene.builder("race")
 *     .entity("player", e -> e
 *         .transform(Transform.at(0, 10, -5))
 *         .component(new GltfModel("glb/car.glb"))
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
    private final List<Tag> tags = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();

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
     * Returns the entity name.
     *
     * @return the name, or null for anonymous entities
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the accumulated tags.
     *
     * @return unmodifiable view of the tags list
     */
    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * Returns the accumulated components.
     *
     * @return unmodifiable view of the components list
     */
    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
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
     * @param components the components to add
     * @return this builder for chaining
     */
    public EntityBuilder component(Component... components) {
        Collections.addAll(this.components, components);
        return this;
    }

    /**
     * Spawns a live entity into the world using the linked {@link EntitySystem}.
     *
     * <p>This is the terminal operation for runtime entity creation. It passes the
     * builder's components and tags through the EntitySystem's phased assembly pipeline
     * (resolve, validate, commit).
     *
     * <p>Requires that this builder was created via {@link EntitySystem#builder(String)}.
     * Builders created directly (e.g., for scene declarations) do not have an EntitySystem
     * linked.
     *
     * @return the new entity ID, or -1 if validation failed
     * @throws IllegalStateException if no EntitySystem is linked to this builder
     */
    public int instantiate() {
        if (entitySystem == null) {
            throw new IllegalStateException(
                "Cannot call instantiate() without an EntitySystem. " +
                "Use EntitySystem.builder() for runtime spawning.");
        }
        return entitySystem.instantiate(name, components, tags);
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
