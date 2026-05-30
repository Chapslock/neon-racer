package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.component.ModelDescriptor;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.TerrainDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;


/**
 * Fluent builder for configuring entity blueprints.
 *
 * <p>Stores descriptor components and game-logic components, validates
 * configuration at build time, and produces an immutable {@link EntityBlueprint}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * new EntityBlueprintBuilder("player")
 *     .transform(Transform.at(0, 10, -5, 0, 0, 180))
 *     .model(ModelDescriptor.of("obj/car.obj", "textures/car.png"))
 *     .physics(PhysicsDescriptor.dynamic(1f))
 *     .tag(new PlayerTag())
 *     .component(new PlayerInputComponent())
 *     .build()
 * }</pre>
 */
public class EntityBlueprintBuilder {

    private final String name;
    private Transform transform;
    private ModelDescriptor modelDescriptor;
    private TerrainDescriptor terrainDescriptor;
    private SpriteDescriptor spriteDescriptor;
    private PhysicsDescriptor physicsDescriptor;
    private final List<Tag> tags = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();

    public EntityBlueprintBuilder(String name) {
        this.name = name;
    }

    /** Sets the initial transform for this entity. */
    public EntityBlueprintBuilder transform(Transform transform) {
        this.transform = transform;
        return this;
    }

/** Declares a 3D model to load for this entity. */
    public EntityBlueprintBuilder model(ModelDescriptor descriptor) {
        this.modelDescriptor = descriptor;
        return this;
    }

    /** Declares terrain to generate for this entity. */
    public EntityBlueprintBuilder terrain(TerrainDescriptor descriptor) {
        this.terrainDescriptor = descriptor;
        return this;
    }

    /** Declares a 2D sprite for this entity. */
    public EntityBlueprintBuilder sprite(SpriteDescriptor descriptor) {
        this.spriteDescriptor = descriptor;
        return this;
    }

    /** Declares physics body configuration for this entity. */
    public EntityBlueprintBuilder physics(PhysicsDescriptor descriptor) {
        this.physicsDescriptor = descriptor;
        return this;
    }

    /** Adds a tag for query filtering. */
    public EntityBlueprintBuilder tag(Tag tagInstance) {
        this.tags.add(tagInstance);
        return this;
    }

    /** Adds game-logic components directly. */
    public EntityBlueprintBuilder component(Component... components) {
        this.components.addAll(List.of(components));
        return this;
    }

/**
     * Validates configuration and produces an immutable EntityBlueprint.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public EntityBlueprint build() {
        validate();
        return new EntityBlueprint(name, transform, modelDescriptor, terrainDescriptor,
            spriteDescriptor, physicsDescriptor, tags, components);
    }

    private void validate() {
        if (modelDescriptor != null && terrainDescriptor != null) {
            throw new IllegalStateException(
                "Entity '" + name + "': model() and terrain() are mutually exclusive");
        }
        if (modelDescriptor != null && spriteDescriptor != null) {
            throw new IllegalStateException(
                "Entity '" + name + "': model() and sprite() are mutually exclusive");
        }
        if (terrainDescriptor != null && spriteDescriptor != null) {
            throw new IllegalStateException(
                "Entity '" + name + "': terrain() and sprite() are mutually exclusive");
        }
        if (physicsDescriptor != null && modelDescriptor == null && terrainDescriptor == null) {
            throw new IllegalStateException(
                "Entity '" + name + "': physics() requires model() or terrain()");
        }
    }
}
