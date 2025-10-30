package org.chapzlock.app.layer;

import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Layer;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.PhysicsBody;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Reflection;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Terrain;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.Entity;
import org.chapzlock.core.geometry.RawMeshDataFactory;
import org.chapzlock.core.graphics.material.EntityMaterialRenderer;
import org.chapzlock.core.graphics.material.TerrainMaterialRenderer;
import org.chapzlock.core.graphics.shader.EntityShaderProps;
import org.chapzlock.core.graphics.shader.TerrainShaderProps;
import org.chapzlock.core.physics.PhysicsSpecs;
import org.chapzlock.core.registry.ComponentRegistry;
import org.chapzlock.core.system.CameraFreeRoamSystem;
import org.chapzlock.core.system.MaterialSystem;
import org.chapzlock.core.system.MeshSystem;
import org.chapzlock.core.system.PhysicsSystem;
import org.chapzlock.core.system.RenderSystem;
import org.chapzlock.core.system.TextureSystem;
import org.joml.Vector3f;

import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;

public class TestLayer implements Layer {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();
    private final TextureSystem textureSystem = TextureSystem.instance();
    private final MaterialSystem materialSystem = MaterialSystem.instance();

    private final List<System> systems = List.of(
        new RenderSystem(),
        new PhysicsSystem(),
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
        registry.addComponent(light, new PointLight(new Vector3f(-5, 10, -10), Color.WHITE));

        int camera = Entity.create();
        registry.addComponent(camera, new Camera(new Vector3f(0, 5, 10)));

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

        registry.addComponent(player, new Transform(new Vector3f(0, 10, -5), new Vector3f(90, 0, 180)));
        registry.addComponent(player, meshSystem.load("wavefront/funcar.obj"));
        registry.addComponent(player, materialSystem.registerNewMaterial(material, new EntityMaterialRenderer()));
        registry.addComponent(player, new PlayerTag());
        registry.addComponent(player, new PlayerInputComponent());
        registry.addComponent(player, new PhysicsBody(new SphereShape(1f), PhysicsSpecs.builder()
            .mass(1)
            .restitution(0.2f)
            .build()));
    }

    private void createTerrain() {
        int terrain = Entity.create();
        Terrain terrainProps = new Terrain(20);
        Material terrainMaterial = new Material(
            new Shader(
                TerrainShaderProps.VERTEX_FILE,
                TerrainShaderProps.FRAGMENT_FILE
            ),
            textureSystem.load("textures/terrain.png")
        );
        materialSystem.registerNewMaterial(terrainMaterial, new TerrainMaterialRenderer());
        registry.addComponent(terrain, new Transform(new Vector3f(-10, 0, -10), new Vector3f(0, 0, 20)));
        registry.addComponent(terrain, terrainMaterial);
        registry.addComponent(terrain, meshSystem.load(
            RawMeshDataFactory.generateFlatTerrain(terrainProps.getVertexCount(), terrainProps.getSize()))
        );
        registry.addComponent(terrain, terrainProps);
        registry.addComponent(terrain, new PhysicsBody(
            new StaticPlaneShape(new javax.vecmath.Vector3f(0, 1, 0), 1f),
            PhysicsSpecs.builder()
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
