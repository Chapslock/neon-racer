package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;
import org.joml.Vector3f;

/**
 * Fluent builder for creating entity blueprints.
 *
 * <p>Provides convenience methods for common components: {@code transform()}, {@code mesh()},
 * {@code texture()}, {@code model()} (mesh + texture), {@code tag()}.
 *
 * <p>For custom components, use {@code component()} or {@code component(ComponentFactory)}.
 */
public class EntityBlueprintBuilder {

    private final String name;
    private final List<ComponentFactory> factories = new ArrayList<>();

    public EntityBlueprintBuilder(String name) {
        this.name = name;
    }

    /** Adds a Transform component at the given position. */
    public EntityBlueprintBuilder transform(float x, float y, float z) {
        factories.add(ctx -> new Component[]{ new Transform(new Vector3f(x, y, z)) });
        return this;
    }

    /** Adds a Transform component at the given position with rotation (euler angles in radians). */
    public EntityBlueprintBuilder transform(float x, float y, float z, float rotX, float rotY, float rotZ) {
        factories.add(ctx -> new Component[]{
            new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ))
        });
        return this;
    }

    /** Adds a Transform component at the given position with rotation (euler angles) and uniform scale. */
    public EntityBlueprintBuilder transform(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
        factories.add(ctx -> new Component[]{
            new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ), scale)
        });
        return this;
    }

    /**
     * Adds a tag component. Tags are empty components used for query filtering.
     *
     * @param tagInstance the tag instance (e.g., {@code new Enemy()})
     * @return this builder
     */
    public EntityBlueprintBuilder tag(Tag tagInstance) {
        factories.add(ctx -> new Component[]{ tagInstance });
        return this;
    }

    /** Adds components directly (no factory). Use for simple, context-free components. */
    public EntityBlueprintBuilder component(Component... components) {
        factories.add(ctx -> components);
        return this;
    }

    /** Adds a component factory. Invoked at scene load with access to EngineContext. */
    public EntityBlueprintBuilder component(ComponentFactory factory) {
        factories.add(factory);
        return this;
    }

    /**
     * Adds a Mesh component by loading from the given path. Uses {@link org.chapzlock.core.render.MeshSystem}.
     *
     * @param resourcePath path to mesh file
     * @return this builder
     */
    public EntityBlueprintBuilder mesh(String resourcePath) {
        factories.add(ctx -> {
            if (ctx.getMeshSystem() == null) {
                return new Component[]{};
            }
            var mesh = ctx.getMeshSystem().load(resourcePath);
            return new Component[]{ mesh };
        });
        return this;
    }

    /**
     * Adds a Material component with the given texture (ENTITY material type).
     *
     * @param filePath path to texture file
     * @return this builder
     */
    public EntityBlueprintBuilder texture(String filePath) {
        factories.add(ctx -> {
            if (ctx.getTextureSystem() == null) {
                return new Component[]{};
            }
            var texture = ctx.getTextureSystem().load(filePath);
            var shader = new Shader(MaterialType.ENTITY.getVertexPath(), MaterialType.ENTITY.getFragmentPath());
            var material = new Material(shader, texture);
            if (ctx.getMaterialSystem() != null) {
                ctx.getMaterialSystem().register(material, MaterialType.ENTITY);
            }
            return new Component[]{ material };
        });
        return this;
    }

    /**
     * Adds Mesh + Material components (ENTITY material type). Convenience for {@code mesh().texture()}.
     *
     * @param meshPath path to mesh file
     * @param texturePath path to texture file
     * @return this builder
     */
    public EntityBlueprintBuilder model(String meshPath, String texturePath) {
        return model(meshPath, texturePath, MaterialType.ENTITY);
    }

    /**
     * Adds Mesh + Material components with the specified material type.
     *
     * @param meshPath path to mesh file
     * @param texturePath path to texture file
     * @param type material type
     * @return this builder
     */
    public EntityBlueprintBuilder model(String meshPath, String texturePath, MaterialType type) {
        factories.add(ctx -> {
            if (ctx.getMeshSystem() == null || ctx.getTextureSystem() == null) {
                return new Component[]{};
            }
            Mesh mesh = ctx.getMeshSystem().load(meshPath);
            Texture texture = ctx.getTextureSystem().load(texturePath);
            Shader shader = new Shader(type.getVertexPath(), type.getFragmentPath());
            Material material = new Material(shader, texture);
            if (ctx.getMaterialSystem() != null) {
                ctx.getMaterialSystem().register(material, type);
            }
            return new Component[]{ mesh, material };
        });
        return this;
    }

    /** Builds the entity blueprint. */
    public EntityBlueprint build() {
        return new EntityBlueprint(name, factories);
    }
}
