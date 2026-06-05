package org.chapzlock.core.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.chapzlock.core.component.Transform;

/**
 * Pure data accumulator for declaring entity structure.
 *
 * <p>An EntityBuilder collects components, tags, and child definitions without
 * performing any side effects. It knows nothing about EntityFactory or the live
 * ECS world — it is simply a blueprint describing what an entity should look like.
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
 * <p>Usage for runtime spawning (via EntityFactory):</p>
 * <pre>{@code
 * EntityBuilder blueprint = EntityBuilder.create("bullet")
 *     .transform(Transform.at(x, y, z))
 *     .component(new Velocity(dir));
 * entityFactory.instantiate(blueprint);
 * }</pre>
 */
public class EntityBuilder {

    private final String name;
    private final List<Tag> tags = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private final List<EntityBuilder> children = new ArrayList<>();

    private EntityBuilder(String name) {
        this.name = name;
    }

    // ─── Static factories (new API) ─────────────────────────────────────────────

    /**
     * Creates a new builder for an entity with the given name.
     *
     * @param name the entity name (used for lookups via ComponentRegistry.getEntityByName)
     * @return a fresh EntityBuilder
     */
    public static EntityBuilder create(String name) {
        return new EntityBuilder(name);
    }

    /**
     * Creates a new builder for an anonymous entity (null name).
     *
     * @return a fresh EntityBuilder with no name
     */
    public static EntityBuilder create() {
        return new EntityBuilder(null);
    }

    // ─── Read-only accessors ────────────────────────────────────────────────────

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
     * Returns the accumulated child builders.
     *
     * @return unmodifiable view of the children list
     */
    public List<EntityBuilder> getChildren() {
        return Collections.unmodifiableList(children);
    }

    // ─── Fluent mutators ────────────────────────────────────────────────────────

    /**
     * Sets the initial transform for this entity.
     *
     * <p>Convenience method that delegates to {@link #component(Component...)}.
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
     * Declares a named child entity. The configurator receives a fresh EntityBuilder
     * for the child, which is stored as a nested blueprint.
     *
     * @param name         the child entity name
     * @param configurator function that attaches components to the child builder
     * @return this builder for chaining
     */
    public EntityBuilder child(String name, Consumer<EntityBuilder> configurator) {
        EntityBuilder childBuilder = new EntityBuilder(name);
        configurator.accept(childBuilder);
        children.add(childBuilder);
        return this;
    }

    /**
     * Declares an anonymous child entity. The configurator receives a fresh EntityBuilder
     * for the child, which is stored as a nested blueprint.
     *
     * @param configurator function that attaches components to the child builder
     * @return this builder for chaining
     */
    public EntityBuilder child(Consumer<EntityBuilder> configurator) {
        EntityBuilder childBuilder = new EntityBuilder(null);
        configurator.accept(childBuilder);
        children.add(childBuilder);
        return this;
    }

}
