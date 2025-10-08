package org.chapzlock.application.layer;

import java.util.List;
import java.util.UUID;

import org.chapzlock.application.component.InputComponent;
import org.chapzlock.application.systems.PlayerInputSystem;
import org.chapzlock.application.systems.PlayerMovementSystem;
import org.chapzlock.application.tags.PlayerTag;
import org.chapzlock.core.Layer;
import org.chapzlock.core.component.CameraComponent;
import org.chapzlock.core.component.MaterialComponent;
import org.chapzlock.core.component.MeshComponent;
import org.chapzlock.core.component.TransformComponent;
import org.chapzlock.core.component.orchestration.ComponentRegistry;
import org.chapzlock.core.entity.Entity;
import org.chapzlock.core.geometry.MeshFactory;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.materials.TexturedMaterial;
import org.chapzlock.core.math.Vector3f;
import org.chapzlock.core.system.RenderSystem;
import org.chapzlock.core.system.System;

public class TestLayer implements Layer {

    private ComponentRegistry registry = new ComponentRegistry();

    private List<System> systems = List.of(
        new RenderSystem(registry),
        new PlayerMovementSystem(registry),
        new PlayerInputSystem(registry)
    );

    public TestLayer() {
        UUID boxEntity = Entity.create();
        Mesh boxMesh = new Mesh(MeshFactory.createCube(1));
        TexturedMaterial boxMaterial = new TexturedMaterial(Texture.loadTexture("textures/rubics.png"));

        registry.addComponent(boxEntity, new TransformComponent(new Vector3f(0, 0, -5)));
        registry.addComponent(boxEntity, new CameraComponent());
        registry.addComponent(boxEntity, new MeshComponent(boxMesh));
        registry.addComponent(boxEntity, new MaterialComponent(boxMaterial));
        registry.addComponent(boxEntity, new PlayerTag());
        registry.addComponent(boxEntity, new InputComponent());

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
