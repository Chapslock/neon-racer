package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL20.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.glActiveTexture;

import org.chapzlock.core.component.Component;

import lombok.Getter;

/**
 * A generic material
 * Can accept just a static shader or a shader and a texture
 */
public class Material implements Component {
    @Getter
    private final StaticShader shader;
    private final Texture texture; // nullable
    @Getter
    private final Reflection reflection = Reflection.builder()
        .reflectivity(1)
        .shineDamper(10)
        .build();

    public Material(Texture texture) {
        this.shader = new StaticShader();
        this.texture = texture;
    }

    public Material() {
        this.shader = new StaticShader();
        this.texture = null;
    }

    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
            shader.setUniform("textureSampler", 0); // tells shader to use texture unit 0
        }
    }


    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }
}
