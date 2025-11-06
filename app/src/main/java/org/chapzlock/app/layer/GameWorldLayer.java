package org.chapzlock.app.layer;

import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerInputSystem;
import org.chapzlock.app.systems.PlayerMovementSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Layer;
import org.chapzlock.core.application.System;
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
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.Entity;
import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.geometry.RawMeshData;
import org.chapzlock.core.geometry.RawMeshDataFactory;
import org.chapzlock.core.graphics.material.EntityMaterialRenderer;
import org.chapzlock.core.graphics.material.TerrainMaterialRenderer;
import org.chapzlock.core.graphics.shader.EntityShaderProps;
import org.chapzlock.core.graphics.shader.TerrainShaderProps;
import org.chapzlock.core.physics.CollisionShapeFactory;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.chapzlock.core.physics.PhysicsSystemSpecs;
import org.chapzlock.core.registry.ComponentRegistry;
import org.chapzlock.core.system.CameraFreeRoamSystem;
import org.chapzlock.core.system.MaterialSystem;
import org.chapzlock.core.system.MeshSystem;
import org.chapzlock.core.system.PhysicsSystem;
import org.chapzlock.core.system.RenderSystem;
import org.chapzlock.core.system.TextureSystem;
import org.joml.Vector3f;

public class GameWorldLayer implements Layer {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();
    private final TextureSystem textureSystem = TextureSystem.instance();
    private final MaterialSystem materialSystem = MaterialSystem.instance();

    private final List<System> systems = List.of(
        new RenderSystem(),
        new PlayerMovementSystem(),
        new PlayerInputSystem(),
        new PhysicsSystem(PhysicsSystemSpecs.builder()
            .isDebugEnabled(true)
            .build()),
        new CameraFreeRoamSystem()
    );

    @Override
    public void onInit() {
        createEntities();
        systems.forEach(System::onInit);
    }

    private void createEntities() {
        createPlayer();

        int light = Entity.create();
        registry.addComponent(light, new PointLight(new Vector3f(-5, 300, -10), Color.WHITE));

        int camera = Entity.create();
        registry.addComponent(camera, new Camera(new Vector3f(0, 5, 10)));

        int sky = Entity.create();
        registry.addComponent(sky, new Sky());

        createTerrain();
    }

    private void createPlayer() {
        int player = Entity.create();

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
        registry.addComponent(player, materialSystem.registerNewMaterial(material, new EntityMaterialRenderer()));
        registry.addComponent(player, new PlayerTag());
        registry.addComponent(player, new PlayerInputComponent());
        registry.addComponent(player, physicsBody);
    }

    private void createTerrain() {
        int terrain = Entity.create();
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
        materialSystem.registerNewMaterial(terrainMaterial, new TerrainMaterialRenderer());
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

    @Override
    public void onUpdate(float deltaTime) {
        systems.forEach(s -> s.onUpdate(deltaTime));
    }

    @Override
    public void onRender(float deltaTime) {
        systems.forEach(s -> s.onRender(deltaTime));
    }

    @Override
    public void onDestroy() {
        systems.forEach(System::onDestroy);
    }
}
