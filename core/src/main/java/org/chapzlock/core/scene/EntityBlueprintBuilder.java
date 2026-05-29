package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Shader;
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

    public EntityBlueprintBuilder tag(Class<? extends Tag> tagClass) {
        factories.add(ctx -> {
            try {
                return new Component[]{ tagClass.getDeclaredConstructor().newInstance() };
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate tag: " + tagClass.getSimpleName() +
                    ". Ensure it has a no-arg constructor.", e);
            }
        });
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
            var defaultRenderer = ctx.getMaterialRendererRegistry().getDefaultRenderer(MaterialType.ENTITY);
            if (defaultRenderer != null) {
                ctx.getMaterialSystem().registerNewMaterial(material, defaultRenderer);
            }
            return new Component[]{ material };
        });
        return this;
    }

    public EntityBlueprint build() {
        return new EntityBlueprint(name, factories);
    }
}
