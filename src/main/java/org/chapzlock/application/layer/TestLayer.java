package org.chapzlock.application.layer;

import java.util.List;
import java.util.Random;

import org.chapzlock.application.component.PlayerInputComponent;
import org.chapzlock.application.systems.PlayerInputSystem;
import org.chapzlock.application.systems.PlayerRotateSystem;
import org.chapzlock.application.tags.PlayerTag;
import org.chapzlock.core.application.Layer;
import org.chapzlock.core.component.Transform;
import org.chapzlock.core.entity.Entity;
import org.chapzlock.core.files.FileUtils;
import org.chapzlock.core.geometry.MeshData;
import org.chapzlock.core.graphics.Camera;
import org.chapzlock.core.graphics.Color;
import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.PointLight;
import org.chapzlock.core.graphics.StaticShader;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.registry.ComponentRegistry;
import org.chapzlock.core.system.CameraFreeRoamSystem;
import org.chapzlock.core.system.RenderSystem;
import org.chapzlock.core.system.System;
import org.joml.Vector3f;

public class TestLayer implements Layer {

    private final ComponentRegistry registry = new ComponentRegistry();

    private final List<System> systems = List.of(
        new RenderSystem(registry),
        new PlayerInputSystem(registry),
        new PlayerRotateSystem(registry),
        new CameraFreeRoamSystem(registry)
    );

    public TestLayer() {
        int player = Entity.create();
        MeshData meshData = FileUtils.loadWavefrontFileToMesh("wavefront/funcar.obj");
        Mesh playerMesh = new Mesh(meshData);
        Material playerMat = new Material(new StaticShader(), Texture.loadTexture("textures/funcar.png"));

        registry.addComponent(player, new Transform(new Vector3f(0, 0, -5), new Vector3f(90, 0, 180)));
        registry.addComponent(player, playerMesh);
        registry.addComponent(player, playerMat);
        registry.addComponent(player, new PlayerTag());
        registry.addComponent(player, new PlayerInputComponent());

        generateCars(playerMesh, playerMat);


        int light = Entity.create();
        registry.addComponent(light, new PointLight(new Vector3f(-5, 0, -10), Color.WHITE));

        int camera = Entity.create();
        registry.addComponent(camera, new Camera());
    }

    private void generateCars(Mesh playerMesh, Material playerMat) {
        Random rand = new Random();
        for (int i = 0; i < 100_000; i++) {
            int id = Entity.create();
            float x = rand.nextFloat(0, 200);
            float y = rand.nextFloat(0, 200);
            float z = rand.nextFloat(0, 200);
            registry.addComponent(id, new Transform(new Vector3f(x, y, -z), new Vector3f(x, y, z)));
            registry.addComponent(id, playerMesh);
            registry.addComponent(id, playerMat);
            registry.addComponent(id, new PlayerTag());
        }
    }

    @Override
    public void onInit() {
        systems.forEach(System::onInit);
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
