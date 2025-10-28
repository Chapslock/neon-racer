package org.chapzlock.app.layer;

import java.util.List;

import org.chapzlock.app.component.PlayerInputComponent;
import org.chapzlock.app.systems.PlayerRotateSystem;
import org.chapzlock.app.tags.PlayerTag;
import org.chapzlock.core.application.Layer;
import org.chapzlock.core.application.System;
import org.chapzlock.core.component.Camera;
import org.chapzlock.core.component.Color;
import org.chapzlock.core.component.Material;
import org.chapzlock.core.component.Mesh;
import org.chapzlock.core.component.PointLight;
import org.chapzlock.core.component.Shader;
import org.chapzlock.core.component.Terrain;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.Entity;
import org.chapzlock.core.geometry.RawMeshDataFactory;
import org.chapzlock.core.graphics.shader.EntityShaderProps;
import org.chapzlock.core.graphics.shader.TerrainShaderProps;
import org.chapzlock.core.registry.ComponentRegistry;
import org.chapzlock.core.system.CameraFreeRoamSystem;
import org.chapzlock.core.system.EntityRenderSystem;
import org.chapzlock.core.system.MeshSystem;
import org.chapzlock.core.system.TerrainRenderSystem;
import org.chapzlock.core.system.TextureSystem;
import org.joml.Vector3f;

public class TestLayer implements Layer {

    private final ComponentRegistry registry = ComponentRegistry.instance();
    private final MeshSystem meshSystem = MeshSystem.instance();
    private final TextureSystem textureSystem = new TextureSystem();

    private final List<System> systems = List.of(
        new EntityRenderSystem(),
        new TerrainRenderSystem(),
        new PlayerRotateSystem(),
        new CameraFreeRoamSystem()
    );

    @Override
    public void onInit() {
        createEntities();
        systems.forEach(System::onInit);
    }

    private void createEntities() {
        int player = Entity.create();
        Mesh playerMesh = meshSystem.bind("wavefront/funcar.obj");
        Material playerMat = new Material(
            new Shader(
                EntityShaderProps.VERTEX_FILE,
                EntityShaderProps.FRAGMENT_FILE),
            textureSystem.loadTexture("textures/funcar.png")
        );
        registry.addComponent(player, new Transform(new Vector3f(0, 0, -5), new Vector3f(90, 0, 180)));
        registry.addComponent(player, playerMesh);
        registry.addComponent(player, playerMat);
        registry.addComponent(player, new PlayerTag());
        registry.addComponent(player, new PlayerInputComponent());

        int light = Entity.create();
        registry.addComponent(light, new PointLight(new Vector3f(-5, 10, -10), Color.WHITE));

        int camera = Entity.create();
        registry.addComponent(camera, new Camera(new Vector3f(0, 3, 0)));


        int terrain = Entity.create();
        Terrain terrainProps = new Terrain(20);
        Material mat = new Material(
            new Shader(
                TerrainShaderProps.VERTEX_FILE,
                TerrainShaderProps.FRAGMENT_FILE
            ),
            textureSystem.loadTexture("textures/terrain.png")
        );
        registry.addComponent(terrain, new Transform(new Vector3f(-10, 0, -10)));
        registry.addComponent(terrain, mat);
        registry.addComponent(terrain, meshSystem.bind(
            RawMeshDataFactory.generateFlatTerrain(terrainProps.getVertexCount(), terrainProps.getSize())));
        registry.addComponent(terrain, terrainProps);
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
