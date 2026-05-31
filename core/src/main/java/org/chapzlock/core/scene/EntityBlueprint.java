package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.component.ModelDescriptor;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;

import lombok.Getter;

/**
 * Immutable blueprint for an entity, produced by {@link EntityBlueprintBuilder}.
 *
 * <p>Contains the entity's initial transform, rendering descriptor,
 * physics descriptor, tags, and game-logic components. The
 * {@link DescriptorResolver} consumes descriptors at scene load time
 * to produce runtime components.</p>
 */
@Getter
public final class EntityBlueprint {
    private final String name;
    private final Transform transform;
    private final ModelDescriptor modelDescriptor;
    private final SpriteDescriptor spriteDescriptor;
    private final PhysicsDescriptor physicsDescriptor;
    private final List<Tag> tags;
    private final List<Component> components;

    public EntityBlueprint(String name, Transform transform, ModelDescriptor modelDescriptor,
                           SpriteDescriptor spriteDescriptor,
                           PhysicsDescriptor physicsDescriptor, List<Tag> tags, List<Component> components) {
        this.name = name;
        this.transform = transform;
        this.modelDescriptor = modelDescriptor;
        this.spriteDescriptor = spriteDescriptor;
        this.physicsDescriptor = physicsDescriptor;
        this.tags = List.copyOf(tags);
        this.components = List.copyOf(components);
    }
}
