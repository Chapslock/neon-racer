package org.chapzlock.core.scene;

import java.util.ArrayList;
import java.util.List;

import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.asset.RawMeshDataFactory;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.ModelDescriptor;
import org.chapzlock.core.component.OpaqueLayer;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PhysicsDescriptor;
import org.chapzlock.core.component.Reflection;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.SpriteDescriptor;
import org.chapzlock.core.component.Terrain;
import org.chapzlock.core.component.TerrainDescriptor;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.TransparentLayer;
import org.chapzlock.core.component.UiLayer;
import org.chapzlock.core.ecs.Component;
import org.chapzlock.core.ecs.Tag;
import org.chapzlock.core.physics.CollisionShapeFactory;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.TextureSystem;

/**
 * Resolves descriptor components into GPU-backed runtime components during
 * scene instantiation.
 *
 * <p>Each descriptor type has a corresponding resolution method that loads assets,
 * selects shaders, creates materials, and registers them with the engine.
 * Descriptors are consumed — they are not retained on the entity.</p>
 */
public class DescriptorResolver {

    private final EngineContext ctx;

    public DescriptorResolver(EngineContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Resolves all descriptors on a blueprint into runtime components.
     *
     * @return ordered list of components to attach to the entity
     */
    public List<Component> resolve(EntityBlueprint blueprint) {
        List<Component> result = new ArrayList<>();

        if (blueprint.getTransform() != null) {
            result.add(blueprint.getTransform());
        }

        Mesh mesh = null;

        if (blueprint.getModelDescriptor() != null) {
            mesh = resolveModel(blueprint.getModelDescriptor(), result);
        } else if (blueprint.getTerrainDescriptor() != null) {
            mesh = resolveTerrain(blueprint.getTerrainDescriptor(), result);
        } else if (blueprint.getSpriteDescriptor() != null) {
            mesh = resolveSprite(blueprint.getSpriteDescriptor(), result);
        }

        if (blueprint.getPhysicsDescriptor() != null) {
            resolvePhysics(blueprint.getPhysicsDescriptor(), mesh, result);
        }

        for (Tag tag : blueprint.getTags()) {
            result.add(tag);
        }

        result.addAll(blueprint.getComponents());

        return result;
    }

    private Mesh resolveModel(ModelDescriptor desc, List<Component> result) {
        MeshSystem meshSystem = ctx.getMeshSystem();
        TextureSystem textureSystem = ctx.getTextureSystem();
        MaterialSystem materialSystem = ctx.getMaterialSystem();

        Mesh mesh = meshSystem.load(desc.getMesh());
        result.add(mesh);

        Texture texture = desc.getTexture() != null
            ? textureSystem.load(desc.getTexture())
            : null;

        MaterialType type = MaterialType.ENTITY;
        Shader shader = new Shader(type.getVertexPath(), type.getFragmentPath());

        Material material = Material.builder()
            .shader(shader)
            .textures(texture != null ? List.of(texture) : List.of())
            .reflection(Reflection.builder()
                .reflectivity(desc.getReflectivity())
                .shineDamper(desc.getShineDamper())
                .build())
            .build();

        materialSystem.register(material, type);
        result.add(material);

        if (desc.isTransparent()) {
            result.add(new TransparentLayer());
        } else {
            result.add(new OpaqueLayer());
        }

        return mesh;
    }

    private Mesh resolveTerrain(TerrainDescriptor desc, List<Component> result) {
        MeshSystem meshSystem = ctx.getMeshSystem();
        TextureSystem textureSystem = ctx.getTextureSystem();
        MaterialSystem materialSystem = ctx.getMaterialSystem();

        Terrain terrainComponent = Terrain.builder()
            .maxHeight((int) desc.getMaxHeight())
            .build();
        result.add(terrainComponent);

        RawMeshData rawMeshData = RawMeshDataFactory.generateTerrainFromHeightMap(
            FileUtils.loadBufferedImage(desc.getHeightmap()),
            desc.getSize(), desc.getMaxHeight());
        Mesh mesh = meshSystem.load(rawMeshData);
        result.add(mesh);

        List<Texture> textures = new ArrayList<>();
        for (String texPath : desc.getTextures()) {
            textures.add(textureSystem.load(texPath));
        }

        Texture blendMap = desc.getBlendMap() != null
            ? textureSystem.load(desc.getBlendMap())
            : null;

        MaterialType type = MaterialType.TERRAIN;
        Shader shader = new Shader(type.getVertexPath(), type.getFragmentPath());

        Material material = Material.builder()
            .shader(shader)
            .textures(textures)
            .blendMap(blendMap)
            .build();

        materialSystem.register(material, type);
        result.add(material);
        result.add(new OpaqueLayer());

        return mesh;
    }

    private Mesh resolveSprite(SpriteDescriptor desc, List<Component> result) {
        MeshSystem meshSystem = ctx.getMeshSystem();
        TextureSystem textureSystem = ctx.getTextureSystem();
        MaterialSystem materialSystem = ctx.getMaterialSystem();

        Texture texture = textureSystem.load(desc.getTexture());
        Mesh mesh = meshSystem.load(RawMeshDataFactory.createUi());
        result.add(mesh);

        MaterialType type = MaterialType.UI;
        Shader shader = new Shader(type.getVertexPath(), type.getFragmentPath());

        Material material = Material.builder()
            .shader(shader)
            .textures(List.of(texture))
            .tint(desc.getTint())
            .build();

        materialSystem.register(material, type);
        result.add(material);
        result.add(new UiLayer());

        return mesh;
    }

    private void resolvePhysics(PhysicsDescriptor desc, Mesh mesh, List<Component> result) {
        com.bulletphysics.collision.shapes.CollisionShape shape = desc.getShape();

        if (shape == null && mesh != null) {
            RawMeshData rawData = ctx.getMeshSystem().getRawMeshById(mesh.getId());
            shape = desc.isDynamic()
                ? CollisionShapeFactory.createConvexHullShape(rawData)
                : CollisionShapeFactory.createBvhTriangleMeshShapeFromRaw(rawData, true);
        }

        PhysicsSpecs specs = PhysicsSpecs.builder()
            .mass(desc.getMass())
            .restitution(desc.getRestitution())
            .build();

        result.add(new PhysicsBody(shape, specs));
    }
}
