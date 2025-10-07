package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL20.*;

import org.chapzlock.core.graphics.shaders.StaticShader;

/**
 * A generic material
 * Can accept just a static shader or a shader and a texture
 */
public class Material {
    private final StaticShader shader;
    private final Texture texture; // nullable

    public Material(StaticShader shader, Texture texture) {
        this.shader = shader;
        this.texture = texture;
    }

    public Material(StaticShader shader) {
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

    public StaticShader getShader() {
        return shader;
    }
}
