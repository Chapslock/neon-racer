package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL20.*;

/**
 * A generic material
 * Can accept just a static shader or a shader and a texture
 */
public class Material {
    private final ShaderProgram shader;
    private final Texture texture; // nullable

    public Material(ShaderProgram shader, Texture texture) {
        this.shader = shader;
        this.texture = texture;
    }

    public Material(ShaderProgram shader) {
        this.shader = shader;
        this.texture = null;
    }

    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
            shader.setUniform("uTexture", 0); // tells shader to use texture unit 0
        }
    }

    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }

    public ShaderProgram getShader() {
        return shader;
    }
}
