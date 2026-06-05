package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
 * <p>Usage for runtime spawning (requires EntityFactory):</p>
 * <pre>{@code
 * entityFactory.builder("bullet")
 *     .transform(Transform.at(x, y, z))
 *     .component(new Velocity(dir))
 *     .instantiate();
 * }</pre>
 */
public class EntityBuilder {

    private final String name;
    private final List<Tag> tags = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private final List<ChildRecipe> childRecipes = new ArrayList<>();

    /** Package-private: set by EntityFactory to enable the instantiate() terminal. */
    EntityFactory entityFactory;

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
     * Declares a named child entity that will be instantiated after this entity
     * and attached as a child in the hierarchy.
     *
     * @param name         the child entity name
     * @param configurator function that attaches components to the child builder
     * @return this builder for chaining
     */
    public EntityBuilder child(String name, Consumer<EntityBuilder> configurator) {
        childRecipes.add(new ChildRecipe(name, configurator));
        return this;
    }

    /**
     * Declares an anonymous child entity that will be instantiated after this entity
     * and attached as a child in the hierarchy.
     *
     * @param configurator function that attaches components to the child builder
     * @return this builder for chaining
     */
    public EntityBuilder child(Consumer<EntityBuilder> configurator) {
        childRecipes.add(new ChildRecipe(null, configurator));
        return this;
    }

    /**
     * Returns the accumulated child recipes.
     *
     * @return unmodifiable view of the child recipes list
     */
    public List<ChildRecipe> getChildRecipes() {
        return Collections.unmodifiableList(childRecipes);
    }

    /**
     * Spawns a live entity into the world using the linked {@link EntityFactory}.
     *
     * <p>This is the terminal operation for runtime entity creation. It passes the
     * builder's components and tags through the EntityFactory's phased assembly pipeline
     * (resolve, validate, commit).
     *
     * <p>Requires that this builder was created via {@link EntityFactory#builder(String)}.
     * Builders created directly (e.g., for scene declarations) do not have an EntityFactory
     * linked.
     *
     * @return the new entity ID, or -1 if validation failed
     * @throws IllegalStateException if no EntityFactory is linked to this builder
     */
    public int instantiate() {
        if (entityFactory == null) {
            throw new IllegalStateException(
                "Cannot call instantiate() without an EntityFactory. " +
                "Use EntityFactory.builder() for runtime spawning.");
        }
        return entityFactory.instantiate(name, components, tags, childRecipes);
    }

    /**
     * Package-private setter for the EntityFactory reference.
     *
     * <p>Called by EntityFactory when creating builders, enabling the {@code instantiate()}
     * terminal operation.
     *
     * @param entityFactory the entity factory that will handle instantiation
     */
    void setEntityFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }
}
