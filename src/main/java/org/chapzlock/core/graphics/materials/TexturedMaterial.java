package org.chapzlock.core.graphics.materials;

import static org.lwjgl.opengl.GL20.*;

import org.chapzlock.core.graphics.Material;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.shaders.StaticShader;

/**
 * A generic material
 * Can accept just a static shader or a shader and a texture
 */
public class TexturedMaterial implements Material {
    private final StaticShader shader;
    private final Texture texture; // nullable

    public TexturedMaterial(Texture texture) {
        this.shader = new StaticShader();
        this.texture = texture;
    }

    public TexturedMaterial() {
        this.shader = new StaticShader();
        this.texture = null;
    }

    @Override
    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
            shader.setUniform("uTexture", 0); // tells shader to use texture unit 0
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
    public StaticShader getShader() {
        return shader;
    }
}
