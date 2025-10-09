package org.chapzlock.core.graphics.materials;

import static org.lwjgl.opengl.GL20.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.glActiveTexture;

import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.shaders.TextureShader;

/**
 * A generic material
 * Can accept just a static shader or a shader and a texture
 */
public class TexturedMaterial implements Material {
    private final TextureShader shader;
    private final Texture texture; // nullable

    public TexturedMaterial(Texture texture) {
        this.shader = new TextureShader();
        this.texture = texture;
    }

    public TexturedMaterial() {
        this.shader = new TextureShader();
        this.texture = null;
    }

    @Override
    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
            shader.setUniform("textureSampler", 0); // tells shader to use texture unit 0
        }
    }

    @Override
    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }

    @Override
    public TextureShader getShader() {
        return shader;
    }
}
