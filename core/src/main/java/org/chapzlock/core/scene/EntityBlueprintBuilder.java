package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
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
 *     .sprite(SpriteDescriptor.of("textures/hud.png"))
 *     .tag(new PlayerTag())
 *     .component(new PlayerInputComponent())
 *     .build()
 * }</pre>
 */
public class EntityBlueprintBuilder {

    private final String name;
    private Transform transform;
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
        return new EntityBlueprint(name, transform,
            spriteDescriptor, physicsDescriptor, tags, components);
    }

    private void validate() {
        // No conflicting descriptors to check currently.
        // Sprites are the only visual descriptor in the blueprint system now;
        // 3D models are loaded via glTF directly through SceneManager.
    }
}
