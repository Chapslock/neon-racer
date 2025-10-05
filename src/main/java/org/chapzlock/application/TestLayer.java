package org.chapzlock.application;

import org.chapzlock.core.Layer;
import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Mesh;
import org.chapzlock.core.graphics.ShaderProgram;
import org.chapzlock.core.graphics.shaders.StaticShader;
import org.chapzlock.core.graphics.Texture;

public class TestLayer implements Layer {

    // Simple quad for 2D sprite
    private float[] positions = {
        -0.5f, -0.5f, 0.0f, // bottom-left
        0.5f, -0.5f, 0.0f, // bottom-right
        0.5f,  0.5f, 0.0f, // top-right
        -0.5f,  0.5f, 0.0f  // top-left
    };

    private float[] texCoords = {
        0.0f, 0.0f,  // bottom-left
        1.0f, 0.0f,  // bottom-right
        1.0f, 1.0f,  // top-right
        0.0f, 1.0f   // top-left
    };

    private int[] indices = {
        0, 1, 2,  // first triangle (bottom-left, bottom-right, top-right)
        2, 3, 0   // second triangle (top-right, top-left, bottom-left)
    };

    private Mesh mesh;
    private Material material;

    public TestLayer() {
        ShaderProgram shaderProgram = new StaticShader();
        Texture texture = Texture.loadTexture("textures/example.png");

        this.mesh = new Mesh(positions, texCoords, indices);
        this.material = new Material(shaderProgram, texture);
    }

    @Override
    public void onRender(float deltaTime) {
        material.bind();
        mesh.render();
        material.unbind();
    }

    @Override
    public void onDestroy() {
    }
}
