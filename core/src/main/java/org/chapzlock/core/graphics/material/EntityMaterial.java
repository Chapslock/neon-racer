package org.chapzlock.core.graphics.material;

import static org.lwjgl.opengl.GL20.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20.glActiveTexture;

import org.chapzlock.core.component.Component;
import org.chapzlock.core.graphics.Reflection;
import org.chapzlock.core.graphics.Texture;
import org.chapzlock.core.graphics.shader.EntityStaticShader;

import lombok.Getter;

/**
 * A generic entityMaterial
 * Can accept just a shared shader or a shader and a texture
 */
public class EntityMaterial implements Component {

    @Getter
    private final EntityStaticShader shader;
    private final Texture texture; // nullable

    @Getter
    private final Reflection reflection;

    public EntityMaterial(EntityStaticShader shader, Texture texture) {
        this(shader, texture, null);
    }

    public EntityMaterial(EntityStaticShader shader, Texture texture, Reflection reflection) {
        this.shader = shader;
        this.texture = texture;
        this.reflection = reflection != null ? reflection : Reflection.builder()
            .reflectivity(1f)
            .shineDamper(10f)
            .build();
    }

    public EntityMaterial(EntityStaticShader shader) {
        this(shader, null, null);
    }

    public void bind() {
        shader.bind();

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);
            texture.bind();
        }

        shader.loadShine(reflection.getShineDamper(), reflection.getReflectivity());
    }

    public void unbind() {
        if (texture != null) {
            texture.unbind();
        }
        shader.unbind();
    }
}
