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

public class EntityBlueprintBuilder {

    private final String name;
    private final List<ComponentFactory> factories = new ArrayList<>();

    public EntityBlueprintBuilder(String name) {
        this.name = name;
    }

    public EntityBlueprintBuilder transform(float x, float y, float z) {
        factories.add(ctx -> new Component[]{ new Transform(new Vector3f(x, y, z)) });
        return this;
    }

    public EntityBlueprintBuilder transform(float x, float y, float z, float rotX, float rotY, float rotZ) {
        factories.add(ctx -> new Component[]{
            new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ))
        });
        return this;
    }

    public EntityBlueprintBuilder transform(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
        factories.add(ctx -> new Component[]{
            new Transform(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ), scale)
        });
        return this;
    }

    public EntityBlueprintBuilder tag(Tag tagInstance) {
        factories.add(ctx -> new Component[]{ tagInstance });
        return this;
    }

    public EntityBlueprintBuilder component(Component... components) {
        factories.add(ctx -> components);
        return this;
    }

    public EntityBlueprintBuilder component(ComponentFactory factory) {
        factories.add(factory);
        return this;
    }

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

    public EntityBlueprintBuilder model(String meshPath, String texturePath) {
        return model(meshPath, texturePath, MaterialType.ENTITY);
    }

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

    public EntityBlueprint build() {
        return new EntityBlueprint(name, factories);
    }
}
