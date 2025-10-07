package org.chapzlock.application;

import org.chapzlock.core.Camera;
import org.chapzlock.core.Entity;
import org.chapzlock.core.Layer;
import org.chapzlock.core.geometry.MeshFactory;
import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.shaders.StaticShader;
import org.chapzlock.core.math.Vector3f;

public class TestLayer implements Layer {

    private Mesh mesh;
    private Material material;

    private Entity entity;

    private Camera camera = new Camera();

    public TestLayer() {
        StaticShader staticShader = new StaticShader();
        Texture texture = Texture.loadTexture("textures/example.png");

        var cubeMesh = MeshFactory.createCube(1);

        this.mesh = new Mesh(cubeMesh.positions(), cubeMesh.texCoords(), cubeMesh.indices());
        this.material = new Material(staticShader, texture);

        this.entity = new Entity(
            mesh,
            material,
            new Vector3f(0, 0,-1),
            new Vector3f(0,0,0),
            1
        );

    }

    @Override
    public void onUpdate(float deltaTime) {
        entity.move(new Vector3f(0, 0,-0.001f));
        entity.rotate(new Vector3f(0,0.5f,0.5f));
        camera.onUpdate();
    }

    @Override
    public void onRender(float deltaTime) {
        entity.onRender(camera);
    }

    @Override
    public void onDestroy() {
    }
}
