package org.chapzlock.core.scene;

import java.util.List;

import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;

import lombok.Getter;

/**
 * Immutable blueprint for an entity, produced by {@link EntityBlueprintBuilder}.
 *
 * <p>Contains the entity's rendering descriptor, physics descriptor, tags,
 * and game-logic components. Transform is now a regular component in the
 * components list. The {@link DescriptorResolver} consumes descriptors at
 * scene load time to produce runtime components.</p>
 *
 * <p>Optionally carries a {@code gltfModelPath} indicating that a glTF model
 * should be instantiated as children of this entity during scene loading. This
 * makes the entity the "owner" of the model — moving it moves the entire model,
 * and animation clips from the glTF are attached to this entity.</p>
 */
@Getter
public final class EntityBlueprint {
    private final String name;
    private final SpriteDescriptor spriteDescriptor;
    private final PhysicsDescriptor physicsDescriptor;
    private final List<Tag> tags;
    private final List<Component> components;

    /**
     * Path to a glTF/GLB file to instantiate as children of this entity, or {@code null}
     * if this entity does not own a glTF model.
     *
     * <p>When set, the scene instantiation system will load the glTF file and parent its
     * node tree under this entity. Animation clips and skeleton data from the glTF are
     * also attached to this entity.
     */
    private final String gltfModelPath;

    /**
     * Full constructor including the glTF model path.
     *
     * @param name              human-readable entity name for debugging and lookup
     * @param spriteDescriptor  2D sprite configuration, or null if not a sprite entity
     * @param physicsDescriptor physics body configuration, or null if not a physics entity
     * @param tags              query-filtering tags to add to this entity
     * @param components        game-logic components to add directly (Transform should be in this list)
     * @param gltfModelPath     resource path to a glTF/GLB file to instantiate as children,
     *                          or null if this entity does not own a model
     */
    public EntityBlueprint(String name,
                           SpriteDescriptor spriteDescriptor,
                           PhysicsDescriptor physicsDescriptor,
                           List<Tag> tags,
                           List<Component> components,
                           String gltfModelPath) {
        this.name = name;
        this.spriteDescriptor = spriteDescriptor;
        this.physicsDescriptor = physicsDescriptor;
        this.tags = List.copyOf(tags);
        this.components = List.copyOf(components);
        this.gltfModelPath = gltfModelPath;
    }

    /**
     * Backwards-compatible constructor without a glTF model path.
     *
     * <p>This is used by {@link SceneBuilder} for programmatic entities like lights, fog,
     * cameras, and sky that do not have an associated 3D model. The {@code gltfModelPath}
     * is set to {@code null}.
     *
     * @param name              human-readable entity name for debugging and lookup
     * @param spriteDescriptor  2D sprite configuration, or null if not a sprite entity
     * @param physicsDescriptor physics body configuration, or null if not a physics entity
     * @param tags              query-filtering tags to add to this entity
     * @param components        game-logic components to add directly (Transform should be in this list)
     */
    public EntityBlueprint(String name,
                           SpriteDescriptor spriteDescriptor,
                           PhysicsDescriptor physicsDescriptor,
                           List<Tag> tags,
                           List<Component> components) {
        this(name, spriteDescriptor, physicsDescriptor, tags, components, null);
    }
}
