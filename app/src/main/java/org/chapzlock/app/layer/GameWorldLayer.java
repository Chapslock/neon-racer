package org.chapzlock.app.layer;

import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerCameraFollowSystem;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerMovementSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.EngineContext;
import org.chapzlock.core.application.Layer;
import org.chapzlock.core.ecs.GameSystem;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Reflection;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Sky;
import org.chapzlock.core.component.Terrain;
import org.chapzlock.core.component.Texture;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.ecs.Entity;
import org.chapzlock.core.asset.FileUtils;
import org.chapzlock.core.asset.RawMeshData;
import org.chapzlock.core.asset.RawMeshDataFactory;
import org.chapzlock.core.render.CameraSystem;
import org.chapzlock.core.render.EntityMaterialRenderer;
import org.chapzlock.core.render.TerrainMaterialRenderer;
import org.chapzlock.core.render.UiMaterialRenderer;
import org.chapzlock.core.render.EntityShaderProps;
import org.chapzlock.core.render.TerrainShaderProps;
import org.chapzlock.core.render.UiShaderProps;
import org.chapzlock.core.physics.CollisionShapeFactory;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.chapzlock.core.physics.PhysicsSystemSpecs;
import org.chapzlock.core.ecs.ComponentRegistry;
import org.chapzlock.core.render.MaterialSystem;
import org.chapzlock.core.render.MeshSystem;
import org.chapzlock.core.render.ShaderSystem;
import org.chapzlock.core.render.TextureSystem;
import org.chapzlock.core.physics.PhysicsSystem;
import org.chapzlock.core.render.RenderSystem;
import org.chapzlock.core.window.WindowSpecification;
import org.joml.Vector3f;

public class GameWorldLayer implements Layer {

    @Override
    public List<GameSystem> systems() {
        return List.of(
            new PlayerInputSystem(),
            new PlayerMovementSystem(),
            new PhysicsSystem(PhysicsSystemSpecs.builder()
                .isDebugEnabled(true)
                .build()),
            new PlayerCameraFollowSystem(),
            new RenderSystem()
        );
    }

    @Override
    public void onInit(EngineContext ctx) {
        createEntities(ctx);
    }

    private void createEntities(EngineContext ctx) {
        ComponentRegistry registry = ctx.getRegistry();
        MeshSystem meshSystem = ctx.getMeshSystem();
        TextureSystem textureSystem = ctx.getTextureSystem();
        MaterialSystem materialSystem = ctx.getMaterialSystem();
        ShaderSystem shaderSystem = ctx.getShaderSystem();
        WindowSpecification windowSpec = ctx.getWindow().getSpecification();

        createPlayer(registry, meshSystem, textureSystem, materialSystem, shaderSystem, windowSpec);

        int light = Entity.create(registry);
        registry.addComponent(light, new PointLight(new Vector3f(-5, 300, -10), Color.WHITE));

        int camera = Entity.create(registry);
        registry.addComponent(camera, new Camera(new Vector3f(0, 5, 10)));

        int sky = Entity.create(registry);
        registry.addComponent(sky, new Sky());

        createUi(registry, meshSystem, textureSystem, materialSystem, shaderSystem, windowSpec);
        createTerrain(registry, meshSystem, textureSystem, materialSystem, shaderSystem, windowSpec);
    }

    private void createUi(ComponentRegistry registry, MeshSystem meshSystem, TextureSystem textureSystem,
                          MaterialSystem materialSystem, ShaderSystem shaderSystem, WindowSpecification windowSpec) {
        int ui = Entity.create(registry);

        CameraSystem cameraSystem = new CameraSystem(windowSpec);
        Texture texture = textureSystem.load("textures/speedometer.png");
        Material material = Material.builder()
            .shader(new Shader(
                UiShaderProps.VERTEX_FILE,
                UiShaderProps.FRAGMENT_FILE
            ))
            .textures(List.of(texture))
            .build();
        materialSystem.registerNewMaterial(material, new UiMaterialRenderer(shaderSystem, textureSystem, cameraSystem, windowSpec));

        Mesh mesh = meshSystem.load(RawMeshDataFactory.createUi());
        registry.addComponents(ui,
            material,
            mesh,
            new Transform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), 200f)
        );
    }

    private void createPlayer(ComponentRegistry registry, MeshSystem meshSystem, TextureSystem textureSystem,
                              MaterialSystem materialSystem, ShaderSystem shaderSystem, WindowSpecification windowSpec) {
        int player = Entity.create(registry);

        CameraSystem cameraSystem = new CameraSystem(windowSpec);
        Material material = new Material(
            new Shader(
                EntityShaderProps.VERTEX_FILE,
                EntityShaderProps.FRAGMENT_FILE
            ),
            textureSystem.load("textures/funcar.png"),
            Reflection.builder()
                .reflectivity(.5f)
                .shineDamper(1f)
                .build()
        );
        Mesh mesh = meshSystem.load("obj/funcar.obj");
        PhysicsBody physicsBody = new PhysicsBody(
            CollisionShapeFactory.createConvexHullShape(meshSystem.getRawMeshById(mesh.getId())),
            PhysicsSpecs.builder()
                .mass(1)
                .build());
        registry.addComponent(player, new Transform(new Vector3f(0, 10, -5), new Vector3f(0, 0, 180)));
        registry.addComponent(player, mesh);
        registry.addComponent(player, materialSystem.registerNewMaterial(material, new EntityMaterialRenderer(shaderSystem, textureSystem, cameraSystem)));
        registry.addComponent(player, new PlayerTag());
        registry.addComponent(player, new PlayerInputComponent());
        registry.addComponent(player, physicsBody);
    }

    private void createTerrain(ComponentRegistry registry, MeshSystem meshSystem, TextureSystem textureSystem,
                               MaterialSystem materialSystem, ShaderSystem shaderSystem, WindowSpecification windowSpec) {
        int terrain = Entity.create(registry);

        CameraSystem cameraSystem = new CameraSystem(windowSpec);
        Terrain terrainProps = Terrain.builder()
            .maxHeight(40)
            .build();
        Material terrainMaterial = Material.builder()
            .textures(List.of(
                textureSystem.load("textures/terrain/grass.png"),
                textureSystem.load("textures/terrain/dirt.png"),
                textureSystem.load("textures/terrain/concrete.png")
            ))
            .blendMap(textureSystem.load("textures/terrain/blendMap.png"))
            .shader(new Shader(
                TerrainShaderProps.VERTEX_FILE,
                TerrainShaderProps.FRAGMENT_FILE
            ))
            .build();
        materialSystem.registerNewMaterial(terrainMaterial, new TerrainMaterialRenderer(shaderSystem, textureSystem, cameraSystem));
        Mesh mesh = meshSystem.load(
            RawMeshDataFactory.generateTerrainFromHeightMap(FileUtils.loadBufferedImage("textures/terrain/heightmap.png"), terrainProps.getSize(), terrainProps.getMaxHeight()));
        registry.addComponent(terrain, new Transform(new Vector3f(-400, 0, -400), new Vector3f(10, 0, 0)));
        registry.addComponent(terrain, terrainMaterial);
        registry.addComponent(terrain, mesh);
        registry.addComponent(terrain, terrainProps);
        RawMeshData rawMeshData = meshSystem.getRawMeshById(mesh.getId());
        registry.addComponent(terrain, new PhysicsBody(
            CollisionShapeFactory.createBvhTriangleMeshShapeFromRaw(rawMeshData, true),
            PhysicsSpecs.builder()
                .restitution(0)
                .build()
        ));
    }
}
