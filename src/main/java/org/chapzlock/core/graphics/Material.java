package org.chapzlock.core.graphics;

import static org.lwjgl.opengl.GL20.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.glActiveTexture;

import org.chapzlock.core.component.Component;

import lombok.Getter;

/**
 * A generic material
 * Can accept just a shared shader or a shader and a texture
 */
public class Material implements Component {

    @Getter
    private final StaticShader shader;
    private final Texture texture; // nullable

    @Getter
    private final Reflection reflection;

    public Material(StaticShader shader, Texture texture) {
        this(shader, texture, null);
    }

    public Material(StaticShader shader, Texture texture, Reflection reflection) {
        this.shader = shader;
        this.texture = texture;
        this.reflection = reflection != null ? reflection : Reflection.builder()
            .reflectivity(1f)
            .shineDamper(10f)
            .build();
    }

    public Material(StaticShader shader) {
        this(shader, null, null);
    }

    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
        }

        // Load reflection uniforms if needed
        shader.loadShine(reflection.getShineDamper(), reflection.getReflectivity());
    }

    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }
}
